package nkp.pspValidator.cli;

import nkp.pspValidator.shared.Dmf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Balík, pro jehož typ a verzi DMF není definovaná fDMF (např. e-periodikum s metadataversion 1.7.1 v info.xml,
 * vzorek NK e-periodik 2.3 RDA): CLI skončí kódem 1 (jako u neexistujícího balíku), protokol nevznikne a hláška
 * uvede verzi, podporované verze typu a volbu, kterou lze verzi vynutit. Detekce verze se nemění.
 */
public class UnknownFdmfExitCodeTest {

    private static final String RES = "../sharedModule/src/test/resources/";
    private static final String CONFIG = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
    private static final String CLIPPING_02 = RES + "fund_unit_0.2/valid_clipping/nk-00027x";
    private static final String NL = System.lineSeparator();

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

    @Test
    public void unknownVersionOfKnownTypeEndsWithCodeOneAndAdvice() throws Exception {
        File psp = minimalEperiodical("1.7.1");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        int code = Main.run(null, concat(validateArgs(psp), "--" + Params.XML_PROTOCOL_FILE, xml.getPath()));
        assertEquals(1, code);
        assertTrue(stdout(), stdout().contains("Bude použita verze standardu E-periodika 1.7.1"));
        assertEquals("Chyba: balík nebyl zvalidován a protokol nevznikl. Pro zjištěnou verzi standardu E-periodika 1.7.1 "
                + "není definována fDMF (formalizovaná DMF)." + NL
                + "Podporované verze (E-periodika): 2.3, 2.4, 2.5, 2.6." + NL
                + "Pokud balík odpovídá jiné verzi, lze ji vynutit volbou --forced-dmf-epe-version VERZE "
                + "(verzi v balíku zjišťuje validátor z info.xml, element metadataversion)." + NL, stderr());
        assertFalse(xml.exists());
    }

    @Test
    public void forcedExistingVersionValidatesWithCodeZero() throws Exception {
        // detekce by dala 1.7.1, vynucena verze 2.3 existuje: validace probehne (minimalni balik je nevalidni,
        // to je vysledek validace, ne chyba CLI), protokol vznikne
        File psp = minimalEperiodical("1.7.1");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        int code = Main.run(null, concat(validateArgs(psp), "--" + Params.XML_PROTOCOL_FILE, xml.getPath(),
                "--" + Params.FORCED_DMF_EPE_VERSION, "2.3"));
        assertEquals(0, code);
        assertTrue(stdout(), stdout().contains("Bude použita verze standardu E-periodika 2.3"));
        assertTrue(stdout(), stdout().contains("balík je: nevalidní"));
        assertFalse(stderr(), stderr().contains("nebyl zvalidován"));
        assertTrue(xml.isFile());
    }

    @Test
    public void forcedMissingVersionEndsWithCodeOneAndNamesTheOption() throws Exception {
        File psp = minimalEperiodical("2.3");
        int code = Main.run(null, concat(validateArgs(psp), "--" + Params.FORCED_DMF_EPE_VERSION, "9.9"));
        assertEquals(1, code);
        assertEquals("Chyba: balík nebyl zvalidován a protokol nevznikl. Pro vynucenou (--forced-dmf-epe-version 9.9) "
                + "verzi standardu E-periodika 9.9 není definována fDMF (formalizovaná DMF)." + NL
                + "Podporované verze (E-periodika): 2.3, 2.4, 2.5, 2.6." + NL
                + "Zadejte ve volbě --forced-dmf-epe-version jednu z podporovaných verzí." + NL, stderr());
    }

    @Test
    public void deprecatedForcedAliasIsReportedByMainOptionName() throws Exception {
        // zastaraly alias VEP (--forced-dmf-eper-version) se prevadi na hlavni volbu, hlaska radi hlavni volbu
        File psp = minimalEperiodical("2.3");
        int code = Main.run(null, concat(validateArgs(psp), "--" + Params.FORCED_DMF_EPER_VERSION_DEPRECATED, "1.7.1"));
        assertEquals(1, code);
        assertTrue(stderr(), stderr().startsWith("Chyba: balík nebyl zvalidován a protokol nevznikl. Pro vynucenou "
                + "(--forced-dmf-epe-version 1.7.1) verzi standardu E-periodika 1.7.1 není definována fDMF"));
    }

    @Test
    public void validPackageEndsWithCodeZero() throws Exception {
        assertEquals(0, Main.run(null, validateArgs(new File(CLIPPING_02))));
        assertTrue(stdout(), stdout().contains("balík je: validní"));
        assertTrue(stderr(), stderr().isEmpty());
    }

    @Test
    public void invalidPackageStillEndsWithCodeZero() throws Exception {
        // nevalidni balik (vysledek validace) neni chyba CLI; webovy validator rozlisuje FINISHED podle kodu 0 a protokolu
        File psp = minimalEperiodical("2.6");
        File xml = new File(tmp.getRoot(), "protokol.xml");
        assertEquals(0, Main.run(null, concat(validateArgs(psp), "--" + Params.XML_PROTOCOL_FILE, xml.getPath())));
        assertTrue(stdout(), stdout().contains("balík je: nevalidní"));
        assertTrue(xml.isFile());
    }

    @Test
    public void optionCodeOfEveryTypeIsARealForcedOption() throws Exception {
        Main.run(null, new String[]{"--" + Params.HELP});
        String help = stdout();
        for (Dmf.Type type : Dmf.Type.values()) {
            String option = "--forced-dmf-" + Main.dmfTypeOptionCode(type) + "-version";
            assertTrue(type + " " + option, help.contains(option + " "));
        }
    }

    @Test
    public void versionsAreSortedNumerically() {
        assertEquals(Arrays.asList("1.0", "1.2", "1.3", "1.3.1", "1.3.2", "1.4", "1.10", "2.0"),
                Main.sortVersions(Arrays.asList("2.0", "1.10", "1.3.2", "1.0", "1.3.1", "1.4", "1.3", "1.2")));
    }

    private String stdout() throws Exception {
        return out.toString("UTF-8");
    }

    private String stderr() throws Exception {
        return err.toString("UTF-8");
    }

    /** Minimální balík e-periodika (jen METS s TYPE a INFO s verzí), stejný jako v MainTest. */
    private File minimalEperiodical(String infoVersion) throws Exception {
        File dir = tmp.newFolder("epe-" + infoVersion);
        Files.write(new File(dir, "mets_x.xml").toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" LABEL=\"x\" TYPE=\"electronic_periodical\"/>").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "info_x.xml").toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<info><metadataversion>" + infoVersion + "</metadataversion></info>").getBytes(StandardCharsets.UTF_8));
        return dir;
    }

    private static String[] validateArgs(File psp) {
        return new String[]{"--disable-imagemagick", "--disable-jhove", "--disable-jpylyzer", "--disable-kakadu",
                "--disable-mp3val", "--disable-shntool", "--disable-checkmate", "--disable-verapdf", "--disable-epubcheck",
                "--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.PSP, psp.getPath(), "--" + Params.CONFIG_DIR, CONFIG, "-v", "3"};
    }

    private static String[] concat(String[] a, String... b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
