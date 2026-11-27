package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static nkp.pspValidator.shared.ConfigManifest.Status.*;
import static org.junit.Assert.*;

/**
 * Manifest konfigurace (manifest.properties, klíč configVersion). Očekávané hodnoty a hlášky jsou psané ručně podle
 * zadání: stejný major a minor je OK, jiný minor jen varování (OLDER_MINOR, NEWER_MINOR), jiný major, chybějící
 * a neplatný manifest je chyba. Verze aplikace se v testech předává výslovně, kromě testů konstruktoru
 * {@link ValidatorConfigurationManager}.
 */
public class ConfigManifestTest {

    private static final File CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** adresář konfigurace jen s manifestem, jehož obsah je zadaný; null = bez souboru manifestu */
    private File dirWithManifest(String content) throws IOException {
        File dir = tmp.newFolder();
        if (content != null) {
            Files.write(new File(dir, ConfigManifest.FILE_NAME).toPath(), content.getBytes(StandardCharsets.UTF_8));
        }
        return dir;
    }

    private File dirWithVersion(String configVersion) throws IOException {
        return dirWithManifest("configVersion=" + configVersion + "\n");
    }

    private ConfigManifest.Result evaluate(String appVersion, String configVersion) throws IOException {
        return ConfigManifest.evaluate(appVersion, dirWithVersion(configVersion));
    }

    private void assertStatus(ConfigManifest.Status expected, String appVersion, String configVersion) throws IOException {
        ConfigManifest.Result r = evaluate(appVersion, configVersion);
        assertEquals("aplikace " + appVersion + ", konfigurace " + configVersion, expected, r.getStatus());
    }

    // ---- shoda

    @Test
    public void sameMajorAndMinorIsOkForEveryAppVersionForm() throws IOException {
        for (String app : new String[]{"3.0", "3.0-beta", "3.0-beta2", "3.0-rc1", "3.0.1", "3.0.1-dev"}) {
            ConfigManifest.Result r = evaluate(app, "3.0");
            assertEquals(app, OK, r.getStatus());
            assertEquals(app, "3.0", r.getConfigVersion());
            assertNull(app, r.getMessage());
        }
    }

    @Test
    public void okForNonZeroMinor() throws IOException {
        assertStatus(OK, "3.1.2", "3.1");
        assertStatus(OK, "3.10", "3.10");
    }

    // ---- minor

    @Test
    public void olderMinorIsWarningNotError() throws Exception {
        for (String app : new String[]{"3.1", "3.1-beta2"}) {
            File dir = dirWithVersion("3.0");
            ConfigManifest.Result r = ConfigManifest.evaluate(app, dir);
            assertEquals(app, OLDER_MINOR, r.getStatus());
            assertEquals("3.0", r.getConfigVersion());
            assertEquals("Adresář konfigurace " + dir.getAbsolutePath() + " obsahuje konfiguraci verze 3.0, validátor má verzi " + app
                    + ". Typy dokumentů a verze DMF přidané v novějších verzích validátoru v ní chybějí.", r.getMessage());
            //neni chyba: check vrati vysledek a nevyhodi vyjimku
            assertSame(OLDER_MINOR, ConfigManifest.check(app, dir).getStatus());
        }
    }

    @Test
    public void newerMinorIsWarningNotError() throws Exception {
        File dir = dirWithVersion("3.2");
        ConfigManifest.Result r = ConfigManifest.evaluate("3.1", dir);
        assertEquals(NEWER_MINOR, r.getStatus());
        assertEquals("3.2", r.getConfigVersion());
        assertEquals("Adresář konfigurace " + dir.getAbsolutePath() + " obsahuje konfiguraci verze 3.2, která je novější než validátor verze 3.1. "
                + "Některé její části validátor nemusí umět načíst.", r.getMessage());
        assertSame(NEWER_MINOR, ConfigManifest.check("3.1", dir).getStatus());
    }

    /** kraje: minor 0 proti 1 a víceciferný minor (10 je víc než 9, ne řetězcové porovnání) */
    @Test
    public void minorBoundaries() throws IOException {
        assertStatus(OLDER_MINOR, "3.1", "3.0");
        assertStatus(NEWER_MINOR, "3.0", "3.1");
        assertStatus(NEWER_MINOR, "3.9", "3.10");
        assertStatus(OLDER_MINOR, "3.10", "3.9");
        assertStatus(OK, "3.9", "3.9");
    }

    // ---- major

    @Test
    public void differentMajorBothDirections() throws Exception {
        File older = dirWithVersion("2.9");
        ConfigManifest.Result r = ConfigManifest.evaluate("3.0", older);
        assertEquals(DIFFERENT_MAJOR, r.getStatus());
        assertEquals("2.9", r.getConfigVersion());
        assertEquals("Adresář konfigurace " + older.getAbsolutePath() + " obsahuje konfiguraci verze 2.9. "
                + "Validátor verze 3.0 potřebuje konfiguraci verze 3.x.", r.getMessage());

        File newer = dirWithVersion("4.0");
        r = ConfigManifest.evaluate("3.0-beta", newer);
        assertEquals(DIFFERENT_MAJOR, r.getStatus());
        assertEquals("Adresář konfigurace " + newer.getAbsolutePath() + " obsahuje konfiguraci verze 4.0. "
                + "Validátor verze 3.0-beta potřebuje konfiguraci verze 3.x.", r.getMessage());
    }

    /** víceciferný major: 10 není 1 */
    @Test
    public void multiDigitMajor() throws IOException {
        assertStatus(DIFFERENT_MAJOR, "10.0", "1.0");
        assertStatus(DIFFERENT_MAJOR, "1.0", "10.0");
        assertStatus(OK, "10.0", "10.0");
    }

    @Test
    public void differentMajorIsErrorWithExceptionMessage() throws Exception {
        File dir = dirWithVersion("2.9");
        try {
            ConfigManifest.check("3.0", dir);
            fail("jiný major měl vyhodit výjimku");
        } catch (ValidatorConfigurationException e) {
            assertEquals("Adresář konfigurace " + dir.getAbsolutePath() + " obsahuje konfiguraci verze 2.9. "
                    + "Validátor verze 3.0 potřebuje konfiguraci verze 3.x.", e.getMessage());
        }
    }

    // ---- chybějící a neplatný manifest

    @Test
    public void missingFile() throws Exception {
        File dir = dirWithManifest(null);
        ConfigManifest.Result r = ConfigManifest.evaluate("3.0-beta", dir);
        assertEquals(MISSING, r.getStatus());
        assertNull(r.getConfigVersion());
        String expected = "Adresář konfigurace " + dir.getAbsolutePath() + " neobsahuje soubor manifest.properties. "
                + "Jde o konfiguraci pro verzi validátoru starší než 3.0, nebo to není adresář konfigurace validátoru. "
                + "Validátor verze 3.0-beta potřebuje konfiguraci verze 3.x.";
        assertEquals(expected, r.getMessage());
        try {
            ConfigManifest.check("3.0-beta", dir);
            fail("chybějící manifest měl vyhodit výjimku");
        } catch (ValidatorConfigurationException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void invalidManifestIsReportedWithManifestPath() throws Exception {
        String[] contents = {
                "",                              // prázdný soubor
                "# jen komentář\n",              // klíč chybí
                "jinyKlic=3.0\n",                // klíč chybí
                "configVersion=\n",              // prázdná hodnota
                "configVersion=   \n",           // jen mezery
                "configVersion=3\n",             // chybí minor
                "configVersion=3.0.1\n",         // PATCH
                "configVersion=3.0-beta\n",      // přípona
                "configVersion=tri\n",
                "configVersion=3.\n",
                "configVersion=.0\n",
                "configVersion=3.x\n",
                "configVersion=-3.0\n",
                "configVersion=3,0\n",
                "configVersion=3.0\\u0\n",       // neplatná escape sekvence
                "configVersion=99999999999.0\n", // mimo rozsah int
        };
        for (String content : contents) {
            File dir = dirWithManifest(content);
            ConfigManifest.Result r = ConfigManifest.evaluate("3.0", dir);
            assertEquals(content, INVALID, r.getStatus());
            assertNull(content, r.getConfigVersion());
            assertEquals(content, "Soubor " + new File(dir, "manifest.properties").getAbsolutePath()
                    + " nejde přečíst nebo neobsahuje platný údaj configVersion (očekává se tvar MAJOR.MINOR, například 3.0).", r.getMessage());
            try {
                ConfigManifest.check("3.0", dir);
                fail(content);
            } catch (ValidatorConfigurationException e) {
                assertEquals(content, r.getMessage(), e.getMessage());
            }
        }
    }

    /** manifest.properties je adresář místo souboru: nejde přečíst */
    @Test
    public void manifestIsDirectory() throws Exception {
        File dir = tmp.newFolder();
        assertTrue(new File(dir, "manifest.properties").mkdir());
        assertEquals(INVALID, ConfigManifest.evaluate("3.0", dir).getStatus());
    }

    /** okolní mezery a komentáře: hodnota se ořezává, ostatní klíče a komentáře nevadí */
    @Test
    public void surroundingWhitespaceIsTrimmed() throws IOException {
        for (String content : new String[]{"configVersion = 3.0\n", "configVersion=3.0   \n", "configVersion=   3.0\n",
                "configVersion=3.0\t\n", "# komentář\nconfigVersion=3.0\njinyKlic=x\n", "configVersion=3.0"}) {
            ConfigManifest.Result r = ConfigManifest.evaluate("3.0", dirWithManifest(content));
            assertEquals(content, OK, r.getStatus());
            assertEquals(content, "3.0", r.getConfigVersion());
        }
    }

    /** vnitřní mezera hodnotu znehodnotí */
    @Test
    public void innerWhitespaceIsInvalid() throws IOException {
        assertEquals(INVALID, ConfigManifest.evaluate("3.0", dirWithManifest("configVersion=3. 0\n")).getStatus());
    }

    /** manifest se čte jako UTF-8 (diakritika v komentáři nevadí) */
    @Test
    public void manifestIsReadAsUtf8() throws Exception {
        File dir = dirWithManifest("# Česká diakritika: žluťoučký kůň\nconfigVersion=3.0\n");
        assertEquals(OK, ConfigManifest.evaluate("3.0", dir).getStatus());
    }

    // ---- záložní verze aplikace

    @Test
    public void fallbackAppVersionSkipsTheCheck() throws Exception {
        String fallback = Version.FALLBACK_VERSION_CODE;
        assertEquals("0.0.0-dev", fallback);
        for (File dir : new File[]{dirWithManifest(null), dirWithVersion("2.9"), dirWithVersion("tri"), dirWithVersion("3.0")}) {
            ConfigManifest.Result r = ConfigManifest.evaluate(fallback, dir);
            assertEquals(OK, r.getStatus());
            assertNull(r.getMessage());
            assertSame(OK, ConfigManifest.check(fallback, dir).getStatus());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void invalidAppVersionIsProgrammingError() throws IOException {
        ConfigManifest.evaluate("tri", dirWithVersion("3.0"));
    }

    // ---- parseMajorMinor

    @Test
    public void parseMajorMinor() {
        assertArrayEquals(new int[]{3, 0}, ConfigManifest.parseMajorMinor("3.0-beta"));
        assertArrayEquals(new int[]{3, 1}, ConfigManifest.parseMajorMinor("3.1.2"));
        assertArrayEquals(new int[]{3, 0}, ConfigManifest.parseMajorMinor("3"));
        assertArrayEquals(new int[]{3, 10}, ConfigManifest.parseMajorMinor("3.10"));
        assertNull(ConfigManifest.parseMajorMinor("tri"));
        assertNull(ConfigManifest.parseMajorMinor(""));
        assertNull(ConfigManifest.parseMajorMinor(null));
        assertNull(ConfigManifest.parseMajorMinor("3."));
    }

    // ---- konfigurace v repozitáři a konstruktor ValidatorConfigurationManager

    @Test
    public void repositoryConfigHasManifestAndPassesWithAppVersion() throws Exception {
        assertTrue(new File(CONFIG_DIR, ConfigManifest.FILE_NAME).isFile());
        ConfigManifest.Result r = ConfigManifest.check(CONFIG_DIR);
        assertFalse(r.getStatus().isError());
        ValidatorConfigurationManager manager = new ValidatorConfigurationManager(CONFIG_DIR);
        assertSame(OK, manager.getManifestResult().getStatus());
    }

    @Test
    public void managerRejectsConfigWithoutManifest() throws Exception {
        Assume.assumeFalse("záložní verze aplikace, kontrola je vypnutá", Version.FALLBACK_VERSION_CODE.equals(Version.VERSION_CODE));
        File dir = copyConfigSkeleton();
        Files.delete(new File(dir, ConfigManifest.FILE_NAME).toPath());
        try {
            new ValidatorConfigurationManager(dir);
            fail("konfigurace bez manifestu měla vyhodit výjimku");
        } catch (ValidatorConfigurationException e) {
            int major = ConfigManifest.parseMajorMinor(Version.VERSION_CODE)[0];
            assertEquals("Adresář konfigurace " + dir.getAbsolutePath() + " neobsahuje soubor manifest.properties. "
                    + "Jde o konfiguraci pro verzi validátoru starší než 3.0, nebo to není adresář konfigurace validátoru. "
                    + "Validátor verze " + Version.VERSION_CODE + " potřebuje konfiguraci verze " + major + ".x.", e.getMessage());
        }
    }

    /** adresář s manifestem jiné major verze a bez ostatních částí: hlásí se verze, ne chybějící části */
    @Test
    public void managerRejectsOtherMajorBeforeOtherChecks() throws Exception {
        Assume.assumeFalse("záložní verze aplikace, kontrola je vypnutá", Version.FALLBACK_VERSION_CODE.equals(Version.VERSION_CODE));
        File dir = dirWithVersion("2.9");
        try {
            new ValidatorConfigurationManager(dir);
            fail("konfigurace 2.9 měla vyhodit výjimku");
        } catch (ValidatorConfigurationException e) {
            int major = ConfigManifest.parseMajorMinor(Version.VERSION_CODE)[0];
            assertEquals("Adresář konfigurace " + dir.getAbsolutePath() + " obsahuje konfiguraci verze 2.9. "
                    + "Validátor verze " + Version.VERSION_CODE + " potřebuje konfiguraci verze " + major + ".x.", e.getMessage());
        }
    }

    @Test
    public void managerAcceptsCopyWithManifestAndExposesResult() throws Exception {
        File dir = copyConfigSkeleton();
        ValidatorConfigurationManager manager = new ValidatorConfigurationManager(dir);
        assertNotNull(manager.getManifestResult());
        assertFalse(manager.getManifestResult().getStatus().isError());
    }

    /** jiný minor konstruktor nezastaví, jen ho zpřístupní přes getManifestResult */
    @Test
    public void managerAcceptsOtherMinorAndExposesStatus() throws Exception {
        Assume.assumeFalse("záložní verze aplikace, kontrola je vypnutá", Version.FALLBACK_VERSION_CODE.equals(Version.VERSION_CODE));
        int[] app = ConfigManifest.parseMajorMinor(Version.VERSION_CODE);
        File dir = copyConfigSkeleton();
        Files.write(new File(dir, ConfigManifest.FILE_NAME).toPath(),
                ("configVersion=" + app[0] + "." + (app[1] + 1) + "\n").getBytes(StandardCharsets.UTF_8));
        ValidatorConfigurationManager manager = new ValidatorConfigurationManager(dir);
        assertSame(NEWER_MINOR, manager.getManifestResult().getStatus());
    }

    /** kostra konfigurace v dočasném adresáři: jedna fDMF stačí pro konstruktor, který fDMF nenačítá */
    private File copyConfigSkeleton() throws IOException {
        File target = tmp.newFolder("kopie");
        copy(new File(CONFIG_DIR, "xsd"), new File(target, "xsd"));
        copy(new File(CONFIG_DIR, "dictionaries"), new File(target, "dictionaries"));
        copy(new File(CONFIG_DIR, "fDMF/monograph_2.3"), new File(target, "fDMF/monograph_2.3"));
        Files.copy(new File(CONFIG_DIR, "externalUtils.xml").toPath(), new File(target, "externalUtils.xml").toPath());
        Files.copy(new File(CONFIG_DIR, ConfigManifest.FILE_NAME).toPath(), new File(target, ConfigManifest.FILE_NAME).toPath());
        return target;
    }

    private static void copy(File source, File target) throws IOException {
        try (Stream<Path> paths = Files.walk(source.toPath())) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path copy = target.toPath().resolve(source.toPath().relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(copy);
                } else {
                    Files.copy(path, copy);
                }
            }
        }
    }
}
