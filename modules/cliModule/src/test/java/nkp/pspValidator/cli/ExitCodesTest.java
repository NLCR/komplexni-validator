package nkp.pspValidator.cli;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * Návratové kódy CLI. Očekávané hodnoty jsou ze zadání, ne z kódu:
 * 0 = validace doběhla a protokol vznikl (balík validní i nevalidní), nápověda a verze;
 * 1 = balík (u skupiny aspoň jeden) se nezvalidoval;
 * 2 = chyba použití (parametry, akce);
 * 3 = chyba konfigurace validátoru.
 * Chybové hlášky jdou na stderr, v chybových větvích nevznikne protokol.
 */
public class ExitCodesTest {

    private static final String RES = "../sharedModule/src/test/resources/";
    private static final String CONFIG = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
    private static final String CLIPPING_02 = RES + "fund_unit_0.2/valid_clipping/nk-00027x";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private PrintStream origOut;
    private PrintStream origErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @Before
    public void redirect() throws Exception {
        origOut = System.out;
        origErr = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, "UTF-8"));
        System.setErr(new PrintStream(err, true, "UTF-8"));
    }

    @After
    public void restore() {
        System.setOut(origOut);
        System.setErr(origErr);
    }

    // ---------------------------------------------------------------- kód 0

    @Test
    public void validPackageEndsWithZeroAndCreatesProtocol() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(0, run(validate(new File(CLIPPING_02), xml)));
        assertTrue(stdout(), stdout().contains("balík je: validní"));
        assertEquals("", stderr());
        assertTrue(xml.isFile());
    }

    @Test
    public void invalidPackageEndsWithZeroAndCreatesProtocol() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(0, run(validate(minimalEperiodical("2.6"), xml)));
        assertTrue(stdout(), stdout().contains("balík je: nevalidní"));
        assertTrue(xml.isFile());
    }

    @Test
    public void helpEndsWithZeroAndDescribesExitCodes() throws Exception {
        assertEquals(0, run("--" + Params.HELP));
        // nápověda odstraňuje diakritiku (replaceUmlaut)
        assertTrue(stdout(), stdout().contains("Navratove kody"));
        assertEquals("", stderr());
    }

    @Test
    public void versionEndsWithZero() throws Exception {
        assertEquals(0, run("--" + Params.VERSION));
        assertTrue(stdout(), stdout().contains("Komplexni validator CLI verze"));
    }

    @Test
    public void validGroupEndsWithZero() throws Exception {
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        File protocols = tmp.newFolder("protokoly");
        assertEquals(0, run(concat(base("VALIDATE_PSP_GROUP"), "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.XML_PROTOCOL_DIR, protocols.getPath())));
        assertEquals(1, protocols.list().length);
    }

    @Test
    public void ignoredFileInGroupDoesNotChangeZero() throws Exception {
        // soubor, který není adresář ani ZIP, se ve skupině přeskočí (hláška na stdout), ostatní balíky se zvalidují
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        Files.write(new File(group, "poznamka.txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
        File protocols = tmp.newFolder("protokoly");
        assertEquals(0, run(concat(base("VALIDATE_PSP_GROUP"), "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.XML_PROTOCOL_DIR, protocols.getPath())));
        assertTrue(stdout(), stdout().contains("není adresář ani soubor ZIP, ignoruji"));
        assertEquals(1, protocols.list().length);
    }

    // ---------------------------------------------------------------- kód 1

    @Test
    public void unknownFdmfEndsWithOne() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(1, run(concat(validate(minimalEperiodical("2.3"), xml), "--" + Params.FORCED_DMF_EPE_VERSION, "9.9")));
        assertTrue(stderr(), stderr().contains("není definována fDMF"));
        assertFalse(xml.exists());
    }

    @Test
    public void notAZipFileAsPspEndsWithOne() throws Exception {
        File notZip = new File(tmp.getRoot(), "neni-zip.txt");
        Files.write(notZip.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(1, run(validate(notZip, xml)));
        assertTrue(stdout(), stdout().contains("není adresář ani soubor ZIP"));
        assertFalse(xml.exists());
    }

    @Test
    public void zipPspWithoutTmpDirEndsWithOne() throws Exception {
        File zip = zipOf(new File(CLIPPING_02), "nk-00027x.zip");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(1, run(validate(zip, xml)));
        assertTrue(stderr(), stderr().contains("--" + Params.TMP_DIR));
        assertFalse(xml.exists());
    }

    @Test
    public void zipPspWithMissingTmpDirEndsWithOne() throws Exception {
        File zip = zipOf(new File(CLIPPING_02), "nk-00027x.zip");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(1, run(concat(validate(zip, xml), "--" + Params.TMP_DIR, new File(tmp.getRoot(), "neexistuje").getPath())));
        assertTrue(stderr(), stderr().startsWith("Chyba: adresář"));
        assertFalse(xml.exists());
    }

    @Test
    public void zipPspStillValidatesWithTmpDir() throws Exception {
        // protipříklad k předchozím dvěma: stejný ZIP s platným --tmp-dir skončí kódem 0
        File zip = zipOf(new File(CLIPPING_02), "nk-00027x.zip");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(0, run(concat(validate(zip, xml), "--" + Params.TMP_DIR, tmp.newFolder("tmp").getPath())));
        assertTrue(xml.isFile());
    }

    @Test
    public void packageWithoutMetsEndsWithOne() throws Exception {
        // PspDataException: v balíku není PRIMARY-METS
        File empty = tmp.newFolder("prazdny-balik");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(1, run(validate(empty, xml)));
        assertTrue(stderr(), stderr().startsWith("Chyba:"));
        assertFalse(xml.exists());
    }

    @Test
    public void packageWithBrokenXmlEndsWithOne() throws Exception {
        // XmlFileParsingException při čtení XML balíku (konfigurace je načtená)
        File dir = tmp.newFolder("rozbity-balik");
        Files.write(new File(dir, "mets_x.xml").toPath(), "<mets:mets xmlns:mets=\"x\"".getBytes(StandardCharsets.UTF_8));
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(1, run(validate(dir, xml)));
        assertTrue(stderr(), stderr().contains("Chyba:chyba parsování xml"));
        assertFalse(xml.exists());
    }

    @Test
    public void protocolThatCouldNotBeWrittenEndsWithOne() throws Exception {
        // nadřazený adresář protokolu je soubor, protokol nejde zapsat
        File blocker = new File(tmp.getRoot(), "soubor");
        Files.write(blocker.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        File xml = new File(blocker, "protokol.xml");
        assertEquals(1, run(validate(new File(CLIPPING_02), xml)));
        assertTrue(stderr(), stderr().contains("ERROR"));
        assertFalse(xml.exists());
    }

    @Test
    public void nonexistentPspEndsWithOneAndMessage() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        File missing = new File(tmp.getRoot(), "neexistuje");
        assertEquals(1, run(validate(missing, xml)));
        assertEquals("Chyba: balík " + missing.getAbsolutePath() + " neexistuje!", stderr().trim());
        assertFalse(stderr(), stderr().contains("Exception"));
        assertFalse(xml.exists());
    }

    @Test
    public void nonexistentPspGroupEndsWithOneAndMessage() throws Exception {
        File missing = new File(tmp.getRoot(), "neexistujici-skupina");
        File protocols = tmp.newFolder("protokoly-neexistujici-skupiny");
        assertEquals(1, run(concat(base("VALIDATE_PSP_GROUP"), "--" + Params.PSP_GROUP, missing.getPath(),
                "--" + Params.XML_PROTOCOL_DIR, protocols.getPath())));
        assertEquals("Chyba: skupina balíků " + missing.getAbsolutePath() + " neexistuje!", stderr().trim());
        assertEquals(0, protocols.list().length);
    }

    @Test
    public void groupWithUnknownFdmfEndsWithOne() throws Exception {
        File group = tmp.newFolder("skupina");
        copyDir(minimalEperiodical("2.3").toPath(), new File(group, "epe").toPath());
        File protocols = tmp.newFolder("protokoly");
        assertEquals(1, run(concat(base("VALIDATE_PSP_GROUP"), "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.FORCED_DMF_EPE_VERSION, "9.9", "--" + Params.XML_PROTOCOL_DIR, protocols.getPath())));
        assertTrue(stderr(), stderr().contains("není definována fDMF"));
        assertEquals(0, protocols.list().length);
    }

    @Test
    public void groupContinuesAfterUnusableZipAndEndsWithOne() throws Exception {
        // ZIP bez --tmp-dir se nezvaliduje (hláška na stderr), balík v adresáři se zvaliduje dál; kód je 1 bez ohledu na pořadí
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        zipOfTo(new File(CLIPPING_02), new File(group, "jiny.zip"));
        File protocols = tmp.newFolder("protokoly");
        assertEquals(1, run(concat(base("VALIDATE_PSP_GROUP"), "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.XML_PROTOCOL_DIR, protocols.getPath())));
        assertTrue(stderr(), stderr().contains("--" + Params.TMP_DIR));
        assertEquals(1, protocols.list().length);
    }

    // ---------------------------------------------------------------- kód 2

    @Test
    public void missingActionEndsWithTwo() throws Exception {
        assertEquals(2, run());
        assertTrue(stderr(), stderr().contains("prázdný parametr " + Params.ACTION));
        assertTrue(stdout(), stdout().contains("--" + Params.ACTION));
    }

    @Test
    public void unknownActionEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "NEEXISTUJICI_AKCE"));
        assertTrue(stderr(), stderr().contains("neznámá akce NEEXISTUJICI_AKCE"));
        assertTrue(stdout(), stdout().contains("--" + Params.ACTION));
    }

    @Test
    public void missingConfigDirEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.PSP, CLIPPING_02));
        assertTrue(stderr(), stderr().contains("--" + Params.CONFIG_DIR));
    }

    @Test
    public void missingPspEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.CONFIG_DIR, CONFIG));
        assertTrue(stderr(), stderr().contains("--" + Params.PSP));
    }

    @Test
    public void missingPspGroupEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "VALIDATE_PSP_GROUP", "--" + Params.CONFIG_DIR, CONFIG));
        assertTrue(stderr(), stderr().contains("--" + Params.PSP_GROUP));
    }

    @Test
    public void missingMinifiedDirEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "BUILD_MINIFIED_PACKAGE", "--" + Params.PSP, CLIPPING_02));
        assertTrue(stderr(), stderr().contains("--" + Params.MINIFIED_PSP_DIR));
    }

    @Test
    public void missingMetadataParamsEndWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE", "--" + Params.CONFIG_DIR, CONFIG,
                "--" + Params.METADATA_FILE, "x.xml"));
        assertTrue(stderr(), stderr().contains("--" + Params.METADATA_PROFILE_ID));
        err.reset();
        assertEquals(2, run("--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE", "--" + Params.CONFIG_DIR, CONFIG,
                "--" + Params.METADATA_PROFILE_ID, "a:b"));
        assertTrue(stderr(), stderr().contains("--" + Params.METADATA_FILE));
    }

    @Test
    public void unknownMetadataProfileEndsWithTwo() throws Exception {
        File file = new File(tmp.getRoot(), "m.xml");
        Files.write(file.toPath(), "<a/>".getBytes(StandardCharsets.UTF_8));
        assertEquals(2, run("--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE", "--" + Params.CONFIG_DIR, CONFIG,
                "--" + Params.METADATA_PROFILE_ID, "fund_unit_0.2:neexistuje", "--" + Params.METADATA_FILE, file.getPath()));
        assertTrue(stdout(), stdout().contains("Neznámý typ profilu"));
    }

    @Test
    public void metadataActionWithConfigWithoutManifestEndsWithThree() throws Exception {
        // adresář konfigurace bez manifest.properties (konfigurace 2.x): i akce nad jedním profilem ho odmítne
        File config = tmp.newFolder("konfigurace-bez-manifestu");
        assertTrue(new File(config, "dictionaries").mkdir());
        File file = new File(tmp.getRoot(), "m2.xml");
        Files.write(file.toPath(), "<a/>".getBytes(StandardCharsets.UTF_8));
        assertEquals(3, run("--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE", "--" + Params.CONFIG_DIR, config.getPath(),
                "--" + Params.METADATA_PROFILE_ID, "fund_unit_0.2:biblio:mods:clipping", "--" + Params.METADATA_FILE, file.getPath()));
        assertTrue(stderr(), stderr().contains("manifest.properties"));
    }

    @Test
    public void unknownOptionEndsWithTwo() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(2, run(concat(validate(new File(CLIPPING_02), xml), "--neexistujici-volba")));
        assertTrue(stderr(), stderr().startsWith("Chyba parsování parametrů:"));
        assertFalse(xml.exists());
    }

    @Test
    public void optionWithoutValueEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION));
        assertTrue(stderr(), stderr().startsWith("Chyba parsování parametrů:"));
    }

    @Test
    public void verbosityAboveRangeEndsWithTwo() throws Exception {
        assertEquals(2, run(verbosityArgs("4")));
        assertTrue(stderr(), stderr().contains("--" + Params.VERBOSITY));
    }

    @Test
    public void verbosityBelowRangeEndsWithTwo() throws Exception {
        assertEquals(2, run(verbosityArgs("-1")));
        assertTrue(stderr(), stderr().contains("--" + Params.VERBOSITY));
    }

    @Test
    public void verbosityNotANumberEndsWithTwo() throws Exception {
        assertEquals(2, run(verbosityArgs("abc")));
        assertTrue(stderr(), stderr().contains("--" + Params.VERBOSITY));
    }

    @Test
    public void verbosityBoundariesAreAccepted() throws Exception {
        for (String v : new String[]{"0", "3"}) {
            File xml = new File(tmp.getRoot(), "p" + v + ".xml");
            assertEquals(v, 0, run(concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.PSP, CLIPPING_02,
                    "--" + Params.CONFIG_DIR, CONFIG, "--" + Params.XML_PROTOCOL_FILE, xml.getPath(), "-v", v)));
            assertTrue(xml.isFile());
        }
    }

    @Test
    public void invalidResolverUrlEndsWithTwo() throws Exception {
        try {
            File xml = new File(tmp.getRoot(), "protokol.xml");
            // http:// bez --urnnbn-resolver-allow-http je chybná hodnota parametru
            assertEquals(2, run(concat(validate(new File(CLIPPING_02), xml), "--" + Params.URNNBN_RESOLVER_URL, "http://localhost:9")));
            assertFalse(xml.exists());
        } finally {
            // statická konfigurace resolveru se u chyby nezmění, vrátit výchozí pro jistotu
            nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker.setResolverBaseUrl(
                    nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker.DEFAULT_RESOLVER_BASE_URL, false);
        }
    }

    @Test
    public void minifiedPackageToMissingDirEndsWithTwo() throws Exception {
        assertEquals(2, run("--" + Params.ACTION, "BUILD_MINIFIED_PACKAGE", "--" + Params.PSP, CLIPPING_02,
                "--" + Params.MINIFIED_PSP_DIR, new File(tmp.getRoot(), "neexistuje").getPath()));
        assertTrue(stderr(), stderr().contains("neexistuje"));
    }

    @Test
    public void minifiedPackageEndsWithZero() throws Exception {
        File target = tmp.newFolder("minifikovane");
        assertEquals(0, run("--" + Params.ACTION, "BUILD_MINIFIED_PACKAGE", "--" + Params.PSP, CLIPPING_02,
                "--" + Params.MINIFIED_PSP_DIR, target.getPath()));
        assertTrue(new File(target, "nk-00027x_minified").isDirectory());
    }

    // ---------------------------------------------------------------- kód 3

    @Test
    public void configDirWithoutManifestEndsWithThree() throws Exception {
        File config = tmp.newFolder("konfigurace");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(3, run(validateWithConfig(new File(CLIPPING_02), xml, config)));
        assertTrue(stderr(), stderr().contains("manifest.properties"));
        assertFalse(xml.exists());
    }

    @Test
    public void configDirOfDifferentMajorVersionEndsWithThree() throws Exception {
        File config = tmp.newFolder("konfigurace");
        Files.write(new File(config, "manifest.properties").toPath(), "configVersion=2.9\n".getBytes(StandardCharsets.UTF_8));
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(3, run(validateWithConfig(new File(CLIPPING_02), xml, config)));
        assertTrue(stderr(), stderr().startsWith("Chyba:"));
        assertTrue(stderr(), stderr().contains("2.9"));
        assertFalse(xml.exists());
    }

    @Test
    public void nonexistentConfigDirEndsWithThree() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(3, run(validateWithConfig(new File(CLIPPING_02), xml, new File(tmp.getRoot(), "neexistuje"))));
        assertTrue(stderr(), stderr().startsWith("Chyba:"));
        assertFalse(xml.exists());
    }

    @Test
    public void groupWithBadConfigDirEndsWithThree() throws Exception {
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        File protocols = tmp.newFolder("protokoly");
        assertEquals(3, run(concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP_GROUP", "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.CONFIG_DIR, tmp.newFolder("prazdna-konfigurace").getPath(),
                "--" + Params.XML_PROTOCOL_DIR, protocols.getPath())));
        assertTrue(stderr(), stderr().contains("manifest.properties"));
        assertEquals(0, protocols.list().length);
    }

    // ---------------------------------------------------------------- pomocné

    private int run(String... args) {
        return Main.run(null, args);
    }

    private String stdout() throws Exception {
        return out.toString("UTF-8");
    }

    private String stderr() throws Exception {
        return err.toString("UTF-8");
    }

    private static String[] noTools() {
        return new String[]{"--disable-imagemagick", "--disable-jhove", "--disable-jpylyzer", "--disable-kakadu",
                "--disable-mp3val", "--disable-shntool", "--disable-checkmate", "--disable-verapdf", "--disable-epubcheck"};
    }

    private static String[] base(String action) {
        return concat(noTools(), "--" + Params.ACTION, action, "--" + Params.CONFIG_DIR, CONFIG);
    }

    private static String[] validate(File psp, File xmlProtocol) {
        return concat(base("VALIDATE_PSP"), "--" + Params.PSP, psp.getPath(), "--" + Params.XML_PROTOCOL_FILE, xmlProtocol.getPath());
    }

    private static String[] validateWithConfig(File psp, File xmlProtocol, File config) {
        return concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.PSP, psp.getPath(),
                "--" + Params.CONFIG_DIR, config.getPath(), "--" + Params.XML_PROTOCOL_FILE, xmlProtocol.getPath());
    }

    private static String[] verbosityArgs(String value) {
        return concat(base("VALIDATE_PSP"), "--" + Params.PSP, CLIPPING_02, "--" + Params.VERBOSITY + "=" + value);
    }

    private static String[] concat(String[] a, String... b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /** Minimální balík e-periodika (jen METS s TYPE a INFO s verzí), stejný jako v UnknownFdmfExitCodeTest. */
    private File minimalEperiodical(String infoVersion) throws Exception {
        File dir = tmp.newFolder("epe-" + infoVersion + "-" + System.nanoTime());
        Files.write(new File(dir, "mets_x.xml").toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" LABEL=\"x\" TYPE=\"electronic_periodical\"/>").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "info_x.xml").toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<info><metadataversion>" + infoVersion + "</metadataversion></info>").getBytes(StandardCharsets.UTF_8));
        return dir;
    }

    private File zipOf(File dir, String zipName) throws Exception {
        File zip = new File(tmp.newFolder("zip-" + System.nanoTime()), zipName);
        zipOfTo(dir, zip);
        return zip;
    }

    private static void zipOfTo(File dir, File zip) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            Path root = dir.toPath().getParent();
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                    zos.putNextEntry(new ZipEntry(root.relativize(file).toString().replace('\\', '/')));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void copyDir(Path src, Path dst) throws Exception {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws java.io.IOException {
                Files.createDirectories(dst.resolve(src.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                Files.copy(file, dst.resolve(src.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
