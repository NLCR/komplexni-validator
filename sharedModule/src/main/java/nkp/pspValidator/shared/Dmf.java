package nkp.pspValidator.shared;

/**
 * Created by martin on 2.11.16.
 */
public class Dmf {

    public static enum Type {
        MONOGRAPH, PERIODICAL;

        @Override
        public String toString() {
            switch (this) {
                case MONOGRAPH:
                    return "Monograph";
                case PERIODICAL:
                    return "Periodical";
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private final Type type;
    private final String version;


    public Dmf(Type type, String version) {
        this.type = type;
        this.version = version;
    }

    public Type getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String toString() {
        return type + " " + version;
    }
}
