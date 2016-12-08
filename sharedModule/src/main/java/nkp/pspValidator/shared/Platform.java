package nkp.pspValidator.shared;

/**
 * Created by Martin Řehánek on 28.9.16.
 */
public class Platform {

    private final OperatingSystem operatingSystem;
    private final OsArchitecture osArchitecture;

    private final String sysPropertyOsName;
    private final String sysPropertyOsArch;


    public Platform(OperatingSystem operatingSystem, OsArchitecture osArchitecture, String sysPropertyOsName, String sysPropertyOsArch) {
        this.operatingSystem = operatingSystem;
        this.osArchitecture = osArchitecture;
        this.sysPropertyOsName = sysPropertyOsName;
        this.sysPropertyOsArch = sysPropertyOsArch;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public OsArchitecture getOsArchitecture() {
        return osArchitecture;
    }

    public String getSysPropertyOsName() {
        return sysPropertyOsName;
    }

    public String getSysPropertyOsArch() {
        return sysPropertyOsArch;
    }

    public static Platform detectOs() {
        //http://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java
        String osName = System.getProperty("os.name");
        OperatingSystem operatingSystem = null;

        if (osName.startsWith("Windows")) {
            operatingSystem = OperatingSystem.WINDOWS;
        } else if (osName.startsWith("Linux")) {
            operatingSystem = OperatingSystem.LINUX;
        } else if (osName.startsWith("Mac")) {
            operatingSystem = OperatingSystem.MAC;
        }

        //http://stackoverflow.com/questions/10846105/all-possible-values-os-arch-in-32bit-jre-and-in-64bit-jre
        String osArch = System.getProperty("os.arch");
        OsArchitecture osArchitecture = null;
        if (osArch.contains("32") || osArch.contains("x86")) {
            osArchitecture = OsArchitecture.B32;
        } else if (osArch.contains("64")) {
            osArchitecture = OsArchitecture.B64;
        }

        return new Platform(operatingSystem, osArchitecture, osName, osArch);

    }

    @Override
    public String toString() {
        return "Platform{" +
                "operatingSystem=" + operatingSystem +
                ", osArchitecture=" + osArchitecture +
                ", sysPropertyOsName='" + sysPropertyOsName + '\'' +
                ", sysPropertyOsArch='" + sysPropertyOsArch + '\'' +
                '}';
    }

    public String toReadableString() {
        if (operatingSystem != null) {
            switch (operatingSystem) {
                case LINUX:
                    return "Linux; " + toArchitectureString();
                case WINDOWS:
                    return "Windows; " + toArchitectureString();
                case MAC:
                    return "Mac; " + toArchitectureString();
            }
        }
        return "unknown OS (" + sysPropertyOsName + "); " + toArchitectureString();
    }

    private String toArchitectureString() {
        if (osArchitecture != null) {
            switch (osArchitecture) {
                case B32:
                    return "32b";
                case B64:
                    return "64b";
                default:
            }
        }
        return "unkown architecture(" + sysPropertyOsArch + ")";
    }
}
