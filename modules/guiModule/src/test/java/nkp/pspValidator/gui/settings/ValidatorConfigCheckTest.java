package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.ConfigManifest;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Version;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Ověření adresáře konfigurace validátoru vybraného v Nastavení. Konfigurace v dočasném adresáři je sestavená z částí
 * konfigurace v repozitáři (dvě verze fDMF), aby šlo jednotlivé části odebrat nebo poškodit.
 */
public class ValidatorConfigCheckTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File config;

    @Before
    public void setUp() throws IOException {
        config = new File(tmp.getRoot(), "Konfigurace validátoru kopie");
        copy(new File(GuiTestSupport.VALIDATOR_CONFIG_DIR, "dictionaries"), new File(config, "dictionaries"));
        copy(new File(GuiTestSupport.VALIDATOR_CONFIG_DIR, "xsd"), new File(config, "xsd"));
        copy(new File(GuiTestSupport.VALIDATOR_CONFIG_DIR, "externalUtils.xml"), new File(config, "externalUtils.xml"));
        copy(new File(GuiTestSupport.VALIDATOR_CONFIG_DIR, ConfigManifest.FILE_NAME), new File(config, ConfigManifest.FILE_NAME));
        copy(new File(GuiTestSupport.VALIDATOR_CONFIG_DIR, "fDMF/monograph_2.3"), new File(config, "fDMF/monograph_2.3"));
        copy(new File(GuiTestSupport.VALIDATOR_CONFIG_DIR, "fDMF/periodical_2.2"), new File(config, "fDMF/periodical_2.2"));
    }

    private static ValidatorConfigCheck.Result check(File dir) {
        return ValidatorConfigCheck.check(dir, OperatingSystem.MAC);
    }

    private static void assertError(String expected, ValidatorConfigCheck.Result result) {
        assertFalse(result.isOk());
        assertEquals(expected, result.getError());
        assertEquals(0, result.getFdmfCount());
    }

    @Test
    public void validCopy() {
        ValidatorConfigCheck.Result result = check(config);
        assertTrue(result.getError(), result.isOk());
        assertNull(result.getError());
        assertEquals(2, result.getFdmfCount());
    }

    @Test
    public void validForEveryOs() {
        for (OperatingSystem os : OperatingSystem.values()) {
            assertTrue(os.name(), ValidatorConfigCheck.check(config, os).isOk());
        }
    }

    /** konfigurace z repozitáře (tatáž jako v instalaci) projde */
    @Test
    public void repositoryConfig() {
        ValidatorConfigCheck.Result result = check(GuiTestSupport.VALIDATOR_CONFIG_DIR);
        assertTrue(result.getError(), result.isOk());
        assertTrue(result.getFdmfCount() > 2);
    }

    /** konfigurace 2.x (bez manifestu, jinak úplná): hlásí se verze, ne obecnější chyba */
    @Test
    public void configWithoutManifest() throws IOException {
        Assume.assumeFalse("záložní verze aplikace, kontrola manifestu je vypnutá", "0.0.0-dev".equals(Version.VERSION_CODE));
        delete(new File(config, ConfigManifest.FILE_NAME));
        ValidatorConfigCheck.Result result = check(config);
        assertFalse(result.isOk());
        assertTrue(result.getError(), result.getError().startsWith("Adresář konfigurace " + config.getAbsolutePath()
                + " neobsahuje soubor manifest.properties. "));
        assertEquals(0, result.getFdmfCount());
    }

    @Test
    public void configOfOtherMajorVersion() throws IOException {
        Assume.assumeFalse("záložní verze aplikace, kontrola manifestu je vypnutá", "0.0.0-dev".equals(Version.VERSION_CODE));
        Files.write(new File(config, ConfigManifest.FILE_NAME).toPath(), "configVersion=2.9\n".getBytes(StandardCharsets.UTF_8));
        String major = Version.VERSION_CODE.split("[.-]")[0];
        assertError("Adresář konfigurace " + config.getAbsolutePath() + " obsahuje konfiguraci verze 2.9. Validátor verze "
                + Version.VERSION_CODE + " potřebuje konfiguraci verze " + major + ".x.", check(config));
    }

    /** konfigurace 2.x s chybějící částí: vyhraje hláška o verzi, ne „není úplná“ */
    @Test
    public void manifestErrorWinsOverIncompleteConfig() throws IOException {
        Assume.assumeFalse("záložní verze aplikace, kontrola manifestu je vypnutá", "0.0.0-dev".equals(Version.VERSION_CODE));
        delete(new File(config, ConfigManifest.FILE_NAME));
        delete(new File(config, "dictionaries"));
        ValidatorConfigCheck.Result result = check(config);
        assertFalse(result.isOk());
        assertTrue(result.getError(), result.getError().contains("neobsahuje soubor manifest.properties"));
    }

    @Test
    public void missingFdmf() throws IOException {
        delete(new File(config, "fDMF"));
        assertError("Konfigurace není úplná, chybí: fDMF.", check(config));
    }

    @Test
    public void emptyFdmf() throws IOException {
        delete(new File(config, "fDMF/monograph_2.3"));
        delete(new File(config, "fDMF/periodical_2.2"));
        assertError("V podadresáři fDMF nejsou žádná pravidla validace (adresáře jako monograph_2.4).", check(config));
    }

    /** jeden zbylý adresář fDMF stačí */
    @Test
    public void singleFdmf() throws IOException {
        delete(new File(config, "fDMF/periodical_2.2"));
        ValidatorConfigCheck.Result result = check(config);
        assertTrue(result.isOk());
        assertEquals(1, result.getFdmfCount());
    }

    @Test
    public void missingDictionaries() throws IOException {
        delete(new File(config, "dictionaries"));
        assertError("Konfigurace není úplná, chybí: dictionaries.", check(config));
    }

    @Test
    public void missingFilesAreListedInOrder() throws IOException {
        delete(new File(config, "externalUtils.xml"));
        delete(new File(config, "xsd/metadataProfile.xsd"));
        assertError("Konfigurace není úplná, chybí: externalUtils.xml, xsd/metadataProfile.xsd.", check(config));
    }

    @Test
    public void eachXsdFileIsRequired() throws IOException {
        for (String name : new String[]{"fdmfConfig.xsd", "binaryFileProfile.xsd", "metadataProfile.xsd"}) {
            File xsd = new File(config, "xsd/" + name);
            File backup = new File(tmp.getRoot(), name);
            Files.move(xsd.toPath(), backup.toPath());
            assertError("Konfigurace není úplná, chybí: xsd/" + name + ".", check(config));
            Files.move(backup.toPath(), xsd.toPath());
        }
        assertTrue(check(config).isOk());
    }

    /** chybí celý adresář xsd: hlásí se jen on, ne jednotlivé soubory v něm */
    @Test
    public void missingXsdDir() throws IOException {
        delete(new File(config, "xsd"));
        assertError("Konfigurace není úplná, chybí: xsd.", check(config));
    }

    /** soubor místo adresáře fDMF se hlásí jako chybějící adresář */
    @Test
    public void fdmfIsFile() throws IOException {
        delete(new File(config, "fDMF"));
        Files.write(new File(config, "fDMF").toPath(), new byte[0]);
        assertError("Konfigurace není úplná, chybí: fDMF.", check(config));
    }

    @Test
    public void notExisting() {
        assertError("Adresář neexistuje.", check(new File(tmp.getRoot(), "neexistuje")));
        assertError("Adresář neexistuje.", check(null));
    }

    @Test
    public void notDirectory() throws IOException {
        File file = tmp.newFile("validatorConfig.zip");
        assertError("Vybraná cesta není adresář.", check(file));
    }

    @Test
    public void emptyDirectory() throws IOException {
        assertError("Adresář je prázdný.", check(tmp.newFolder("prázdný")));
    }

    @Test
    public void unrelatedDirectory() throws IOException {
        File dir = tmp.newFolder("Dokumenty");
        Files.write(new File(dir, "poznámky.txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
        assertError("Adresář neobsahuje konfiguraci validátoru (podadresáře fDMF, dictionaries, xsd a soubor externalUtils.xml).", check(dir));
    }

    /** vybraný nadřazený adresář (např. adresář rozbaleného zipu), konfigurace je o úroveň níž */
    @Test
    public void parentOfValidatorConfig() throws IOException {
        File parent = tmp.newFolder("rozbaleno");
        Files.move(config.toPath(), new File(parent, "validatorConfig").toPath());
        assertError("Konfigurace je v podadresáři validatorConfig, vyberte přímo ten.", check(parent));
    }

    /** neúplný vnořený validatorConfig se jako nápověda nenabízí */
    @Test
    public void parentOfIncompleteValidatorConfig() throws IOException {
        File parent = tmp.newFolder("rozbaleno");
        Files.move(config.toPath(), new File(parent, "validatorConfig").toPath());
        delete(new File(parent, "validatorConfig/fDMF"));
        assertError("Adresář neobsahuje konfiguraci validátoru (podadresáře fDMF, dictionaries, xsd a soubor externalUtils.xml).", check(parent));
    }

    /** jen některé části: neúplná konfigurace, ne „neobsahuje“ */
    @Test
    public void onlyFdmfPresent() throws IOException {
        delete(new File(config, "dictionaries"));
        delete(new File(config, "xsd"));
        delete(new File(config, "externalUtils.xml"));
        assertError("Konfigurace není úplná, chybí: dictionaries, xsd, externalUtils.xml.", check(config));
    }

    @Test
    public void externalUtilsWithWrongRoot() throws IOException {
        Files.write(new File(config, "externalUtils.xml").toPath(), "<dmf/>".getBytes(StandardCharsets.UTF_8));
        assertError("Konfiguraci nelze načíst: root element není dmf", check(config));
    }

    @Test
    public void malformedExternalUtils() throws IOException {
        Files.write(new File(config, "externalUtils.xml").toPath(), "<externalUtils>".getBytes(StandardCharsets.UTF_8));
        ValidatorConfigCheck.Result result = check(config);
        assertFalse(result.isOk());
        assertTrue(result.getError(), result.getError().startsWith("Konfiguraci nelze načíst: "));
        assertTrue(result.getError(), result.getError().length() > "Konfiguraci nelze načíst: ".length());
    }

    private static void copy(File from, File to) throws IOException {
        Path source = from.toPath();
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path target = to.toPath().resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        Files.delete(file.toPath());
    }
}
