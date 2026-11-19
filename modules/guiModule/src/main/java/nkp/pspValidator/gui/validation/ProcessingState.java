package nkp.pspValidator.gui.validation;

/**
 * Stav sekce nebo pravidla během validace. CANCELED: nedokončeno kvůli zrušení validace, INTERRUPTED: nedokončeno,
 * protože validace skončila chybou.
 * <p>
 * Created by Martin Řehánek on 16.12.16.
 */
public enum ProcessingState {
    WAITING, RUNNING, FINISHED, CANCELED, SKIPPED, INTERRUPTED;
}
