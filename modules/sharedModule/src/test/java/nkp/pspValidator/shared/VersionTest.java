package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ResourceType;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileValidator;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Verze aplikace z version.properties a kontrola atributu validatorVersion v konfiguraci (porovnava se jen major).
 */
public class VersionTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final File CONFIG = new File("rules.xml");

    @Test
    public void versionPropertiesGeneratedByGradle() {
        //testy bezi pres Gradle, version.properties je na classpath; fallback by znamenal rozbity build
        assertNotEquals(Version.FALLBACK_VERSION_CODE, Version.VERSION_CODE);
        assertNotEquals(Version.FALLBACK_BUILD_DATE, Version.BUILD_DATE);
        assertTrue(Version.BUILD_DATE, Version.BUILD_DATE.matches("\\d{1,2}\\. \\d{1,2}\\. \\d{4}"));
        assertNotNull(Version.BUILD_DATE_ISO);
        assertTrue(Version.BUILD_DATE_ISO, Version.BUILD_DATE_ISO.matches("\\d{4}-\\d{2}-\\d{2}"));
        assertNotNull("verze aplikace musi zacinat cislem: " + Version.VERSION_CODE, Version.majorVersion(Version.VERSION_CODE));
    }

    @Test
    public void majorVersion() {
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-alpha"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-alpha2"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-beta"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-beta2"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-rc1"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.1-dev"));
        // dřívější tvar s tečkou v příponě (3.0-alpha.0) konvenci neodpovídá, major se z něj ale přečte
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-alpha.0"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0.1"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.1"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3"));
        assertEquals(Integer.valueOf(12), Version.majorVersion("12.4"));
        assertNull(Version.majorVersion(null));
        assertNull(Version.majorVersion(""));
        assertNull(Version.majorVersion("x.y"));
        assertNull(Version.majorVersion("3.0beta"));
    }

    @Test
    public void sameMajorPasses() throws ValidatorConfigurationException {
        for (String app : new String[]{"3.0-alpha", "3.0-alpha2", "3.0-beta", "3.0-rc1", "3.0", "3.0.1", "3.1", "3.2.5-dev"}) {
            Version.checkConfigValidatorVersion(app, "3.0", CONFIG);
            Version.checkConfigValidatorVersion(app, "3.1", CONFIG);
            Version.checkConfigValidatorVersion(app, "3", CONFIG);
        }
    }

    @Test
    public void differentMajorFails() {
        assertFails("3.0-alpha", "2.9");
        assertFails("3.0", "2.0");
        assertFails("3.0.1", "4.0");
        assertFails("2.9", "3.0");
    }

    @Test
    public void invalidOrMissingConfigVersionFails() {
        assertFails("3.0", null);
        assertFails("3.0", "");
        assertFails("3.0", "tri");
    }

    @Test
    public void messageSaysWhatIsExpected() {
        String message = assertFails("3.0-alpha", "2.9");
        assertTrue(message, message.contains("validatorVersion='2.9'"));
        assertTrue(message, message.contains("3.0-alpha"));
        assertTrue(message, message.contains("major verzí 3"));
        assertTrue(message, message.contains(CONFIG.getAbsolutePath()));
    }

    @Test
    public void unknownAppVersionSkipsCheck() throws ValidatorConfigurationException {
        //beh z IDE bez version.properties: kontrola vypnuta
        Version.checkConfigValidatorVersion(Version.FALLBACK_VERSION_CODE, "2.9", CONFIG);
        Version.checkConfigValidatorVersion(Version.FALLBACK_VERSION_CODE, null, CONFIG);
    }

    @Test
    public void metadataProfileParserUsesMajorCheck() throws Exception {
        DictionaryManager dictionaryManager = new DictionaryManager(tmp.newFolder("dictionaries"));
        MetadataProfileParser parser = new MetadataProfileParser(dictionaryManager);
        String major = Version.majorVersion(Version.VERSION_CODE).toString();

        //stejny major, jina minor: projde
        parser.parseProfile(profile(major + ".9"));
        //jiny major: neprojde
        try {
            parser.parseProfile(profile((Integer.parseInt(major) - 1) + ".9"));
            fail("profil s jinou major verzi mel byt odmitnut");
        } catch (ValidatorConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nesouhlasí verze validátoru"));
        }
    }

    @Test
    public void binaryFileProfileUsesMajorCheck() throws Exception {
        String major = Version.majorVersion(Version.VERSION_CODE).toString();

        //stejny major, jina minor: projde
        registerBinaryFileProfile(major + ".9");
        //jiny major: neprojde
        try {
            registerBinaryFileProfile((Integer.parseInt(major) - 1) + ".9");
            fail("profil binarniho souboru s jinou major verzi mel byt odmitnut");
        } catch (ValidatorConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nesouhlasí verze validátoru"));
        }
    }

    @Test
    public void fdmfConfigurationUsesMajorCheck() throws Exception {
        String major = Version.majorVersion(Version.VERSION_CODE).toString();
        File configDir = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
        File xsdDir = new File(configDir, "xsd");
        File fdmfConfigXsd = new File(xsdDir, "fdmfConfig.xsd");
        File binaryFileProfileXsd = new File(xsdDir, "binaryFileProfile.xsd");
        File metadataProfileXsd = new File(xsdDir, "metadataProfile.xsd");

        //kopie fDMF beze zmeny: projde
        File fdmfRoot = copyDir(new File(configDir, "fDMF/data_disc_0.1"), tmp.newFolder("data_disc_0.1"));
        new FdmfConfiguration(fdmfRoot, fdmfConfigXsd, binaryFileProfileXsd, metadataProfileXsd);

        //jiny major v jedinem konfiguracnim souboru: cela fDMF neprojde
        File rules = new File(fdmfRoot, "rules.xml");
        String original = new String(Files.readAllBytes(rules.toPath()), StandardCharsets.UTF_8);
        String otherMajor = "validatorVersion=\"" + (Integer.parseInt(major) - 1) + ".9\"";
        String changed = original.replaceFirst("validatorVersion=\"[^\"]*\"", otherMajor);
        assertTrue("rules.xml nema atribut validatorVersion", changed.contains(otherMajor));
        Files.write(rules.toPath(), changed.getBytes(StandardCharsets.UTF_8));
        try {
            new FdmfConfiguration(fdmfRoot, fdmfConfigXsd, binaryFileProfileXsd, metadataProfileXsd);
            fail("fDMF s jinou major verzi v rules.xml mela byt odmitnuta");
        } catch (ValidatorConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nesouhlasí verze validátoru"));
            assertTrue(e.getMessage(), e.getMessage().contains(rules.getAbsolutePath()));
        }
    }

    private void registerBinaryFileProfile(String validatorVersion) throws Exception {
        File file = tmp.newFile();
        String xml = "<binaryFileProfile xmlns=\"http://www.nkp.cz/pspValidator/3.0/binaryFileProfile\" validatorVersion=\""
                + validatorVersion + "\" dmf=\"test\"><fromText/></binaryFileProfile>";
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.KAKADU, file);
    }

    private static File copyDir(File source, File target) throws Exception {
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
        return target;
    }

    private File profile(String validatorVersion) throws Exception {
        File file = tmp.newFile();
        String xml = "<metadataProfile xmlns=\"http://www.nkp.cz/pspValidator/3.0/metadataProfile\" validatorVersion=\""
                + validatorVersion + "\" dmf=\"test\"><info/><rootElement name=\"mods\"/></metadataProfile>";
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String assertFails(String appVersion, String configVersion) {
        try {
            Version.checkConfigValidatorVersion(appVersion, configVersion, CONFIG);
            fail(String.format("aplikace %s nemela nacist konfiguraci s validatorVersion '%s'", appVersion, configVersion));
            return null;
        } catch (ValidatorConfigurationException e) {
            return e.getMessage();
        }
    }
}
