package nkp.pspValidator.gui.results;

import nkp.pspValidator.gui.validation.ProcessingState;

/**
 * Stav sekce nebo pravidla v seznamech hlavního okna: barva tečky a krátký text. Bez JavaFX.
 */
public enum ItemStatus {

    WAITING("status-dot-none", "čeká"),
    RUNNING("status-dot-running", "probíhá"),
    SKIPPED("status-dot-none", "přeskočeno"),
    CANCELED("status-dot-none", "zrušeno"),
    INTERRUPTED("status-dot-none", "nedokončeno"),
    OK("status-dot-ok", null),
    WARNING("status-dot-warning", null),
    ERROR("status-dot-error", null);

    private final String dotStyleClass;
    private final String text;

    ItemStatus(String dotStyleClass, String text) {
        this.dotStyleClass = dotStyleClass;
        this.text = text;
    }

    /**
     * Dokončená položka je chybová, když má chybu, s varováním, když má varování, jinak v pořádku (informace
     * na stav nemají vliv, stejně jako na validitu balíku).
     */
    public static ItemStatus of(ProcessingState state, int errors, int warnings) {
        switch (state) {
            case WAITING:
                return WAITING;
            case RUNNING:
                return RUNNING;
            case SKIPPED:
                return SKIPPED;
            case CANCELED:
                return CANCELED;
            case INTERRUPTED:
                return INTERRUPTED;
            case FINISHED:
                if (errors > 0) {
                    return ERROR;
                } else if (warnings > 0) {
                    return WARNING;
                } else {
                    return OK;
                }
            default:
                throw new IllegalStateException("neznámý stav " + state);
        }
    }

    /** Třída tečky v base.css. */
    public String getDotStyleClass() {
        return dotStyleClass;
    }

    /** Text vpravo v řádku místo počtů, null u dokončené položky (ta ukazuje počty). */
    public String getText() {
        return text;
    }
}
