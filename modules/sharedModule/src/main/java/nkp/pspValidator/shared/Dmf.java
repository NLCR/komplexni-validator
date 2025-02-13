package nkp.pspValidator.shared;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class Dmf implements Serializable {

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dmf)) return false;
        Dmf dmf = (Dmf) o;
        return type == dmf.type &&
                Objects.equals(version, dmf.version);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, version);
    }

    public enum Type implements Serializable {
        MONOGRAPH, PERIODICAL, AUDIO_GRAM, AUDIO_FONO;

        @Override
        public String toString() {
            switch (this) {
                case MONOGRAPH:
                    return "Monografie";
                case PERIODICAL:
                    return "Periodika";
                case AUDIO_GRAM:
                    return "Zvukové dokumenty (gramodesky)";
                case AUDIO_FONO:
                    return "Zvukové dokumenty (fonoválečky)";
                default:
                    throw new IllegalStateException();
            }
        }
    }

}
