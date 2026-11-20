package nkp.pspValidator.gui;

import nkp.pspValidator.gui.ConfigurationManager.Locations;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Adresa aktualizace slovníku kódů institucí (Sigla) v config.properties: dřívější výchozí adresa (stará cesta
 * k modulu, 404) se při startu nahradí novou, adresa změněná uživatelem zůstane, chybějící se doplní.
 */
public class ConfigurationManagerDictionariesTest {

    private static final String KEY = "dictionary.siglaInstitutionCodes.syncUrl";
    private static final String OLD_DEFAULT = "https://raw.githubusercontent.com/NLCR/komplexni-validator/master/"
            + "sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/dictionaries/siglaInstitutionCodes.dict";
    private static final String NEW_DEFAULT = "https://raw.githubusercontent.com/NLCR/komplexni-validator/master/"
            + "modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/dictionaries/siglaInstitutionCodes.dict";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Locations locations;
    private Platform platform;
    private File configFile;

    @Before
    public void setUp() throws IOException {
        File jarDir = new File(tmp.getRoot(), "KomplexniValidator/app");
        assertTrue(new File(jarDir, "validatorConfig").mkdirs());
        File home = tmp.newFolder("home");
        locations = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, null, null, tmp.newFolder("cwd"));
        platform = new Platform(OperatingSystem.WINDOWS, null, "Windows 11", "amd64");
        configFile = new File(locations.userDataDir, "config.properties");
        assertTrue(locations.userDataDir.mkdirs());
    }

    @Test
    public void constantsAreTheExpectedAddresses() {
        assertEquals(NEW_DEFAULT, ConfigurationManager.SIGLA_SYNC_URL);
        assertEquals(OLD_DEFAULT, ConfigurationManager.SIGLA_SYNC_URL_BEFORE_3_0);
    }

    @Test
    public void oldDefaultIsReplacedAndSaved() throws IOException {
        write(KEY, OLD_DEFAULT);
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertEquals(NEW_DEFAULT, manager.getStringOrDefault(KEY, null));
        assertEquals("nová adresa je uložená v souboru", NEW_DEFAULT, read(KEY));
    }

    @Test
    public void customValueIsKept() throws IOException {
        String custom = "https://example.org/moje/siglaInstitutionCodes.dict";
        write(KEY, custom);
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertEquals(custom, manager.getStringOrDefault(KEY, null));
        assertEquals(custom, read(KEY));
    }

    @Test
    public void oldDefaultWithDifferenceIsKept() throws IOException {
        //jen přesná shoda se starou výchozí adresou se nahrazuje (tady navíc lomítko na konci)
        write(KEY, OLD_DEFAULT + "/");
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertEquals(OLD_DEFAULT + "/", manager.getStringOrDefault(KEY, null));
    }

    @Test
    public void emptyValueGetsNewDefault() throws IOException {
        write(KEY, "");
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertEquals(NEW_DEFAULT, manager.getStringOrDefault(KEY, null));
        assertEquals(NEW_DEFAULT, read(KEY));
    }

    @Test
    public void missingValueGetsNewDefault() throws IOException {
        assertFalse(configFile.exists());
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertEquals(NEW_DEFAULT, manager.getStringOrDefault(KEY, null));
        assertEquals(NEW_DEFAULT, read(KEY));
    }

    @Test
    public void otherDictionariesAreNotAffected() throws IOException {
        write(KEY, OLD_DEFAULT);
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertNull("slovník bez aktualizace nemá adresu", manager.getStringOrDefault("dictionary.marcRelatorCodes.syncUrl", null));
        assertEquals("https://www.loc.gov/marc/relators/relaterm.html",
                manager.getStringOrDefault("dictionary.marcRelatorCodes.specUrl", null));
    }

    private void write(String key, String value) throws IOException {
        Properties props = new Properties();
        props.setProperty(key, value);
        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, null);
        }
    }

    private String read(String key) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        }
        return props.getProperty(key);
    }

    /** Kódy institucí mají místo specifikace odkaz na seznam na GitHubu; doplní se i do nastavení, kde dosud chyběl. */
    @Test
    public void siglaHasLinkToListOnGithub() throws IOException {
        assertEquals("https://github.com/NLCR/komplexni-validator/blob/master/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/dictionaries/siglaInstitutionCodes.dict",
                ConfigurationManager.SIGLA_LIST_URL);
        //nastaveni z drivejsi verze odkaz nema: doplni se
        write(KEY, NEW_DEFAULT);
        ConfigurationManager manager = new ConfigurationManager(platform, locations);
        assertEquals(ConfigurationManager.SIGLA_LIST_URL,
                manager.getStringOrDefault("dictionary.siglaInstitutionCodes.specUrl", null));
        //ostatni slovniky maji dal svou specifikaci
        assertEquals("https://www.iso.org/iso-3166-country-codes.html",
                manager.getStringOrDefault("dictionary.iso31661Alpha2languageCodes.specUrl", null));
    }
}
