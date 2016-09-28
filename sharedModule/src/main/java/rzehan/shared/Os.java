package rzehan.shared;

/**
 * Created by Martin Řehánek on 28.9.16.
 */
public class Os {

    public enum OsType {

        LINUX, WINDOWS, MAC, UNKNOWN;

    }

    public enum Architecture {

        B32, B64, UNKNOWN;

    }

    private final OsType osType;
    private final Architecture architecture;

    private final String sysPropertyOsName;
    private final String sysPropertyOsArch;


    public Os(OsType osType, Architecture architecture, String sysPropertyOsName, String sysPropertyOsArch) {
        this.osType = osType;
        this.architecture = architecture;
        this.sysPropertyOsName = sysPropertyOsName;
        this.sysPropertyOsArch = sysPropertyOsArch;
    }

    public OsType getOsType() {
        return osType;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public String getSysPropertyOsName() {
        return sysPropertyOsName;
    }

    public String getSysPropertyOsArch() {
        return sysPropertyOsArch;
    }

    public static Os detectOs() {
        //http://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java
        String osName = System.getProperty("os.name");
        OsType osType = OsType.UNKNOWN;

        if (osName.startsWith("Windows")) {
            osType = OsType.WINDOWS;
        } else if (osName.startsWith("Linux")) {
            osType = OsType.LINUX;
        } else if (osName.startsWith("Mac")) {
            osType = OsType.MAC;
        }

        //http://stackoverflow.com/questions/10846105/all-possible-values-os-arch-in-32bit-jre-and-in-64bit-jre
        String osArch = System.getProperty("os.arch");
        Architecture architecture = Architecture.UNKNOWN;
        if (osArch.contains("32")) {
            architecture = Architecture.B32;
        } else if (osArch.contains("64")) {
            architecture = Architecture.B64;
        }

        return new Os(osType, architecture, osName, osArch);

    }

    @Override
    public String toString() {
        return "Os{" +
                "osType=" + osType +
                ", architecture=" + architecture +
                ", sysPropertyOsName='" + sysPropertyOsName + '\'' +
                ", sysPropertyOsArch='" + sysPropertyOsArch + '\'' +
                '}';
    }

    public String toReadableString() {
        switch (osType) {
            case LINUX:
                return "Linux; " + toArchitectureString();
            case WINDOWS:
                return "Windows; " + toArchitectureString();
            default:
                return "Unknown (" + sysPropertyOsName + "); " + toArchitectureString();
        }
    }

    private String toArchitectureString() {
        switch (architecture) {
            case B32:
                return "32b";
            case B64:
                return "64b";
            default:
                return "unkown (" + sysPropertyOsArch + ")";
        }
    }
}
