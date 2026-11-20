package nkp.pspValidator.gui.desktop;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spustí příkaz {@link FileManagerCommand} (Finder, Průzkumník, xdg-open) v samostatném vlákně, aby čekání na
 * správce souborů nikdy neblokovalo vlákno JavaFX. Bez JavaFX a bez AWT.
 */
public final class FileManager {

    private static final Logger LOG = Logger.getLogger(FileManager.class.getSimpleName());
    private static final long WAIT_SECONDS = 15;

    /** Výsledek spuštění příkazu (pro volající, který chce chybu ukázat, a pro kontrolu ve snímkovacím režimu). */
    public interface Callback {
        /** @param error null při úspěchu, jinak popis chyby */
        void done(List<String> command, String error);
    }

    private FileManager() {
    }

    /** Otevře adresář ve správci souborů. */
    public static void openDirectory(File dir, Callback callback) {
        run(FileManagerCommand.openDirectory(os(), dir), callback);
    }

    /** Ukáže soubor ve správci souborů (na Linuxu otevře jeho adresář). */
    public static void revealFile(File file, Callback callback) {
        run(FileManagerCommand.revealFile(os(), file), callback);
    }

    private static OperatingSystem os() {
        return Platform.detectOs().getOperatingSystem();
    }

    private static void run(List<String> command, Callback callback) {
        Thread thread = new Thread(() -> {
            String error = execute(command);
            if (error != null) {
                LOG.log(Level.WARNING, "{0}: {1}", new Object[]{command, error});
            }
            if (callback != null) {
                callback.done(command, error);
            }
        }, "file-manager");
        thread.setDaemon(true);
        thread.start();
    }

    /** Spustí příkaz a počká na něj; vrátí null, nebo popis chyby. */
    static String execute(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
                //spravce souboru se mohl otevrit, jen prikaz nekonci; nechat ho bezet
                return null;
            }
            int exit = process.exitValue();
            return FileManagerCommand.isFailure(os(), exit) ? "návratový kód " + exit : null;
        } catch (IOException e) {
            return e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return e.getMessage();
        }
    }
}
