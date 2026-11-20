package nkp.pspValidator.gui.desktop;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spustí novou instanci aplikace příkazem {@link RestartCommand} v samostatném vlákně (vlákno JavaFX se nečeká). Nový
 * proces je odpojený od končícího: výstupy se zahazují a vstup se hned zavře, takže nedrží roury rodiče (při běhu přes
 * Gradle by jinak úloha {@code run} čekala na konec nové instance). Bez JavaFX.
 */
public final class Restarter {

    private static final Logger LOG = Logger.getLogger(Restarter.class.getSimpleName());
    /** jak dlouho musí nová instance běžet, aby se spuštění považovalo za úspěšné */
    private static final long ALIVE_CHECK_MS = 2000;
    /** jak dlouho se čeká na příkaz, který jen předá spuštění systému ({@code open -n}) */
    private static final long HAND_OVER_SECONDS = 15;

    public interface Callback {
        /** @param error null, když nová instance běží (nebo ji systém převzal), jinak popis chyby */
        void done(String error);
    }

    private Restarter() {
    }

    public static void start(RestartCommand.Plan plan, Callback callback) {
        Thread thread = new Thread(() -> {
            String error = launch(plan);
            if (error != null) {
                LOG.log(Level.WARNING, "{0}: {1}", new Object[]{plan, error});
            }
            callback.done(error);
        }, "restart");
        thread.setDaemon(true);
        thread.start();
    }

    static String launch(RestartCommand.Plan plan) {
        try {
            ProcessBuilder builder = new ProcessBuilder(plan.getCommand())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD);
            if (plan.getWorkingDir() != null && new File(plan.getWorkingDir()).isDirectory()) {
                builder.directory(new File(plan.getWorkingDir()));
            }
            Process process = builder.start();
            process.getOutputStream().close();
            if (plan.exitsAfterHandOver()) {
                if (!process.waitFor(HAND_OVER_SECONDS, TimeUnit.SECONDS)) {
                    return null;
                }
                int exit = process.exitValue();
                return exit == 0 ? null : "návratový kód " + exit;
            }
            if (process.waitFor(ALIVE_CHECK_MS, TimeUnit.MILLISECONDS)) {
                return "nová instance skončila hned po spuštění (návratový kód " + process.exitValue() + ")";
            }
            return null;
        } catch (IOException e) {
            return e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return e.getMessage();
        }
    }
}
