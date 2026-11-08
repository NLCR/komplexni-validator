package nkp.pspValidator.gui;

import nkp.pspValidator.gui.ConfigurationManager.Locations;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Výběr cest ke konfiguraci validace, config.properties a logům podle způsobu spuštění (jpackage nebo java -jar)
 * a OS. Bez JavaFX, jen dočasné adresáře.
 */
public class ConfigurationManagerLocationsTest {

    private static final String PROP_APP_PATH = "jpackage.app-path";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String origAppPath;
    private String origUserHome;
    private File home;
    private File cwd;

    @Before
    public void setUp() throws IOException {
        cwd = tmp.newFolder("cwd");
        origAppPath = System.getProperty(PROP_APP_PATH);
        origUserHome = System.getProperty("user.home");
        home = tmp.newFolder("home");
    }

    @After
    public void tearDown() {
        restore(PROP_APP_PATH, origAppPath);
        restore("user.home", origUserHome);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    /** jar v <instalace>/app (Windows), <instalace>/lib/app (Linux), <bundle>/Contents/app (macOS) */
    private File appJarDir(String relative) throws IOException {
        File dir = new File(tmp.getRoot(), relative);
        assertTrue(new File(dir, "validatorConfig").mkdirs());
        return dir;
    }

    @Test
    public void jpackageWindows() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/app");
        File appData = new File(home, "AppData/Roaming");
        Locations loc = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, appData.getPath(), null, cwd);
        assertTrue(loc.jpackage);
        assertEquals(new File(jarDir, "validatorConfig").getAbsoluteFile(), loc.defaultValidatorConfigDir);
        assertTrue(loc.defaultValidatorConfigDir.isAbsolute());
        assertEquals(new File(appData, "KomplexniValidator").getAbsoluteFile(), loc.userDataDir);
        assertEquals(new File(loc.userDataDir, "logs"), loc.defaultLogDir);
    }

    @Test
    public void jpackageWindowsWithoutAppData() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/app");
        Locations loc = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, null, null, cwd);
        assertEquals(new File(home, "AppData/Roaming/KomplexniValidator").getAbsoluteFile(), loc.userDataDir);
    }

    @Test
    public void jpackageLinux() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/lib/app");
        Locations loc = Locations.resolve(OperatingSystem.LINUX, true, jarDir, home, null, null, cwd);
        assertEquals(new File(jarDir, "validatorConfig").getAbsoluteFile(), loc.defaultValidatorConfigDir);
        assertEquals(new File(home, ".config/KomplexniValidator").getAbsoluteFile(), loc.userDataDir);

        File xdg = new File(tmp.getRoot(), "xdg");
        loc = Locations.resolve(OperatingSystem.LINUX, true, jarDir, home, null, xdg.getAbsolutePath(), cwd);
        assertEquals(new File(xdg, "KomplexniValidator").getAbsoluteFile(), loc.userDataDir);

        // relativní XDG_CONFIG_HOME se podle specifikace ignoruje
        loc = Locations.resolve(OperatingSystem.LINUX, true, jarDir, home, null, "relative/xdg", cwd);
        assertEquals(new File(home, ".config/KomplexniValidator").getAbsoluteFile(), loc.userDataDir);
    }

    @Test
    public void jpackageMac() throws IOException {
        File jarDir = appJarDir("KomplexniValidator.app/Contents/app");
        Locations loc = Locations.resolve(OperatingSystem.MAC, true, jarDir, home, null, null, cwd);
        assertEquals(new File(jarDir, "validatorConfig").getAbsoluteFile(), loc.defaultValidatorConfigDir);
        assertEquals(new File(home, "Library/Application Support/KomplexniValidator").getAbsoluteFile(), loc.userDataDir);
        assertEquals(new File(loc.userDataDir, "logs"), loc.defaultLogDir);
    }

    @Test
    public void jarWithValidatorConfigBesideJar() throws IOException {
        File jarDir = appJarDir("KomplexniValidatorGUI-v2.9");
        for (OperatingSystem os : OperatingSystem.values()) {
            Locations loc = Locations.resolve(os, false, jarDir, home, "C:\\appdata", null, cwd);
            assertFalse(loc.jpackage);
            assertEquals(new File(jarDir, "validatorConfig").getAbsoluteFile(), loc.defaultValidatorConfigDir);
            assertNull("config.properties v pracovním adresáři", loc.userDataDir);
            assertEquals(new File("logs"), loc.defaultLogDir);
        }
    }

    @Test
    public void jarWithoutValidatorConfigBesideJar() throws IOException {
        File jarDir = tmp.newFolder("ide-classes");
        Locations loc = Locations.resolve(OperatingSystem.WINDOWS, false, jarDir, home, null, null, cwd);
        assertEquals(new File("validatorConfig"), loc.defaultValidatorConfigDir);
        assertNull(loc.userDataDir);
    }

    @Test
    public void unknownOsOrUnknownJarDirBehavesLikeJar() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/app");
        Locations loc = Locations.resolve(null, true, jarDir, home, null, null, cwd);
        assertFalse(loc.jpackage);
        assertNull(loc.userDataDir);
        assertEquals(new File(jarDir, "validatorConfig").getAbsoluteFile(), loc.defaultValidatorConfigDir);

        loc = Locations.resolve(OperatingSystem.LINUX, true, null, home, null, null, cwd);
        assertFalse(loc.jpackage);
        assertEquals(new File("validatorConfig"), loc.defaultValidatorConfigDir);
    }

    @Test
    public void staleValidatorConfigDir() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/app");
        Locations loc = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, null, null, cwd);

        assertTrue("neexistuje", loc.isStaleValidatorConfigDir(new File(tmp.getRoot(), "missing")));
        assertTrue("výchozí v instalaci", loc.isStaleValidatorConfigDir(loc.defaultValidatorConfigDir));
        File insideInstall = new File(tmp.getRoot(), "KomplexniValidator/my-config");
        assertTrue(insideInstall.mkdirs());
        assertTrue("uvnitř instalace", loc.isStaleValidatorConfigDir(insideInstall));
        File otherInstall = new File(tmp.getRoot(), "Old/KomplexniValidator.app/Contents/app/validatorConfig");
        assertTrue(otherInstall.mkdirs());
        assertTrue("jiná instalace", loc.isStaleValidatorConfigDir(otherInstall));
        File custom = tmp.newFolder("my-validatorConfig");
        assertFalse("vlastní adresář mimo instalaci", loc.isStaleValidatorConfigDir(custom));
        File customWithAppSegment = new File(tmp.getRoot(), "srv/app/validatorConfig-custom");
        assertTrue(customWithAppSegment.mkdirs());
        assertFalse("vlastní adresář s 'app' v cestě není instalace", loc.isStaleValidatorConfigDir(customWithAppSegment));

        Locations jar = Locations.resolve(OperatingSystem.LINUX, false, tmp.newFolder("zip"), home, null, null, cwd);
        assertFalse(jar.isStaleValidatorConfigDir(custom));
        assertTrue(jar.isStaleValidatorConfigDir(new File(tmp.getRoot(), "missing")));
        assertFalse("při spuštění jarem se existující adresář nepřepisuje", jar.isStaleValidatorConfigDir(otherInstall));
    }

    @Test
    public void legacyConfigFileIsMigratedOnce() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/app");
        File legacy = new File(jarDir.getParentFile(), "config.properties");
        java.nio.file.Files.write(legacy.toPath(), "jhove.dir=/opt/jhove\n".getBytes("UTF-8"));
        Locations loc = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, null, null, cwd);
        assertEquals(legacy, loc.findLegacyConfigFile("config.properties"));

        ConfigurationManager manager = new ConfigurationManager(new Platform(OperatingSystem.WINDOWS, null, "Windows 11", "amd64"), loc);
        File migrated = new File(loc.userDataDir, "config.properties");
        assertTrue("config.properties zkopírován do adresáře uživatele", migrated.isFile());
        assertEquals(new File("/opt/jhove"), manager.getFileOrNull(ConfigurationManager.PROP_JHOVE_DIR));
        assertEquals("výchozí cesta ke konfiguraci v instalaci", loc.defaultValidatorConfigDir.getCanonicalFile(),
                manager.getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR));
    }

    @Test
    public void detectBySystemProperty() {
        System.setProperty("user.home", home.getAbsolutePath());
        Platform mac = new Platform(OperatingSystem.MAC, null, "Mac OS X", "aarch64");

        System.setProperty(PROP_APP_PATH, new File(tmp.getRoot(), "KomplexniValidator.app/Contents/MacOS/KomplexniValidator").getPath());
        Locations loc = Locations.detect(mac);
        assertTrue(loc.jpackage);
        assertNotNull(loc.appJarDir);
        assertEquals(new File(loc.appJarDir, "validatorConfig").getAbsoluteFile(), loc.defaultValidatorConfigDir);
        assertEquals(new File(home, "Library/Application Support/KomplexniValidator").getAbsoluteFile(), loc.userDataDir);

        System.clearProperty(PROP_APP_PATH);
        loc = Locations.detect(mac);
        assertFalse(loc.jpackage);
        assertNull(loc.userDataDir);

        // neznámý OS: bez výjimky
        loc = Locations.detect(new Platform(null, null, "Plan9", "mips"));
        assertFalse(loc.jpackage);
    }

    @Test
    public void managerReplacesStaleStoredPathAndKeepsCustom() throws IOException {
        File jarDir = appJarDir("KomplexniValidator/app");
        Locations loc = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, null, null, cwd);
        Platform win = new Platform(OperatingSystem.WINDOWS, null, "Windows 11", "amd64");

        // bez config.properties: výchozí cesta, soubor vznikne mimo instalaci
        ConfigurationManager manager = new ConfigurationManager(win, loc);
        File configFile = new File(loc.userDataDir, "config.properties");
        assertTrue(configFile.isFile());
        assertEquals(loc.defaultValidatorConfigDir.getCanonicalFile(), manager.getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR));
        assertEquals(loc.defaultLogDir.getCanonicalFile(), manager.getFileOrNull(ConfigurationManager.PROP_LOG_DIR));
        assertTrue(loc.defaultLogDir.isDirectory());

        // uložená cesta neexistuje: nahradí se výchozí
        writeValidatorConfigDir(configFile, new File(tmp.getRoot(), "missing"));
        manager = new ConfigurationManager(win, loc);
        assertEquals(loc.defaultValidatorConfigDir.getCanonicalFile(), manager.getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR));

        // vlastní existující adresář mimo instalaci zůstává
        File custom = tmp.newFolder("custom");
        writeValidatorConfigDir(configFile, custom);
        manager = new ConfigurationManager(win, loc);
        assertEquals(custom, manager.getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR));
    }

    private static void writeValidatorConfigDir(File configFile, File dir) throws IOException {
        Properties props = new Properties();
        props.setProperty(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR, dir.getAbsolutePath());
        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, null);
        }
    }
}
