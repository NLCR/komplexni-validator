package nkp.pspValidator.gui;

import javafx.application.Platform;
import nkp.pspValidator.gui.desktop.RestartCommand;
import nkp.pspValidator.gui.desktop.Restarter;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Restart a ukončení aplikace z Nastavení (po změně adresáře konfigurace validátoru). Příkaz sestaví
 * {@link RestartCommand}, spustí {@link Restarter}; aplikace se ukončí, až nová instance běží. Ve snímkovacím režimu
 * se nic nespouští ani neukončuje, jen se vypíše, co by se stalo.
 */
final class AppRestart {

    private AppRestart() {
    }

    /** Příkaz pro nové spuštění běžící aplikace, null když ho sestavit nejde. */
    static RestartCommand.Plan plan(Main main) {
        return RestartCommand.build(RestartCommand.Environment.current(appArgs(main)));
    }

    private static List<String> appArgs(Main main) {
        javafx.application.Application.Parameters parameters = main.getParameters();
        return parameters == null ? Collections.emptyList() : parameters.getRaw();
    }

    /**
     * Spustí novou instanci a tuto ukončí. Volat z vlákna JavaFX.
     *
     * @param onError chyba spuštění (ve vlákně JavaFX); aplikace pak běží dál
     * @return false ve snímkovacím režimu (nic se nespustilo), jinak true (spouští se)
     */
    static boolean restart(RestartCommand.Plan plan, Consumer<String> onError) {
        if (SnapshotMode.isEnabled()) {
            System.out.println("SNAPSHOT MODE: restart (not executed): " + plan.getCommand()
                    + (plan.getWorkingDir() != null ? ", working dir " + plan.getWorkingDir() : "")
                    + (plan.exitsAfterHandOver() ? ", launcher hands over and exits" : ""));
            return false;
        }
        Restarter.start(plan, error -> Platform.runLater(() -> {
            if (error != null) {
                onError.accept(error);
            } else {
                exit();
            }
        }));
        return true;
    }

    /** Ukončí aplikaci (i když běží jiná vlákna než JavaFX). */
    static void exit() {
        if (SnapshotMode.isEnabled()) {
            System.out.println("SNAPSHOT MODE: exit (not executed)");
            return;
        }
        Platform.exit();
        //vlakna mimo JavaFX (napr. kontrola nastroju) nesmi zdrzet konec; hacky pri ukonceni (uklid rozbalenych ZIPu) probehnou
        Thread exit = new Thread(() -> System.exit(0), "exit");
        exit.start();
    }
}
