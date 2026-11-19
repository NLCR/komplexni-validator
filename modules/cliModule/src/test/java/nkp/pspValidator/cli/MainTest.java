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
    private static final String NS_PROTOCOL = "http://www.nkp.cz/pspValidator/3.0/validationProtocol";

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
    public void helpListsVeraPdfAndEpubCheckOptions() throws Exception {
        Main.main(null, new String[]{"--" + Params.HELP});
        for (String option : new String[]{Params.VERAPDF_PATH, Params.EPUBCHECK_PATH, Params.DISABLE_VERAPDF, Params.DISABLE_EPUBCHECK}) {
            assertTrue(option, stdout().contains("--" + option));
        }
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
                "--disable-mp3val", "--disable-shntool", "--disable-checkmate", "--disable-verapdf", "--disable-epubcheck"};
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
        assertEquals(NS_PROTOCOL, doc.getDocumentElement().getNamespaceURI());
    }

    @Test
    public void veraPdfAndEpubCheckCanBeDisabled() throws Exception {
        Main.main(null, validateArgs(CLIPPING_02));
        String o = stdout();
        assertTrue(o, o.contains("Vypnuto použití nástroje veraPDF."));
        assertTrue(o, o.contains("Vypnuto použití nástroje EPUBCheck."));
        assertFalse(o, o.contains("Kontroluji dostupnost nástroje"));
    }

    @Test
    public void toolWithFailedVersionDetectionIsUnavailable() throws Exception {
        // nalez 27: po neuspesne detekci verze se nastroj oznaci jako nedostupny; validace pak u pravidla hlasi
        // jednu INFO hlasku "nástroj ... není dostupný" misto ERROR "Cannot run program" u kazdeho souboru
        File missing = new File(tmp.getRoot(), "neexistuje");
        String[] args = concat(validateArgs(CLIPPING_02), "--" + Params.JHOVE_PATH, missing.getPath(),
                "--" + Params.VERAPDF_PATH, missing.getPath(), "--" + Params.EPUBCHECK_PATH, missing.getPath());
        args = java.util.Arrays.stream(args)
                .filter(a -> !a.equals("--" + Params.DISABLE_JHOVE) && !a.equals("--" + Params.DISABLE_VERAPDF) && !a.equals("--" + Params.DISABLE_EPUBCHECK))
                .toArray(String[]::new);
        Main.main(null, args);
        String o = stdout();
        assertTrue(o, o.contains("Kontroluji dostupnost nástroje JHOVE: nenalezen ("));
        assertTrue(o, o.contains("Kontroluji dostupnost nástroje veraPDF: nenalezen ("));
        // EPUBCheck se spousti pres java -jar <cesta>/epubcheck.jar: java se najde, chybu o chybejicim jaru vypise
        // na stderr a stdout je prazdny ("nenalezen (prázdný výstup)")
        assertTrue(o, o.contains("Kontroluji dostupnost nástroje EPUBCheck: nenalezen ("));
        assertTrue(o, o.contains("Pravidlo MASTER-COPY_FILES_VALID_BY_JHOVE: 1 problém (1x INFO)"));
        assertTrue(o, o.contains("INFO: nástroj JHOVE není dostupný"));
        assertFalse(o, o.contains("ERROR: Cannot run program"));
        assertTrue(o, o.contains("balík je: validní"));
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

    /**
     * Minimalni balik e-publikace (jen METS s TYPE a INFO s verzi) pro overeni volby verze DMF; validace sama
     * na takovem baliku hlasi chyby, testy kontroluji jen pouzitou verzi standardu.
     */
    private File minimalEpublicationPsp(String metsType, String infoVersion) throws Exception {
        File dir = tmp.newFolder("epub-" + metsType);
        Files.write(new File(dir, "mets_x.xml").toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" LABEL=\"x\" TYPE=\"" + metsType + "\"/>").getBytes("UTF-8"));
        Files.write(new File(dir, "info_x.xml").toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<info><metadataversion>" + infoVersion + "</metadataversion></info>").getBytes("UTF-8"));
        return dir;
    }

    private void assertForcedVersion(String metsType, String option, String version, String expected) throws Exception {
        //verze v INFO (9.9) neexistuje, vynucena verze ma prednost
        File psp = minimalEpublicationPsp(metsType, "9.9");
        Main.main(null, concat(validateArgs(psp.getPath()), "--" + option, version));
        String o = stdout();
        assertTrue(o, o.contains("Bude použita verze standardu " + expected));
        assertFalse(o, o.contains("9.9"));
    }

    @Test
    public void validatePspForcedVersionEmonograph() throws Exception {
        assertForcedVersion("electronic_monograph", Params.FORCED_DMF_EMO_VERSION, "3.0", "E-monografie 3.0");
    }

    @Test
    public void validatePspForcedVersionEperiodical() throws Exception {
        assertForcedVersion("electronic_periodical", Params.FORCED_DMF_EPE_VERSION, "2.5", "E-periodika 2.5");
    }

    @Test
    public void validatePspForcedVersionComposedPeriodical() throws Exception {
        assertForcedVersion("electronic_coll_journal", Params.FORCED_DMF_SEP_VERSION, "1.3", "Skládaná e-periodika 1.3");
    }

    @Test
    public void validatePspForcedVersionDeprecatedVepAliases() throws Exception {
        //volby Validatoru e-publikaci (emon/eper/comper) funguji jako zastarale aliasy
        assertForcedVersion("electronic_monograph", Params.FORCED_DMF_EMON_VERSION_DEPRECATED, "3.0", "E-monografie 3.0");
        out.reset();
        assertForcedVersion("electronic_periodical", Params.FORCED_DMF_EPER_VERSION_DEPRECATED, "2.5", "E-periodika 2.5");
        out.reset();
        assertForcedVersion("electronic_coll_journal", Params.FORCED_DMF_COMPER_VERSION_DEPRECATED, "1.3", "Skládaná e-periodika 1.3");
    }

    @Test
    public void validatePspMainOptionTakesPrecedenceOverDeprecatedAlias() throws Exception {
        File psp = minimalEpublicationPsp("electronic_monograph", "9.9");
        Main.main(null, concat(validateArgs(psp.getPath()), "--" + Params.FORCED_DMF_EMON_VERSION_DEPRECATED, "2.9",
                "--" + Params.FORCED_DMF_EMO_VERSION, "3.0"));
        assertTrue(stdout(), stdout().contains("Bude použita verze standardu E-monografie 3.0"));
    }

    @Test
    public void validatePspPreferredVersionEpublication() throws Exception {
        //bez verze v INFO se pouzije preferovana verze (i pres zastaraly alias)
        File psp = minimalEpublicationPsp("electronic_periodical", "");
        Main.main(null, concat(validateArgs(psp.getPath()), "--" + Params.PREFERRED_DMF_EPER_VERSION_DEPRECATED, "2.5"));
        assertTrue(stdout(), stdout().contains("Bude použita verze standardu E-periodika 2.5"));
    }

    @Test
    public void helpMarksVepOptionsAsDeprecated() throws Exception {
        Main.main(null, new String[]{"--" + Params.HELP});
        String o = stdout();
        for (String option : new String[]{Params.FORCED_DMF_EMO_VERSION, Params.FORCED_DMF_EPE_VERSION, Params.FORCED_DMF_SEP_VERSION,
                Params.PREFERRED_DMF_EMO_VERSION, Params.PREFERRED_DMF_EPE_VERSION, Params.PREFERRED_DMF_SEP_VERSION,
                Params.FORCED_DMF_COMPER_VERSION_DEPRECATED}) {
            assertTrue(option, o.contains("--" + option));
        }
        //HelpFormatter zalamuje popis, porovnava se bez zalomeni
        String flat = o.replaceAll("\\s+", " ");
        assertTrue(o, flat.contains("--" + Params.FORCED_DMF_EMON_VERSION_DEPRECATED + " <VERZE> Zastarale, pouzijte --" + Params.FORCED_DMF_EMO_VERSION + "."));
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
    public void validatePspGroupPrintsIntroOnce() throws Exception {
        // issue #166: konfigurace, detekce nastroju a registr fDMF se u skupiny inicializuji jednou, uvodni vypis
        // (platforma, adresar konfigurace, nastroje) je jen jednou na zacatku, ne u kazdeho balicku
        File group = tmp.newFolder("skupina");
        copyDir(new File(CLIPPING_02).toPath(), new File(group, "nk-00027x").toPath());
        copyDir(new File(RES + "fund_unit_0.2/valid_clipping_aacr/nk-00026q").toPath(), new File(group, "nk-00026q").toPath());
        copyDir(new File(RES + "epublikace/eperiodical_2.6/valid_pdf_originaldata/5e0b0003-0000-4000-8000-000000000261").toPath(),
                new File(group, "5e0b0003-0000-4000-8000-000000000261").toPath());
        // JHOVE zapnuty s neexistujici cestou: detekce probehne (a selze) jen jednou
        File missing = new File(tmp.getRoot(), "neexistuje");
        String[] args = concat(noTools(), "--" + Params.ACTION, "VALIDATE_PSP_GROUP", "--" + Params.PSP_GROUP, group.getPath(),
                "--" + Params.CONFIG_DIR, CONFIG, "-v", "1", "--" + Params.JHOVE_PATH, missing.getPath());
        args = java.util.Arrays.stream(args).filter(a -> !a.equals("--" + Params.DISABLE_JHOVE)).toArray(String[]::new);
        Main.main(null, args);
        String o = stdout();
        assertEquals(o, 1, count(o, "Platforma: "));
        assertEquals(o, 1, count(o, "Kořenový adresář konfigurace validátoru: "));
        assertEquals(o, 1, count(o, "Vypnuto použití nástroje ImageMagick."));
        assertEquals(o, 1, count(o, "Kontroluji dostupnost nástroje JHOVE: nenalezen ("));
        assertEquals(o, 3, count(o, "Zpracovávám PSP balík "));
        assertEquals(o, 3, count(o, "Validátor inicializován, spouštím validace"));
        assertEquals(o, 3, count(o, "balík je: validní"));
        assertTrue(o, o.contains("Bude použita verze standardu Jednotky fondu 0.2"));
        assertTrue(o, o.contains("Bude použita verze standardu E-periodika 2.6"));
        // uvod je pred prvnim balikem
        assertTrue(o, o.indexOf("Platforma: ") < o.indexOf("Zpracovávám PSP balík "));
        assertTrue(o, o.indexOf("Kontroluji dostupnost nástroje JHOVE") < o.indexOf("Zpracovávám PSP balík "));
    }

    @Test
    public void validatePspPrintsIntroOnce() throws Exception {
        Main.main(null, validateArgs(CLIPPING_02));
        String o = stdout();
        assertEquals(o, 1, count(o, "Platforma: "));
        assertEquals(o, 1, count(o, "Kořenový adresář konfigurace validátoru: "));
        assertEquals(o, 1, count(o, "Zpracovávám PSP balík "));
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
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
