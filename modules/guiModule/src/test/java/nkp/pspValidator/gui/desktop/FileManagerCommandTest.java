package nkp.pspValidator.gui.desktop;

import nkp.pspValidator.shared.OperatingSystem;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Příkazy pro otevření adresáře a „Zobrazit ve složce“ podle OS. Cesty jsou absolutní a vždy jeden argument, i s mezerami
 * a diakritikou (ProcessBuilder je předá beze změny, na Windows v uvozovkách).
 */
public class FileManagerCommandTest {

    //cesty ve tvaru jednotlivych systemu jako retezce: sestaveni prikazu se testuje stejne na vsech platformach
    private static final String UNIX_DIR = "/Users/uživatel/Protokoly validace";
    private static final String UNIX_FILE = "/Users/uživatel/Protokoly validace/nk-00027x 2026 č.1.xml";
    private static final String WIN_DIR = "C:\\Users\\Uživatel\\Dokumenty\\Protokoly validace";
    private static final String WIN_FILE = "C:\\Users\\Uživatel\\Protokoly validace\\nk 1.xml";

    @Test
    public void macOpensDirectoryInFinder() {
        assertEquals(Arrays.asList("open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.openDirectory(OperatingSystem.MAC, UNIX_DIR));
    }

    @Test
    public void macRevealsFileInFinder() {
        assertEquals(Arrays.asList("open", "-R", "/Users/uživatel/Protokoly validace/nk-00027x 2026 č.1.xml"),
                FileManagerCommand.revealFile(OperatingSystem.MAC, UNIX_FILE, UNIX_DIR));
    }

    @Test
    public void windowsOpensDirectoryInExplorer() {
        assertEquals(Arrays.asList("explorer", "\"C:\\Users\\Uživatel\\Dokumenty\\Protokoly validace\""),
                FileManagerCommand.openDirectory(OperatingSystem.WINDOWS, WIN_DIR));
    }

    @Test
    public void windowsSelectsFileInExplorer() {
        assertEquals(Arrays.asList("explorer", "/select,", "\"C:\\Users\\Uživatel\\Protokoly validace\\nk 1.xml\""),
                FileManagerCommand.revealFile(OperatingSystem.WINDOWS, WIN_FILE, "C:\\Users\\Uživatel\\Protokoly validace"));
    }

    /** Čárka v cestě: Průzkumník ji bere jako oddělovač, cesta proto musí být v uvozovkách i bez mezer. */
    @Test
    public void windowsQuotesPathWithCommaAndNoSpaces() {
        assertEquals("\"C:\\logy\\a,b.xml\"", FileManagerCommand.windowsQuoted("C:\\logy\\a,b.xml"));
        assertEquals(Arrays.asList("explorer", "/select,", "\"C:\\logy\\a,b.xml\""),
                FileManagerCommand.revealFile(OperatingSystem.WINDOWS, "C:\\logy\\a,b.xml", "C:\\logy"));
    }

    @Test
    public void linuxOpensDirectoryWithXdgOpen() {
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.openDirectory(OperatingSystem.LINUX, UNIX_DIR));
    }

    @Test
    public void linuxRevealOpensParentDirectory() {
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.revealFile(OperatingSystem.LINUX, UNIX_FILE, UNIX_DIR));
    }

    @Test
    public void unknownOsBehavesLikeLinux() {
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.openDirectory(null, UNIX_DIR));
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.revealFile(null, UNIX_FILE, UNIX_DIR));
    }

    /**
     * Varianty se souborem: cesta se převede na absolutní a u „Zobrazit ve složce“ se určí nadřazený adresář.
     * Očekávané cesty jsou složené z pracovního adresáře, aby test platil na každém systému.
     */
    @Test
    public void fileVariantsUseAbsolutePathAndParent() {
        String cwd = System.getProperty("user.dir");
        String dir = cwd + File.separator + "protokoly";
        String file = dir + File.separator + "x.xml";
        File relative = new File("protokoly" + File.separator + "x.xml");
        assertEquals(Arrays.asList("open", "-R", file), FileManagerCommand.revealFile(OperatingSystem.MAC, relative));
        assertEquals(Arrays.asList("xdg-open", dir), FileManagerCommand.revealFile(OperatingSystem.LINUX, relative));
        assertEquals(Arrays.asList("explorer", "/select,", "\"" + file + "\""),
                FileManagerCommand.revealFile(OperatingSystem.WINDOWS, relative));
        assertEquals(Arrays.asList("open", dir), FileManagerCommand.openDirectory(OperatingSystem.MAC, new File("protokoly")));
        assertEquals(Arrays.asList("explorer", "\"" + dir + "\""),
                FileManagerCommand.openDirectory(OperatingSystem.WINDOWS, new File("protokoly")));
    }

    /** Soubor přímo v kořeni: nadřazeným adresářem je kořen (na Windows kořen jednotky). */
    @Test
    public void fileInRootOpensRoot() {
        File root = File.listRoots()[0];
        File inRoot = new File(root, "x.xml");
        assertEquals(Arrays.asList("xdg-open", root.getAbsolutePath()), FileManagerCommand.revealFile(OperatingSystem.LINUX, inRoot));
    }

    @Test
    public void exitCodes() {
        assertFalse(FileManagerCommand.isFailure(OperatingSystem.MAC, 0));
        assertTrue(FileManagerCommand.isFailure(OperatingSystem.MAC, 1));
        assertTrue(FileManagerCommand.isFailure(OperatingSystem.LINUX, 4));
        assertFalse(FileManagerCommand.isFailure(OperatingSystem.LINUX, 0));
        //pruzkumnik vraci 1 i pri uspechu
        assertFalse(FileManagerCommand.isFailure(OperatingSystem.WINDOWS, 1));
        assertFalse(FileManagerCommand.isFailure(OperatingSystem.WINDOWS, 0));
    }

    @Test
    public void commandIsUnmodifiable() {
        try {
            FileManagerCommand.openDirectory(OperatingSystem.MAC, UNIX_DIR).add("x");
            fail();
        } catch (UnsupportedOperationException e) {
            //ok
        }
    }
}
