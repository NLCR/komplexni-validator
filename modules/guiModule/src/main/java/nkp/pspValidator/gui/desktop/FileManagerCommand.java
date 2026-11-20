package nkp.pspValidator.gui.desktop;

import nkp.pspValidator.shared.OperatingSystem;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Příkaz, který otevře adresář ve správci souborů (Finder, Průzkumník, správce souborů Linuxu), nebo v něm ukáže
 * soubor („Zobrazit ve složce“). Jen sestavení příkazu podle OS, bez JavaFX a bez spouštění (spouští
 * {@code FileManager} v gui mimo vlákno JavaFX).
 * <ul>
 * <li>macOS: {@code open <adresář>}, soubor {@code open -R <soubor>} (Finder ho označí),</li>
 * <li>Windows: {@code explorer <adresář>}, soubor {@code explorer /select, <soubor>},</li>
 * <li>Linux a ostatní: {@code xdg-open <adresář>}, u souboru se otevře nadřazený adresář (označení souboru správci
 * souborů jednotně nepodporují).</li>
 * </ul>
 * Cesta je vždy jeden samostatný argument (mezery ani diakritiku není třeba escapovat), absolutní.
 */
public final class FileManagerCommand {

    private FileManagerCommand() {
    }

    /** Otevře adresář ve správci souborů. */
    public static List<String> openDirectory(OperatingSystem os, File dir) {
        return openDirectory(os, dir.getAbsolutePath());
    }

    /**
     * Příkaz pro otevření adresáře; cesta už je absolutní a ve tvaru cílového systému (sestavení příkazu tak nezávisí
     * na systému, na kterém se počítá).
     */
    static List<String> openDirectory(OperatingSystem os, String absolutePath) {
        switch (effective(os)) {
            case MAC:
                return list("open", absolutePath);
            case WINDOWS:
                return list("explorer", windowsQuoted(absolutePath));
            default:
                return list("xdg-open", absolutePath);
        }
    }

    /**
     * Ukáže soubor ve správci souborů (označený); kde to nejde, otevře adresář, ve kterém soubor je.
     */
    public static List<String> revealFile(OperatingSystem os, File file) {
        File absolute = file.getAbsoluteFile();
        File parent = absolute.getParentFile();
        return revealFile(os, absolute.getPath(), (parent != null ? parent : absolute).getPath());
    }

    /**
     * Příkaz pro „Zobrazit ve složce“; cesty už jsou absolutní a ve tvaru cílového systému.
     *
     * @param parentPath adresář, ve kterém soubor je (u souboru v kořeni kořen)
     */
    static List<String> revealFile(OperatingSystem os, String absolutePath, String parentPath) {
        switch (effective(os)) {
            case MAC:
                return list("open", "-R", absolutePath);
            case WINDOWS:
                //explorer bere „/select,“ a cestu jako dva argumenty i s mezerou za čárkou
                return list("explorer", "/select,", windowsQuoted(absolutePath));
            default:
                return list("xdg-open", parentPath);
        }
    }

    /**
     * Zda návratový kód příkazu znamená chybu. Průzkumník ve Windows vrací 1 i při úspěchu, jeho kód se proto
     * nevyhodnocuje.
     */
    public static boolean isFailure(OperatingSystem os, int exitCode) {
        return effective(os) != OperatingSystem.WINDOWS && exitCode != 0;
    }

    /**
     * Cesta pro Průzkumník vždy v uvozovkách. Java na Windows obalí uvozovkami jen argument s mezerou, ale Průzkumník
     * bere jako oddělovač i čárku: cesta „C:\\logy\\a,b.xml“ bez uvozovek by se rozdělila a soubor by se neoznačil.
     * Argument, který už v uvozovkách je, předá Java beze změny. Uvozovka se v cestě na Windows vyskytnout nemůže.
     */
    static String windowsQuoted(String path) {
        return '"' + path + '"';
    }

    private static OperatingSystem effective(OperatingSystem os) {
        return os == null ? OperatingSystem.LINUX : os;
    }

    private static List<String> list(String... parts) {
        return Collections.unmodifiableList(Arrays.asList(parts));
    }
}
