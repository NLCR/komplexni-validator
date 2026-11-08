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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

/**
 * Zakladni test vstupu CLI: napoveda, verze a neplatne parametry nesmi vyhodit vyjimku ani zastavit JVM.
 * Validace balicku se testuje jinde (sharedModule); puvodni MainTest byl vyvojarsky runner s cestami mimo repo.
 */
public class MainTest {

    private static final String RES = "../sharedModule/src/test/resources/";
    private static final String CONFIG = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
    private static final String CLIPPING_02 = RES + "fund_unit_0.2/valid_clipping/nk-00027x";
    private static final String NS_PROTOCOL_29 = "http://www.nkp.cz/pspValidator/2.9/validationProtocol";

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

    private String stdout() throws Exception {
        return out.toString("UTF-8");
    }

    private String stderr() throws Exception {
        return err.toString("UTF-8");
    }

    @Test
    public void helpPrintsUsage() throws Exception {
        Main.main(null, new String[]{"--" + Params.HELP});
        assertTrue(stdout().contains("--" + Params.ACTION));
        assertTrue(stdout().contains("--" + Params.CONFIG_DIR));
        assertTrue(stderr().isEmpty());
    }

    @Test
    public void versionPrintsVersion() throws Exception {
        Main.main(null, new String[]{"--" + Params.VERSION});
        assertTrue(stdout().contains("Komplexni validator CLI verze"));
    }

    @Test
    public void missingActionReportsErrorAndHelp() throws Exception {
        Main.main(null, new String[]{});
        assertTrue(stderr().contains(Params.ACTION));
        assertTrue(stdout().contains("--" + Params.ACTION));
    }

    @Test
    public void unknownActionReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "NEEXISTUJICI_AKCE"});
        assertTrue(stderr().contains("NEEXISTUJICI_AKCE"));
    }

    @Test
    public void validatePspWithoutPspReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.CONFIG_DIR,
                "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig"});
        assertTrue(stderr().contains("--" + Params.PSP));
    }

    @Test
    public void validatePspWithoutConfigDirReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_PSP"});
        assertTrue(stderr().contains("--" + Params.CONFIG_DIR));
    }

    private static String[] concat(String[] a, String... b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static String[] noTools() {
        return new String[]{"--disable-imagemagick", "--disable-jhove", "--disable-jpylyzer", "--disable-kakadu",
                "--disable-mp3val", "--disable-shntool", "--disable-checkmate"};
    }

    private static String[] validateArgs(String psp) {
        return concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.PSP, psp,
                "--" + Params.CONFIG_DIR, CONFIG, "-v", "3");
    }

    private static void zipDir(File dir, File zip) throws Exception {
        Path root = dir.toPath().getParent();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
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
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) throws java.io.IOException {
                Files.createDirectories(dst.resolve(src.relativize(d).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws java.io.IOException {
                Files.copy(f, dst.resolve(src.relativize(f).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void validatePspDirWritesXmlProtocol() throws Exception {
        File xml = new File(tmp.getRoot(), "protokol.xml");
        Main.main(null, concat(validateArgs(CLIPPING_02), "--" + Params.XML_PROTOCOL_FILE, xml.getPath()));
        String o = stdout();
        assertTrue(o, o.contains("balík je: validní"));
        assertTrue(o, o.contains("Bude použita verze standardu Jednotky fondu 0.2"));
        assertTrue(o, o.contains("Vypnuto použití nástroje"));
        assertTrue(stderr(), stderr().isEmpty());
        assertTrue(xml.isFile());
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(xml);
        assertEquals(NS_PROTOCOL_29, doc.getDocumentElement().getNamespaceURI());
    }

    @Test
    public void validatePspWithXmlProtocolDir() throws Exception {
        File dir = tmp.newFolder("protokoly");
        Main.main(null, concat(validateArgs(CLIPPING_02), "--" + Params.XML_PROTOCOL_DIR, dir.getPath()));
        File[] files = dir.listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().endsWith("_nk-00027x.xml"));
    }

    @Test
    public void validatePspForcedVersionIsUsed() throws Exception {
        Main.main(null, concat(validateArgs(CLIPPING_02), "--" + Params.FORCED_DMF_FDU_VERSION, "0.1"));
        String o = stdout();
        assertTrue(o, o.contains("Bude použita verze standardu Jednotky fondu 0.1"));
        assertFalse(o, o.contains("Jednotky fondu 0.2"));
    }

    @Test
    public void validatePspZip() throws Exception {
        File zip = new File(tmp.newFolder("zip"), "balik.zip");
        zipDir(new File(CLIPPING_02), zip);
        File work = tmp.newFolder("work");
        Main.main(null, concat(validateArgs(zip.getPath()), "--" + Params.TMP_DIR, work.getPath()));
        String o = stdout();
        assertTrue(o, o.contains("Rozbaluji"));
        assertTrue(o, o.contains("balík je: validní"));
        assertTrue(new File(work, "balik.zip_extracted").isDirectory());
    }

    @Test
    public void validatePspZipWithoutTmpDirReportsError() throws Exception {
        File zip = new File(tmp.newFolder("zip"), "balik.zip");
        zipDir(new File(CLIPPING_02), zip);
        Main.main(null, validateArgs(zip.getPath()));
        assertTrue(stderr(), stderr().contains("--" + Params.TMP_DIR));
    }

    @Test
    public void validatePspNotAZipFileIsIgnored() throws Exception {
        File f = tmp.newFile("neni.zip");
        Files.write(f.toPath(), "text".getBytes("UTF-8"));
        Main.main(null, validateArgs(f.getPath()));
        assertTrue(stdout(), stdout().contains("není adresář ani soubor ZIP"));
    }

    @Test
    public void validatePspGroup() throws Exception {
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        copyDir(new File(RES + "fund_unit_0.2/valid_clipping_aacr/nk-00026q").toPath(), new File(group, "nk-00026q").toPath());
        Main.main(null, concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP_GROUP", "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.CONFIG_DIR, CONFIG, "-v", "3"));
        String o = stdout();
        assertTrue(o, o.contains("nk-00027x"));
        assertTrue(o, o.contains("nk-00026q"));
        assertEquals(o, 2, o.split("balík je: validní", -1).length - 1);
        assertTrue(stderr(), stderr().isEmpty());
    }

    @Test
    public void validatePspGroupZip() throws Exception {
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        File zip = new File(tmp.newFolder("zip"), "skupina.zip");
        zipDir(group, zip);
        File work = tmp.newFolder("work");
        Main.main(null, concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP_GROUP", "--" + Params.PSP_GROUP, zip.getPath(),
                "--" + Params.CONFIG_DIR, CONFIG, "--" + Params.TMP_DIR, work.getPath()));
        String o = stdout();
        assertTrue(o, o.contains("Rozbaluji"));
        assertTrue(o, o.contains("balík je: validní"));
    }

    @Test
    public void validatePspGroupWithoutGroupReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_PSP_GROUP", "--" + Params.CONFIG_DIR, CONFIG});
        assertTrue(stderr(), stderr().contains("--" + Params.PSP_GROUP));
    }

    /**
     * Zname chovani: neexistujici --psp konci neodchycenou IllegalStateException (zadna uzivatelska hlaska).
     * Test fixuje soucasny stav.
     */
    @Test
    public void validatePspNonexistentPspThrowsIllegalState() throws Exception {
        try {
            Main.main(null, validateArgs(new File(tmp.getRoot(), "neexistuje").getPath()));
            fail("ocekavana IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("neexistuje"));
        }
    }

    @Test
    public void validatePspInvalidVerbosityReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.PSP, CLIPPING_02,
                "--" + Params.CONFIG_DIR, CONFIG, "-v", "9"});
        assertTrue(stderr(), stderr().contains("--" + Params.VERBOSITY));
    }

    @Test
    public void buildMinifiedPackage() throws Exception {
        File out2 = tmp.newFolder("minified");
        Main.main(null, new String[]{"--" + Params.ACTION, "BUILD_MINIFIED_PACKAGE",
                "--" + Params.PSP, RES + "monograph_1.2/empty_images/b50eb6b0-f0a4-11e3-b72e-005056827e52",
                "--" + Params.MINIFIED_PSP_DIR, out2.getPath()});
        assertTrue(stdout(), stdout().contains("Vyrábím minifikovaný psp balík"));
        File[] created = out2.listFiles();
        assertEquals(1, created.length);
        assertTrue(created[0].isDirectory());
        assertTrue(created[0].list().length > 0);
    }

    @Test
    public void buildMinifiedPackageMissingOutputDirReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "BUILD_MINIFIED_PACKAGE",
                "--" + Params.PSP, CLIPPING_02,
                "--" + Params.MINIFIED_PSP_DIR, new File(tmp.getRoot(), "neni").getPath()});
        assertTrue(stderr(), stderr().contains("neexistuje"));
    }

    @Test
    public void validateMetadataByProfile() throws Exception {
        File xml = new File(tmp.getRoot(), "profil.xml");
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE",
                "--" + Params.CONFIG_DIR, CONFIG,
                "--" + Params.METADATA_PROFILE_ID, "monograph_1.4:biblio:mods:page_rda",
                "--" + Params.METADATA_FILE, RES + "metadata_only/mon_1.4/mods_page_rda.xml",
                "--" + Params.XML_PROTOCOL_FILE, xml.getPath()});
        assertTrue(stdout(), stdout().contains("Validuji oproti profilu monograph_1.4:biblio:mods:page_rda"));
        assertTrue(xml.isFile());
        assertTrue(stderr(), stderr().isEmpty());
    }

    @Test
    public void validateMetadataByProfileUnknownProfileDoesNotThrow() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE",
                "--" + Params.CONFIG_DIR, CONFIG,
                "--" + Params.METADATA_PROFILE_ID, "monograph_1.4:biblio:mods:blbost",
                "--" + Params.METADATA_FILE, RES + "metadata_only/mon_1.4/mods_page_rda.xml"});
        assertTrue(stdout() + stderr(), stdout().contains("Soubor profilu neexistuje: blbost"));
    }

    @Test
    public void validateMetadataByProfileWithoutProfileIdReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_METADATA_BY_PROFILE", "--" + Params.CONFIG_DIR, CONFIG});
        assertTrue(stderr(), stderr().contains("--" + Params.METADATA_PROFILE_ID));
    }
}
