package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Cesta ke knihovnám pro spouštěný nástroj: které adresáře se přidávají, do které proměnné a v jakém pořadí.
 * Očekávané hodnoty jsou složené z dočasných adresářů testu, oddělovač je vždy dvojtečka (macOS a Linux).
 */
public class LibraryPathEnvironmentTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File dir(File parent, String path) {
        File d = new File(parent, path);
        assertTrue(d.mkdirs());
        return d;
    }

    @Test
    public void variableByOperatingSystem() {
        assertEquals("DYLD_FALLBACK_LIBRARY_PATH", LibraryPathEnvironment.variableName(OperatingSystem.MAC));
        assertEquals("LD_LIBRARY_PATH", LibraryPathEnvironment.variableName(OperatingSystem.LINUX));
        assertNull(LibraryPathEnvironment.variableName(OperatingSystem.WINDOWS));
        assertNull(LibraryPathEnvironment.variableName(null));
    }

    /** Rozložení Kakadu: program v bin/<platforma>, knihovny v lib/<platforma>. */
    @Test
    public void kakaduLayout() throws IOException {
        File root = tmp.newFolder("kakadu");
        File bin = dir(root, "bin/Mac-arm-64-gcc");
        File lib = dir(root, "lib/Mac-arm-64-gcc");
        dir(root, "lib/Linux-x86-64-gcc");
        assertEquals(Arrays.asList(bin.getAbsoluteFile(), lib.getAbsoluteFile()), LibraryPathEnvironment.libraryDirs(bin));
    }

    /** Knihovny vedle programu (demo Kakadu) nebo v podadresáři lib. */
    @Test
    public void utilDirAndItsLibSubdirectory() throws IOException {
        File util = tmp.newFolder("nastroj");
        assertEquals(Collections.singletonList(util.getAbsoluteFile()), LibraryPathEnvironment.libraryDirs(util));
        File lib = dir(util, "lib");
        assertEquals(Arrays.asList(util.getAbsoluteFile(), lib.getAbsoluteFile()), LibraryPathEnvironment.libraryDirs(util));
    }

    /**
     * Obecný sousední adresář lib (např. /opt/homebrew/bin a /opt/homebrew/lib) se nepřidává: jsou v něm knihovny
     * jiných programů.
     */
    @Test
    public void siblingLibIsNotAdded() throws IOException {
        File prefix = tmp.newFolder("prefix");
        File bin = dir(prefix, "bin");
        dir(prefix, "lib");
        assertEquals(Collections.singletonList(bin.getAbsoluteFile()), LibraryPathEnvironment.libraryDirs(bin));
    }

    @Test
    public void missingOrNullUtilDir() {
        assertTrue(LibraryPathEnvironment.libraryDirs(null).isEmpty());
        assertTrue(LibraryPathEnvironment.libraryDirs(new File(tmp.getRoot(), "neexistuje")).isEmpty());
        assertTrue(LibraryPathEnvironment.forUtil(OperatingSystem.MAC, null, null).isEmpty());
        assertTrue(LibraryPathEnvironment.forUtil(OperatingSystem.LINUX, new File(tmp.getRoot(), "neexistuje"), "/x").isEmpty());
    }

    @Test
    public void macUsesFallbackVariableAndKeepsSystemDefault() throws IOException {
        File root = tmp.newFolder("kakadu");
        File bin = dir(root, "bin/Mac-arm-64-gcc");
        File lib = dir(root, "lib/Mac-arm-64-gcc");
        Map<String, String> env = LibraryPathEnvironment.forUtil(OperatingSystem.MAC, bin, null);
        assertEquals(Collections.singleton("DYLD_FALLBACK_LIBRARY_PATH"), env.keySet());
        assertEquals(bin.getAbsolutePath() + ":" + lib.getAbsolutePath() + ":/usr/local/lib:/usr/lib",
                env.get("DYLD_FALLBACK_LIBRARY_PATH"));
        //prazdna hodnota se bere jako nenastavena
        assertEquals(env, LibraryPathEnvironment.forUtil(OperatingSystem.MAC, bin, "  "));
    }

    /** Hodnota nastavená uživatelem zůstává první, adresáře nástroje se připojí za ni a výchozí cesta se nepřidává. */
    @Test
    public void existingValueStaysFirst() throws IOException {
        File util = tmp.newFolder("nastroj");
        assertEquals("/moje/knihovny:" + util.getAbsolutePath(),
                LibraryPathEnvironment.forUtil(OperatingSystem.MAC, util, "/moje/knihovny").get("DYLD_FALLBACK_LIBRARY_PATH"));
        assertEquals("/moje/knihovny:/dalsi:" + util.getAbsolutePath(),
                LibraryPathEnvironment.forUtil(OperatingSystem.LINUX, util, "/moje/knihovny:/dalsi").get("LD_LIBRARY_PATH"));
    }

    @Test
    public void linuxWithoutExistingValue() throws IOException {
        File util = tmp.newFolder("nastroj");
        Map<String, String> env = LibraryPathEnvironment.forUtil(OperatingSystem.LINUX, util, null);
        assertEquals(Collections.singleton("LD_LIBRARY_PATH"), env.keySet());
        assertEquals(util.getAbsolutePath(), env.get("LD_LIBRARY_PATH"));
    }

    @Test
    public void windowsAndUnknownSystemSetNothing() throws IOException {
        File util = tmp.newFolder("nastroj");
        assertTrue(LibraryPathEnvironment.forUtil(OperatingSystem.WINDOWS, util, null).isEmpty());
        assertTrue(LibraryPathEnvironment.forUtil(null, util, "/x").isEmpty());
    }
}
