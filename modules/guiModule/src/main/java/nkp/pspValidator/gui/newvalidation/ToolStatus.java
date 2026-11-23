package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.externalUtils.VersionCheck;

/**
 * Stav externího nástroje zjištěný detekcí verze (hodnota).
 */
public class ToolStatus {

    public enum State {
        CHECKING, FOUND, NOT_FOUND, ERROR
    }

    private final State state;
    private final String version;
    private final String detail;
    private final VersionCheck versionCheck;

    private ToolStatus(State state, String version, String detail, VersionCheck versionCheck) {
        this.state = state;
        this.version = version;
        this.detail = detail;
        this.versionCheck = versionCheck;
    }

    public static ToolStatus checking() {
        return new ToolStatus(State.CHECKING, null, null, null);
    }

    public static ToolStatus found(String version) {
        return found(version, null);
    }

    /**
     * Nalezený nástroj s porovnáním verze s ověřenými řadami.
     *
     * @param versionCheck porovnání verze, nebo null (verze se neoznačuje)
     */
    public static ToolStatus found(String version, VersionCheck versionCheck) {
        return new ToolStatus(State.FOUND, version, null, versionCheck);
    }

    public static ToolStatus notFound(String detail) {
        return new ToolStatus(State.NOT_FOUND, null, detail, null);
    }

    public static ToolStatus error(String detail) {
        return new ToolStatus(State.ERROR, null, detail, null);
    }

    public State getState() {
        return state;
    }

    public boolean isAvailable() {
        return state == State.FOUND;
    }

    public String getVersion() {
        return version;
    }

    /** Porovnání verze nalezeného nástroje s ověřenými řadami, nebo null. */
    public VersionCheck getVersionCheck() {
        return versionCheck;
    }

    /** Je verze nalezeného nástroje neověřená nebo nerozpoznaná (stav se označí, nástroj zůstává dostupný)? */
    public boolean isVersionWarning() {
        return state == State.FOUND && versionCheck != null && versionCheck.isWarning();
    }

    /** Podrobnost chyby (výstup nebo hláška při spuštění), jinak null. */
    public String getDetail() {
        return detail;
    }

    /** Krátký text stavu do řádku nástroje. */
    public String getText() {
        switch (state) {
            case CHECKING:
                return "zjišťuji…";
            case FOUND:
                return version == null || version.isEmpty() ? "nalezen" : "nalezen, " + version;
            case NOT_FOUND:
                return "nenalezen";
            case ERROR:
                return "chyba při spuštění";
            default:
                throw new IllegalStateException();
        }
    }
}
