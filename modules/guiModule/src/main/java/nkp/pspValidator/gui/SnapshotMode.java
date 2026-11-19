package nkp.pspValidator.gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import nkp.pspValidator.gui.newvalidation.OutputSettings;
import nkp.pspValidator.gui.newvalidation.ValidationRequest;
import nkp.pspValidator.gui.newvalidation.ValidationSource;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.gui.skipping.SkippingConfigurationDialog;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Vývojářský režim: po inicializaci validačních dat postupně otevře vybrané dialogy, uloží snímek každého z nich
 * do PNG a aplikaci ukončí. Slouží ke kontrole vzhledu napříč platformami (macOS, Windows) bez ručního klikání
 * a bez oprávnění ke snímání obrazovky; stejný příkaz lze pustit lokálně i v CI.
 * <p>
 * Zapíná se systémovou vlastností {@code -Dkv.snapshotDir=<adresář>}. Výběr dialogů volitelně přes
 * {@code -Dkv.snapshot=main,mainMenu,newValidation,skipping,externalUtils,about,dictionaries} (výchozí je vše),
 * prodleva před snímkem přes {@code -Dkv.snapshotDelayMs} (výchozí 800 ms, aby proběhl layout a asynchronní
 * inicializace dialogu). {@code mainMenu} otevře v hlavním okně menu Validovat a uloží snímek jeho rozbalovací nabídky.
 * <p>
 * Dialog Nová validace lze otevřít s předvyplněnou cestou ({@code -Dkv.snapshotPsp=<adresář nebo soubor .zip>})
 * a rozbalenou sekcí ({@code -Dkv.snapshotExpand=versions|tools|skipped}), aby šly nasnímat všechny stavy.
 * <p>
 * Skutečná validace z GUI: {@code -Dkv.snapshotValidate=<adresář balíku nebo soubor .zip>} a v seznamu kroků
 * {@code results} spustí validaci stejnou cestou jako dialog Nová validace (požadavek z uloženého nastavení verzí
 * a výstupů, {@code Main.launchValidation}), počká na její konec, vybere první sekci a pravidlo s problémem a uloží
 * snímek {@code results.png}. Volitelně {@code -Dkv.snapshotDisable=JHOVE,KAKADU} vypne nástroje pro tuto validaci
 * (jako zaškrtávátka v dialogu) a {@code -Dkv.snapshotRunningMs=<ms>} uloží i snímek {@code running.png} během běhu.
 * Krok {@code rerun} pak spustí „Validovat znovu“, počká na konec a uloží {@code rerun.png}; do výpisu napíše, zda
 * se znovu spustil tentýž požadavek. Krok {@code small} zmenší hlavní okno na 1000 × 640 a uloží {@code small.png}
 * (další kroky pak běží v malém okně). Krok {@code filtered} zapne filtr „chyby a varování, jen sekce a pravidla
 * s problémy“, vybere první problém a uloží {@code filtered.png}. Výsledek každé validace se vypíše na standardní výstup.
 * <p>
 * Soubor nastavení se výchozně čte z obvyklého umístění (při spuštění z Gradle je to {@code config.properties}
 * v pracovním adresáři modulu), takže snímky odrážejí osobní nastavení vývojáře. Pro snímky nad čistým nastavením
 * lze zadat vlastní soubor systémovou vlastností {@code -Dkv.configFile=<soubor>} (nemusí existovat, vznikne s výchozími
 * hodnotami). Při spuštění z Gradle ale chybí výchozí cesta ke konfiguraci validace, proto soubor musí obsahovat alespoň
 * {@code validatorConfig.dir=<adresář validatorConfig>} (například
 * {@code modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig}) a nejlépe i
 * {@code validation.log_dir}, jinak aplikace čeká v dialogu inicializace validačních dat.
 * <p>
 * Přes gradle: {@code ./gradlew :modules:guiModule:runSnapshots -PsnapshotDir=/tmp/snap [-PsnapshotConfigFile=/tmp/snap-config.properties]}.
 */
public class SnapshotMode {

    public static final String PROP_DIR = "kv.snapshotDir";
    public static final String PROP_DIALOGS = "kv.snapshot";
    public static final String PROP_DELAY_MS = "kv.snapshotDelayMs";
    public static final String PROP_PSP = "kv.snapshotPsp";
    public static final String PROP_EXPAND = "kv.snapshotExpand";
    public static final String PROP_VALIDATE = "kv.snapshotValidate";
    public static final String PROP_DISABLE = "kv.snapshotDisable";
    public static final String PROP_RUNNING_MS = "kv.snapshotRunningMs";
    private static final long VALIDATION_TIMEOUT_MS = 10 * 60 * 1000;
    public static final String ALL_DIALOGS = "main,mainMenu,newValidation,skipping,externalUtils,about,dictionaries";

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
        String psp = System.getProperty(PROP_PSP);
        NewValidationDialog.Expand expand = parseExpand(System.getProperty(PROP_EXPAND));
        dialogs.put("newValidation", () -> new NewValidationDialog(dialogStage, main, psp, expand));
        dialogs.put("skipping", () -> new SkippingConfigurationDialog(dialogStage, main));
        dialogs.put("externalUtils", () -> new ExternalUtilsCheckDialog(dialogStage, main, false, "Pokračovat", true));
        dialogs.put("about", () -> new AboutAppDialog(dialogStage, main));
        dialogs.put("dictionaries", () -> new DictionariesConfigurationDialog(dialogStage, main));

        System.out.println("SNAPSHOT MODE: dir=" + dir.getAbsolutePath() + ", dialogs=" + names + ", delay=" + delayMs + " ms");
        new Runner(main, dir, delayMs, names, dialogs).next();
    }

    private static NewValidationDialog.Expand parseExpand(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        switch (value.trim()) {
            case "versions":
                return NewValidationDialog.Expand.VERSIONS;
            case "tools":
                return NewValidationDialog.Expand.TOOLS;
            case "skipped":
                return NewValidationDialog.Expand.SKIPPED;
            default:
                System.err.println("SNAPSHOT MODE: unknown " + PROP_EXPAND + " '" + value + "', known: versions,tools,skipped");
                return null;
        }
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
            if (!"rerun".equals(name) && !"results".equals(name)) {
                //ostatni kroky ocekavaji klid (zadna bezici validace)
                MainController controller = main.getMainController();
                if (controller.isValidationRunning()) {
                    queue.add(0, name);
                    after(this::next);
                    return;
                }
            }
            try {
                if ("main".equals(name)) {
                    Stage stage = main.getPrimaryStage();
                    after(() -> {
                        save(stage.getScene(), name);
                        next();
                    });
                } else if ("mainMenu".equals(name)) {
                    Menu menu = findMenu(main.getPrimaryStage().getScene(), "Validovat");
                    if (menu == null) {
                        System.err.println("SNAPSHOT MODE: menu Validovat not found");
                        failed = true;
                        next();
                        return;
                    }
                    menu.show();
                    after(() -> {
                        for (Window window : new ArrayList<>(Window.getWindows())) {
                            if (window instanceof PopupWindow && window.isShowing() && window.getScene() != null) {
                                save(window.getScene(), name);
                            }
                        }
                        menu.hide();
                        next();
                    });
                } else if ("results".equals(name)) {
                    runValidation(name);
                } else if ("rerun".equals(name)) {
                    rerun(name);
                } else if ("filtered".equals(name)) {
                    MainController controller = main.getMainController();
                    controller.setFilterChecks(true, true, false, true);
                    controller.selectFirstProblem();
                    after(() -> {
                        System.out.println("SNAPSHOT MODE: filtered: " + controller.describeShownResult());
                        save(main.getPrimaryStage().getScene(), name);
                        next();
                    });
                } else if ("small".equals(name)) {
                    Stage stage = main.getPrimaryStage();
                    stage.setWidth(1000);
                    stage.setHeight(640);
                    after(() -> {
                        System.out.println("SNAPSHOT MODE: window " + (int) stage.getWidth() + "x" + (int) stage.getHeight()
                                + ", scene " + (int) stage.getScene().getWidth() + "x" + (int) stage.getScene().getHeight());
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
                    System.err.println("SNAPSHOT MODE: unknown dialog '" + name + "', known: main,mainMenu,results,rerun,filtered,small," + String.join(",", dialogs.keySet()));
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

        private void runValidation(String name) {
            String path = System.getProperty(PROP_VALIDATE);
            if (path == null || path.trim().isEmpty()) {
                System.err.println("SNAPSHOT MODE: step 'results' needs -D" + PROP_VALIDATE + "=<package dir or zip>");
                failed = true;
                next();
                return;
            }
            ValidationSource source = path.trim().toLowerCase().endsWith(".zip") ? ValidationSource.ZIP : ValidationSource.DIRECTORY;
            ConfigurationManager config = main.getConfigurationManager();
            FdmfRegistry registry = main.getValidationDataManager().getFdmfRegistry();
            ValidationRequest request = ValidationRequest.create(source, path, new VersionSettings(config, registry),
                    new OutputSettings(config), parseDisabled(System.getProperty(PROP_DISABLE)));
            System.out.println("SNAPSHOT MODE: validating " + describe(request));
            MainController controller = main.getMainController();
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            //mimo animaci (u ZIPu se otevre modalni dialog rozbalovani)
            Platform.runLater(() -> main.launchValidation(request));
            String runningMs = System.getProperty(PROP_RUNNING_MS);
            if (runningMs != null) {
                PauseTransition pause = new PauseTransition(Duration.millis(Long.parseLong(runningMs)));
                pause.setOnFinished(e -> {
                    System.out.println("SNAPSHOT MODE: while running: " + controller.describeShownResult());
                    save(main.getPrimaryStage().getScene(), "running");
                });
                pause.play();
            }
            waitForEnd(controller, startedBefore, started, () -> afterValidation(name, controller, started));
        }

        private void rerun(String name) {
            MainController controller = main.getMainController();
            ValidationRequest before = controller.getLastRequest();
            boolean available = controller.canRerun();
            System.out.println("SNAPSHOT MODE: rerun available=" + available + ", last request: " + (before == null ? "-" : describe(before)));
            if (!available) {
                failed = true;
                next();
                return;
            }
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            Platform.runLater(() -> controller.rerunValidation(null));
            waitForEnd(controller, startedBefore, started, () -> {
                ValidationRequest after = controller.getLastRequest();
                System.out.println("SNAPSHOT MODE: rerun used the same request object: " + (after == before)
                        + ", request: " + (after == null ? "-" : describe(after)));
                afterValidation(name, controller, started);
            });
        }

        /** Čeká, až validace začne a skončí (dokončená, chyba nebo zrušení). */
        private void waitForEnd(MainController controller, int startedBefore, long started, Runnable then) {
            PauseTransition pause = new PauseTransition(Duration.millis(250));
            pause.setOnFinished(e -> {
                if (controller.getStartedValidations() > startedBefore && !controller.isValidationRunning()) {
                    then.run();
                } else if (System.currentTimeMillis() - started > VALIDATION_TIMEOUT_MS) {
                    System.err.println("SNAPSHOT MODE: validation timeout");
                    failed = true;
                    next();
                } else {
                    waitForEnd(controller, startedBefore, started, then);
                }
            });
            pause.play();
        }

        private void afterValidation(String name, MainController controller, long started) {
            System.out.println("SNAPSHOT MODE: validation ended after " + (System.currentTimeMillis() - started) + " ms");
            controller.selectFirstProblem();
            after(() -> {
                System.out.println("SNAPSHOT MODE: shown: " + controller.describeShownResult());
                save(main.getPrimaryStage().getScene(), name);
                next();
            });
        }

        private static Set<ExternalUtil> parseDisabled(String value) {
            Set<ExternalUtil> result = EnumSet.noneOf(ExternalUtil.class);
            if (value != null) {
                for (String item : value.split("\\s*,\\s*")) {
                    if (!item.trim().isEmpty()) {
                        result.add(ExternalUtil.valueOf(item.trim()));
                    }
                }
            }
            return result;
        }

        private static String describe(ValidationRequest request) {
            return request.getSource() + " " + request.getFile().getAbsolutePath()
                    + ", txtLog=" + request.isCreateTxtLog() + ", xmlLog=" + request.isCreateXmlLog()
                    + ", verbosity=" + request.getVerbosity() + ", disabledTools=" + request.getDisabledUtils()
                    + ", forcedFundUnit=" + request.getParams().forcedDmfFduVersion;
        }

        private static Menu findMenu(Scene scene, String text) {
            for (Node node : scene.getRoot().lookupAll(".menu-bar")) {
                if (node instanceof MenuBar) {
                    for (Menu menu : ((MenuBar) node).getMenus()) {
                        if (text.equals(menu.getText())) {
                            return menu;
                        }
                    }
                }
            }
            return null;
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
