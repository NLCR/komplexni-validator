package nkp.pspValidator.gui.desktop;

import nkp.pspValidator.shared.OperatingSystem;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Příkazy pro otevření adresáře a „Zobrazit ve složce“ podle OS. Cesty jsou absolutní a vždy jeden argument, i s mezerami
 * a diakritikou (ProcessBuilder je předá beze změny, na Windows v uvozovkách).
 */
public class FileManagerCommandTest {

    private static final File DIR = new File("/Users/uživatel/Protokoly validace");
    private static final File FILE = new File("/Users/uživatel/Protokoly validace/nk-00027x 2026 č.1.xml");

    @Test
    public void macOpensDirectoryInFinder() {
        assertEquals(Arrays.asList("open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.openDirectory(OperatingSystem.MAC, DIR));
    }

    @Test
    public void macRevealsFileInFinder() {
        assertEquals(Arrays.asList("open", "-R", "/Users/uživatel/Protokoly validace/nk-00027x 2026 č.1.xml"),
                FileManagerCommand.revealFile(OperatingSystem.MAC, FILE));
    }

    @Test
    public void windowsOpensDirectoryInExplorer() {
        File dir = new File("C:\\Users\\Uživatel\\Dokumenty\\Protokoly validace");
        List<String> command = FileManagerCommand.openDirectory(OperatingSystem.WINDOWS, dir);
        assertEquals(2, command.size());
        assertEquals("explorer", command.get(0));
        assertEquals("\"" + dir.getAbsolutePath() + "\"", command.get(1));
    }

    @Test
    public void windowsSelectsFileInExplorer() {
        File file = new File("C:\\Users\\Uživatel\\Protokoly validace\\nk 1.xml");
        List<String> command = FileManagerCommand.revealFile(OperatingSystem.WINDOWS, file);
        assertEquals(Arrays.asList("explorer", "/select,", "\"" + file.getAbsolutePath() + "\""), command);
    }

    /** Čárka v cestě: Průzkumník ji bere jako oddělovač, cesta proto musí být v uvozovkách i bez mezer. */
    @Test
    public void windowsQuotesPathWithCommaAndNoSpaces() {
        assertEquals("\"C:\\logy\\a,b.xml\"", FileManagerCommand.windowsQuoted("C:\\logy\\a,b.xml"));
    }

    @Test
    public void linuxOpensDirectoryWithXdgOpen() {
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.openDirectory(OperatingSystem.LINUX, DIR));
    }

    @Test
    public void linuxRevealOpensParentDirectory() {
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.revealFile(OperatingSystem.LINUX, FILE));
    }

    @Test
    public void unknownOsBehavesLikeLinux() {
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.openDirectory(null, DIR));
        assertEquals(Arrays.asList("xdg-open", "/Users/uživatel/Protokoly validace"),
                FileManagerCommand.revealFile(null, FILE));
    }

    @Test
    public void relativePathBecomesAbsolute() {
        File relative = new File("protokoly/x.xml");
        List<String> command = FileManagerCommand.revealFile(OperatingSystem.MAC, relative);
        assertEquals(new File(new File(System.getProperty("user.dir"), "protokoly"), "x.xml").getPath(), command.get(2));
        assertTrue(new File(command.get(2)).isAbsolute());
        List<String> linux = FileManagerCommand.revealFile(OperatingSystem.LINUX, relative);
        assertEquals(new File(System.getProperty("user.dir"), "protokoly").getPath(), linux.get(1));
    }

    @Test
    public void fileInRootOnLinuxOpensRoot() {
        assertEquals(Arrays.asList("xdg-open", "/"), FileManagerCommand.revealFile(OperatingSystem.LINUX, new File("/x.xml")));
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
            FileManagerCommand.openDirectory(OperatingSystem.MAC, DIR).add("x");
            fail();
        } catch (UnsupportedOperationException e) {
            //ok
        }
    }
}
