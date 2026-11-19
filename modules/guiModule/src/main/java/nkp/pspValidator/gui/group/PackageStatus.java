package nkp.pspValidator.gui.group;

/**
 * Stav balíku ve skupině. Bez JavaFX.
 */
public enum PackageStatus {

    /** ještě se nevalidoval (běh skupiny k němu nedošel) */
    WAITING("čeká", "status-waiting"),
    /** právě se validuje */
    RUNNING("validuje se", "status-running"),
    /** validace doběhla, jádro balík označilo za validní */
    VALID("validní", "status-valid"),
    /** validace doběhla, jádro balík označilo za nevalidní */
    INVALID("nevalidní", "status-invalid"),
    /** balík nešlo validovat (není balík, typ nelze rozpoznat, chybí fDMF, chyba ZIPu, výjimka během validace) */
    ERROR("chyba", "status-error"),
    /** validace balíku byla zrušena během běhu */
    CANCELED("zrušeno", "status-waiting"),
    /** skupina byla zrušena dřív, než na balík došla řada */
    NOT_VALIDATED("nevalidováno", "status-waiting");

    private final String label;
    private final String styleClass;

    PackageStatus(String label, String styleClass) {
        this.label = label;
        this.styleClass = styleClass;
    }

    /** Text do tabulky a CSV. */
    public String getLabel() {
        return label;
    }

    /** Třída štítku v tabulce (base.css). */
    public String getStyleClass() {
        return styleClass;
    }

    /** Validace balíku skončila (s verdiktem, chybou nebo zrušením) nebo se na balík už nedostane. */
    public boolean isDone() {
        return this != WAITING && this != RUNNING;
    }

    /**
     * Pořadí při řazení podle výsledku: nejdřív balíky, které potřebují pozornost (nevalidní, s chybou), pak zrušené
     * a validní, nakonec ty, které výsledek ještě nemají.
     */
    public int sortRank() {
        switch (this) {
            case INVALID:
                return 0;
            case ERROR:
                return 1;
            case CANCELED:
                return 2;
            case VALID:
                return 3;
            case RUNNING:
                return 4;
            case WAITING:
                return 5;
            case NOT_VALIDATED:
            default:
                return 6;
        }
    }

    /** Pro filtr „jen nevalidní“: nevalidní balík nebo balík, který nešlo validovat. */
    public boolean isProblem() {
        return this == INVALID || this == ERROR;
    }
}
