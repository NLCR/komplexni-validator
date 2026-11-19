package nkp.pspValidator.gui.newvalidation;

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

    private ToolStatus(State state, String version, String detail) {
        this.state = state;
        this.version = version;
        this.detail = detail;
    }

    public static ToolStatus checking() {
        return new ToolStatus(State.CHECKING, null, null);
    }

    public static ToolStatus found(String version) {
        return new ToolStatus(State.FOUND, version, null);
    }

    public static ToolStatus notFound(String detail) {
        return new ToolStatus(State.NOT_FOUND, null, detail);
    }

    public static ToolStatus error(String detail) {
        return new ToolStatus(State.ERROR, null, detail);
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
