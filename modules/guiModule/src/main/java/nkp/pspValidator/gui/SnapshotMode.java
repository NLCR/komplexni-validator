package nkp.pspValidator.gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import nkp.pspValidator.gui.skipping.SkippingConfigurationDialog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Vývojářský režim: po inicializaci validačních dat postupně otevře vybrané dialogy, uloží snímek každého z nich
 * do PNG a aplikaci ukončí. Slouží ke kontrole vzhledu napříč platformami (macOS, Windows) bez ručního klikání
 * a bez oprávnění ke snímání obrazovky; stejný příkaz lze pustit lokálně i v CI.
 * <p>
 * Zapíná se systémovou vlastností {@code -Dkv.snapshotDir=<adresář>}. Výběr dialogů volitelně přes
 * {@code -Dkv.snapshot=main,pspDir,pspZip,skipping,externalUtils,about,dictionaries} (výchozí je vše),
 * prodleva před snímkem přes {@code -Dkv.snapshotDelayMs} (výchozí 800 ms, aby proběhl layout a asynchronní
 * inicializace dialogu).
 * <p>
 * Přes gradle: {@code ./gradlew :modules:guiModule:runSnapshots -PsnapshotDir=/tmp/snap}.
 */
public class SnapshotMode {

    public static final String PROP_DIR = "kv.snapshotDir";
    public static final String PROP_DIALOGS = "kv.snapshot";
    public static final String PROP_DELAY_MS = "kv.snapshotDelayMs";
    public static final String ALL_DIALOGS = "main,pspDir,pspZip,skipping,externalUtils,about,dictionaries";

    public static boolean isEnabled() {
        return System.getProperty(PROP_DIR) != null;
    }

    /**
     * Spustí sekvenci snímků. Volat z FX vlákna po nastavení ValidationDataManager.
     */
    public static void start(Main main) {
        File dir = new File(System.getProperty(PROP_DIR));
        dir.mkdirs();
        long delayMs = Long.parseLong(System.getProperty(PROP_DELAY_MS, "800"));
        List<String> names = new ArrayList<>(Arrays.asList(System.getProperty(PROP_DIALOGS, ALL_DIALOGS).split("\\s*,\\s*")));

        Map<String, Supplier<AbstractDialog>> dialogs = new LinkedHashMap<>();
        Stage dialogStage = main.getDialogStage();
        dialogs.put("pspDir", () -> new PspDirValidationConfigurationDialog(dialogStage, main));
        dialogs.put("pspZip", () -> new PspZipValidationConfigurationDialog(dialogStage, main));
        dialogs.put("skipping", () -> new SkippingConfigurationDialog(dialogStage, main));
        dialogs.put("externalUtils", () -> new ExternalUtilsCheckDialog(dialogStage, main, false, "Pokračovat"));
        dialogs.put("about", () -> new AboutAppDialog(dialogStage, main));
        dialogs.put("dictionaries", () -> new DictionariesConfigurationDialog(dialogStage, main));

        System.out.println("SNAPSHOT MODE: dir=" + dir.getAbsolutePath() + ", dialogs=" + names + ", delay=" + delayMs + " ms");
        new Runner(main, dir, delayMs, names, dialogs).next();
    }

    private static class Runner {
        private final Main main;
        private final File dir;
        private final long delayMs;
        private final List<String> queue;
        private final Map<String, Supplier<AbstractDialog>> dialogs;
        private boolean failed = false;

        Runner(Main main, File dir, long delayMs, List<String> queue, Map<String, Supplier<AbstractDialog>> dialogs) {
            this.main = main;
            this.dir = dir;
            this.delayMs = delayMs;
            this.queue = queue;
            this.dialogs = dialogs;
        }

        void next() {
            if (queue.isEmpty()) {
                System.out.println("SNAPSHOT MODE: " + (failed ? "finished with errors" : "finished"));
                Platform.exit();
                return;
            }
            String name = queue.remove(0);
            try {
                if ("main".equals(name)) {
                    Stage stage = main.getPrimaryStage();
                    after(() -> {
                        save(stage.getScene(), name);
                        next();
                    });
                } else if (dialogs.containsKey(name)) {
                    AbstractDialog dialog = dialogs.get(name).get();
                    Stage stage = dialog.showNonBlocking();
                    after(() -> {
                        save(stage.getScene(), name);
                        stage.close();
                        next();
                    });
                } else {
                    System.err.println("SNAPSHOT MODE: unknown dialog '" + name + "', known: main," + String.join(",", dialogs.keySet()));
                    failed = true;
                    next();
                }
            } catch (Exception e) {
                System.err.println("SNAPSHOT MODE: failed to open '" + name + "': " + e);
                e.printStackTrace();
                failed = true;
                next();
            }
        }

        private void after(Runnable action) {
            PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
            pause.setOnFinished(e -> action.run());
            pause.play();
        }

        private void save(Scene scene, String name) {
            File file = new File(dir, name + ".png");
            try {
                WritableImage image = scene.snapshot(null);
                writePng(image, file);
                System.out.println("SNAPSHOT MODE: " + name + " -> " + file.getAbsolutePath()
                        + " (" + (int) image.getWidth() + "x" + (int) image.getHeight() + ")");
            } catch (Exception e) {
                System.err.println("SNAPSHOT MODE: failed to save '" + name + "': " + e);
                failed = true;
            }
        }

        /**
         * Bez závislosti na javafx.swing (SwingFXUtils): pixely se přepíší do BufferedImage ručně.
         */
        private static void writePng(WritableImage image, File file) throws IOException {
            int w = (int) image.getWidth();
            int h = (int) image.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    out.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(out, "png", file);
        }
    }
}
