package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilExecution;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.loadProfile;
import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.output;
import static org.junit.Assert.*;

/**
 * Profily binarnich souboru z fDMF fund_unit_0.2 (binaryFileProfiles/{mc,uc}) postavene parserem aplikace
 * (BinaryFileValidator.registerProfile) a vyhodnocene nad ulozenymi vystupy nastroju bez spusteni procesu
 * (CannedOutputExternalUtilManager). Ocekavane chyby odpovidaji soucasnemu chovani pri vystupech
 * Jpylyzer 2.2.0, JHOVE 1.29, Kakadu 8.5 a ImageMagick 7.1 (viz src/test/resources/externalUtils/README.md).
 */
public class BinaryFileProfileTest {

    private static final String FDMF = "fund_unit_0.2";
    private static final File DUMMY_FILE = new File("src/test/tools/jp2/mc.jp2");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Locale originalLocale;

    @Before
    public void setLocale() {
        // hlaseni isInFloatRange zavisi na Locale (viz ConstraintsTest)
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    private static List<String> validate(ExternalUtil util, String copy, String outputName) throws Exception {
        String dir = utilDir(util);
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(util, output(dir, outputName));
        BinaryFileProfile profile = loadProfile(manager, FDMF, copy, util);
        return profile.validate("jp2k", DUMMY_FILE);
    }

    private static String utilDir(ExternalUtil util) {
        switch (util) {
            case JPYLYZER:
                return "jpylyzer";
            case JHOVE:
                return "jhove";
            case KAKADU:
                return "kakadu";
            case IMAGE_MAGICK:
                return "imageMagick";
            default:
                throw new IllegalArgumentException(util.name());
        }
    }

    // Jpylyzer

    @Test
    public void jpylyzerMcOnToolsMc() throws Exception {
        // mc.jp2 pro synteticke balicky nema resolution box a je bez coding bypass
        assertEquals(Arrays.asList(
                "Rozlišení vertik.: kontrolovaná hodnota nenalezena",
                "Rozlišení horiz.: kontrolovaná hodnota nenalezena",
                "Bypass: hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes']"
        ), validate(ExternalUtil.JPYLYZER, "mc", "tools_mc.xml"));
    }

    @Test
    public void jpylyzerMcOnLrcp() throws Exception {
        assertEquals(Arrays.asList(
                "Rozlišení vertik.: hodnota \"39.37\" neodpovídá žádnému z omezení: [musí být alespoň 11732.000000]",
                "Rozlišení horiz.: hodnota \"39.37\" neodpovídá žádnému z omezení: [musí být alespoň 11732.000000]",
                "Order: hodnota \"LRCP\" neodpovídá žádnému z omezení: [musí být přesně 'RPCL']"
        ), validate(ExternalUtil.JPYLYZER, "mc", "odlis_od_prof_mc_LRCP.xml"));
    }

    @Test
    public void jpylyzerUcOnUc1CompressionRatio() throws Exception {
        // ok_uc/UC1.jp2 ma kompresni pomer 2.67, profil UC vyzaduje alespon 6
        assertEquals(Collections.singletonList(
                "Kompresní poměr: hodnota \"2.67\" neodpovídá žádnému z omezení: [musí být alespoň 6.000000]"
        ), validate(ExternalUtil.JPYLYZER, "uc", "ok_uc_UC1.xml"));
    }

    @Test
    public void jpylyzerUcOnBezSop() throws Exception {
        // UC profil SOP nekontroluje (validace zakomentovana), bypass ano
        assertEquals(Collections.singletonList(
                "Bypass: hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes']"
        ), validate(ExternalUtil.JPYLYZER, "uc", "odlis_od_prof_uc_bezSOP.xml"));
    }

    @Test
    public void jpylyzerUcOnToolsUc() throws Exception {
        List<String> errors = validate(ExternalUtil.JPYLYZER, "uc", "tools_uc.xml");
        assertEquals(errors.toString(), 9, errors.size());
        assertEquals("Rozlišení vertik.: kontrolovaná hodnota nenalezena", errors.get(0));
        assertEquals("Velikost dlaždic (1): hodnota \"1685\" neodpovídá žádnému z omezení: [musí být přesně '1024']", errors.get(2));
        assertEquals("Precincts (regiony): hodnota \"default\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes',musí být přesně 'user defined']", errors.get(4));
        // známá vada: NTimesXRemainingYConstraint nema toString(), hlaseni obsahuje nazev tridy a hash
        assertTrue(errors.get(5), errors.get(5).startsWith("Velikost precincts (1): hodnota \"[32768,32768,32768,32768,32768,32768]\" neodpovídá žádnému z omezení: [nkp.pspValidator.shared.externalUtils.validation.rules.constraints.NTimesXRemainingYConstraint@"));
        assertTrue(errors.get(6), errors.get(6).startsWith("Velikost precincts (2): "));
        assertEquals("Layers: hodnota \"8\" neodpovídá žádnému z omezení: [musí být přesně '12']", errors.get(7));
        assertEquals("Bypass: hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes']", errors.get(8));
    }

    @Test
    public void jpylyzerMcOnEmptyFile() throws Exception {
        // prazdny soubor: Valid False a vsechny vlastnosti chybi; kazda validace hlasi nejvys jednu chybu
        // (prvni neuspesne pravidlo, dalsi se nevyhodnocuji)
        List<String> errors = validate(ExternalUtil.JPYLYZER, "mc", "nevalidni_u5463f.xml");
        assertEquals("Valid: hodnota \"False\" neodpovídá žádnému z omezení: [musí být přesně 'True']", errors.get(0));
        assertEquals("Height: kontrolovaná hodnota nenalezena", errors.get(1));
        for (String error : errors.subList(1, errors.size())) {
            assertTrue(error, error.endsWith(": kontrolovaná hodnota nenalezena"));
        }
        // Valid + 23 validaci vlastnosti obrazu (Height az Migrační sw); File name, Last modified, Size, Tool name,
        // Tool version a regions_of_interest projdou
        assertEquals(errors.toString(), 24, errors.size());
    }

    @Test
    public void jpylyzerMcOnInvalid() throws Exception {
        List<String> errors = validate(ExternalUtil.JPYLYZER, "mc", "nevalidni_u0911e.xml");
        assertEquals("Valid: hodnota \"False\" neodpovídá žádnému z omezení: [musí být přesně 'True']", errors.get(0));
    }

    // JHOVE

    @Test
    public void jhoveMcOnToolsMc() throws Exception {
        assertEquals(Collections.emptyList(), validate(ExternalUtil.JHOVE, "mc", "tools_mc.xml"));
    }

    @Test
    public void jhoveUcOnToolsUc() throws Exception {
        assertEquals(Arrays.asList(
                "Velikost dlaždic (1): hodnota \"1685\" neodpovídá žádnému z omezení: [musí být přesně '1024']",
                "Velikost dlaždic (2): hodnota \"2806\" neodpovídá žádnému z omezení: [musí být přesně '1024']",
                "Layers: hodnota \"8\" neodpovídá žádnému z omezení: [musí být přesně '12']"
        ), validate(ExternalUtil.JHOVE, "uc", "tools_uc.xml"));
    }

    @Test
    public void jhoveMcOnLrcp() throws Exception {
        // JHOVE vypisuje poradi jako cislo (0 = LRCP), profil chce 2 (RPCL)
        assertEquals(Collections.singletonList("Order: hodnota \"0\" neodpovídá žádnému z omezení: [musí být přesně '2']"),
                validate(ExternalUtil.JHOVE, "mc", "odlis_od_prof_mc_LRCP.xml"));
    }

    @Test
    public void jhoveUcDoesNotSeeMissingSop() throws Exception {
        // JHOVE SOP ani bypass nekontroluje, soubor z odlis_od_prof_uc projde
        assertEquals(Collections.emptyList(), validate(ExternalUtil.JHOVE, "uc", "odlis_od_prof_uc_bezSOP.xml"));
        assertEquals(Collections.emptyList(), validate(ExternalUtil.JHOVE, "uc", "ok_uc_UC1.xml"));
    }

    @Test
    public void jhoveMcOnInvalid() throws Exception {
        List<String> errors = validate(ExternalUtil.JHOVE, "mc", "nevalidni_u0911e.xml");
        assertEquals("valid: hodnota \"Not well-formed\" neodpovídá žádnému z omezení: [musí být přesně 'Well-Formed and valid']", errors.get(0));
        assertTrue(errors.contains("Počet dlaždic: hodnota \"0\" neodpovídá žádnému z omezení: [musí být alespoň 1]"));
    }

    @Test
    public void jhoveMcOnOldHarvardNamespace() throws Exception {
        // známá vada: u stareho jmenneho prostoru JHOVE (hul.harvard.edu, JHOVE 1.6) vrati count() z prvniho XPath 0
        // a druhy XPath se nezkusi, "Počet dlaždic" tak selze i u spravneho souboru (viz XpathDataExtractionTest)
        List<String> errors = validate(ExternalUtil.JHOVE, "mc", "harvard_ns_rel1.6_mc.xml");
        assertTrue(errors.toString(), errors.contains("Počet dlaždic: hodnota \"0\" neodpovídá žádnému z omezení: [musí být alespoň 1]"));
    }

    // Kakadu

    @Test
    public void kakaduWithoutError() throws Exception {
        assertEquals(Collections.emptyList(), validate(ExternalUtil.KAKADU, "mc", "tools_mc.txt"));
        assertEquals(Collections.emptyList(), validate(ExternalUtil.KAKADU, "uc", "tools_uc.txt"));
    }

    @Test
    public void kakaduError() throws Exception {
        assertEquals(Collections.singletonList("no_errors: zakázaná hodnota nalezena: Kakadu Core Error:\nExpected to find EPH marker following packet header.  Found 0x0 instead."),
                validate(ExternalUtil.KAKADU, "uc", "detekce_poskozeni_w0352gUC.txt"));
        assertEquals(2, validate(ExternalUtil.KAKADU, "mc", "synteticky_dve_chyby.txt").size());
    }

    // ImageMagick

    @Test
    public void imageMagick() throws Exception {
        assertEquals(Collections.emptyList(), validate(ExternalUtil.IMAGE_MAGICK, "mc", "synteticky_ok.txt"));
        assertEquals(Collections.singletonList("no_errors: zakázaná hodnota nalezena: identify: no decode delegate for this image format `src/test/tools/jp2/mc.jp2' @ error/constitute.c/ReadImage/753."),
                validate(ExternalUtil.IMAGE_MAGICK, "mc", "no_delegate_tools_mc.txt"));
        assertEquals(2, validate(ExternalUtil.IMAGE_MAGICK, "uc", "synteticky_poskozeny.txt").size());
        assertEquals(Collections.singletonList("no_errors: zakázaná hodnota nalezena: Error: Codestream too short"),
                validate(ExternalUtil.IMAGE_MAGICK, "uc", "synteticky_error_radek.txt"));
    }

    // chovani BinaryFileProfile.validate

    @Test
    public void executionNameAndTargetFileArePassedToManager() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.KAKADU, "");
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.KAKADU);
        profile.validate("jp2k", DUMMY_FILE);
        assertEquals(new ExternalUtilExecution("jp2k", ExternalUtil.KAKADU), manager.getLastExecution());
        assertEquals(DUMMY_FILE, manager.getLastTargetFile());
        assertTrue(profile.toString(), profile.toString().startsWith("BinaryFileProfile{externalUtil=KAKADU, validations=1"));
    }

    @Test
    public void toolFailureIsSingleError() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withFailure(ExternalUtil.JPYLYZER, "Cannot run program \"jpylyzer\": error=2, No such file or directory");
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.JPYLYZER);
        assertEquals(Collections.singletonList("Cannot run program \"jpylyzer\": error=2, No such file or directory"), profile.validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void nonXmlOutputOfXmlProfile() throws Exception {
        // napr. JHOVE vypise misto XML jen chybu na stdout
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.JHOVE, "Exception in thread \"main\" java.lang.OutOfMemoryError");
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.JHOVE);
        List<String> errors = profile.validate("jp2k", DUMMY_FILE);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).startsWith("chyba parsování xml (výstup nástroje JHOVE): "));
    }

    @Test
    public void emptyOutputOfXmlProfile() throws Exception {
        // ExternalUtilManager vraci "" i pri prazdnem vystupu nastroje
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.JPYLYZER, "");
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.JPYLYZER);
        List<String> errors = profile.validate("jp2k", DUMMY_FILE);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).startsWith("chyba parsování xml (výstup nástroje JPYLYZER): "));
    }

    @Test
    public void unexpectedExceptionIsSwallowed() throws Exception {
        // známá vada: BinaryFileProfile.validate konci "finally { return totalErrors; }", ktery spolkne kazdou
        // neocekavanou vyjimku (NullPointerException, ClassCastException ...). Soubor pak projde bez jedine chyby.
        // Zde null vystup u XML profilu (NPE v XmlUtils.buildDocumentFromString).
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.JPYLYZER, null);
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.JPYLYZER);
        assertEquals(Collections.emptyList(), profile.validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void unexpectedExceptionInConstraintStopsValidationSilently() throws Exception {
        // známá vada: chybne nakonfigurovane omezeni (isInIntRange nad resultType STRING) vyhodi ClassCastException,
        // ta se spolkne (viz vyse) a dalsi validace se uz nevyhodnoti; chyba z prvni validace zustane
        File profileFile = writeProfile("<fromText>"
                + validationText("prvni", "(?m)^A.*$", "<mustNotExist/>")
                + validationText("druha", "[0-9]+", "<mustMatchAny><isInIntRange><min>1</min></isInIntRange></mustMatchAny>")
                + validationText("treti", "(?m)^C.*$", "<mustNotExist/>")
                + "</fromText>");
        BinaryFileProfile profile = register(ExternalUtil.KAKADU, profileFile, "A1\n5\nC3");
        assertEquals(Collections.singletonList("prvni: zakázaná hodnota nalezena: A1"),
                profile.validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void extractionExceptionIsReportedAndOtherValidationsContinue() throws Exception {
        File profileFile = writeProfile("<fromText>"
                + validationText("spatny_regexp", "(", "<mustNotExist/>")
                + validationText("dalsi", "(?m)^C.*$", "<mustNotExist/>")
                + "</fromText>");
        BinaryFileProfile profile = register(ExternalUtil.KAKADU, profileFile, "C3");
        List<String> errors = profile.validate("jp2k", DUMMY_FILE);
        assertEquals(errors.toString(), 2, errors.size());
        // hlaseni extrakce je toString() vyjimky PatternSyntaxException (bez nazvu validace)
        assertTrue(errors.get(0), errors.get(0).startsWith("java.util.regex.PatternSyntaxException"));
        assertEquals("dalsi: zakázaná hodnota nalezena: C3", errors.get(1));
    }

    @Test
    public void textProfileWithEndsWithAndRuleOrder() throws Exception {
        // endsWith se v zadnem fDMF nepouziva, parser ho ale umi; pravidla se vyhodnocuji v poradi a prvni chyba konci validaci
        File profileFile = writeProfile("<fromText>"
                + validationFirst("pripona", "(?m)^soubor:.*$", "<mustExist/><mustMatchAny><endsWith>.jp2</endsWith><endsWith>.jpf</endsWith></mustMatchAny>")
                + "</fromText>");
        assertEquals(Collections.emptyList(), register(ExternalUtil.KAKADU, profileFile, "soubor: a.jp2").validate("jp2k", DUMMY_FILE));
        assertEquals(Collections.emptyList(), register(ExternalUtil.KAKADU, profileFile, "soubor: a.jpf").validate("jp2k", DUMMY_FILE));
        assertEquals(Collections.singletonList("pripona: hodnota \"soubor: a.tif\" neodpovídá žádnému z omezení: [musí končit na '.jp2',musí končit na '.jpf']"),
                register(ExternalUtil.KAKADU, profileFile, "soubor: a.tif").validate("jp2k", DUMMY_FILE));
        assertEquals(Collections.singletonList("pripona: kontrolovaná hodnota nenalezena"),
                register(ExternalUtil.KAKADU, profileFile, "nic").validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void xmlProfileWithDefaultNamespaceAndRanges() throws Exception {
        File profileFile = writeProfile("<fromXml><namespaces><namespace prefix=\"a\">urn:a</namespace><defaultNamespace>urn:d</defaultNamespace></namespaces>"
                + "<validation name=\"cislo\"><xmlDataExtraction resultType=\"INTEGER\"><firstNonempty><xpath>//a:n</xpath></firstNonempty></xmlDataExtraction>"
                + "<rules><mustExist/><mustMatchAny><isInIntRange><min>1</min><max>3</max></isInIntRange></mustMatchAny></rules></validation>"
                + "<validation name=\"desetinne\"><xmlDataExtraction resultType=\"FLOAT\"><firstNonempty><xpath>//a:f</xpath></firstNonempty></xmlDataExtraction>"
                + "<rules><mustMatchAny><isInFloatRange><max>1.5</max></isInFloatRange></mustMatchAny></rules></validation>"
                + "</fromXml>");
        BinaryFileValidator validator = new BinaryFileValidator(new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.JHOVE, "<r xmlns:a=\"urn:a\"><a:n>5</a:n><a:f>1.25</a:f></r>"));
        validator.registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.JHOVE, profileFile);
        BinaryFileXmlProfile profile = (BinaryFileXmlProfile) validator.getProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.JHOVE);
        assertEquals("urn:a", profile.getNamespaceContext().getNamespaceURI("a"));
        assertEquals(Collections.singletonList("cislo: hodnota \"5\" neodpovídá žádnému z omezení: [musí být v intervalu <1, 3>]"),
                profile.validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void unknownConstraintFailsOnRegistration() throws Exception {
        File profileFile = writeProfile("<fromText>" + validationFirst("x", "a", "<mustMatchAny><startsWith>a</startsWith></mustMatchAny>") + "</fromText>");
        try {
            new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.KAKADU, profileFile);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("unknown constraint startsWith", e.getMessage());
        }
    }

    @Test
    public void unknownProfileTypeAndRuleFailOnRegistration() throws Exception {
        try {
            new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.KAKADU,
                    writeProfile("<fromJson/>"));
            fail();
        } catch (nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException e) {
            assertEquals("neznámý element fromJson", e.getMessage());
        }
        try {
            new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.KAKADU,
                    writeProfile("<fromText>" + validationFirst("x", "a", "<mustBeEmpty/>") + "</fromText>"));
            fail();
        } catch (nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("mustBeEmpty"));
        }
    }

    @Test
    public void allFdmfProfilesAreParsable() throws Exception {
        // vsechny profily binarnich souboru ve vsech fDMF (obrazove i zvukove) projdou parserem
        int count = 0;
        File[] fdmfDirs = ExternalUtilsTestSupport.FDMF_DIR.listFiles(File::isDirectory);
        assertNotNull(fdmfDirs);
        for (File fdmfDir : fdmfDirs) {
            File[] typeDirs = new File(fdmfDir, "binaryFileProfiles").listFiles(File::isDirectory);
            if (typeDirs == null) {
                continue;
            }
            for (File typeDir : typeDirs) {
                for (ExternalUtil util : ExternalUtil.values()) {
                    File profileFile = new File(typeDir, util.getProfileFileName());
                    if (profileFile.exists()) {
                        BinaryFileValidator validator = new BinaryFileValidator(new CannedOutputExternalUtilManager());
                        validator.registerProfile(ResourceType.IMAGE_MASTER_COPY, util, profileFile);
                        assertNotNull(profileFile.getPath(), validator.getProfile(ResourceType.IMAGE_MASTER_COPY, util));
                        count++;
                    }
                }
            }
        }
        assertTrue("profilů: " + count, count >= 280);
    }

    @Test
    public void allFdmfImageProfilesWorkWithStoredOutputs() throws Exception {
        // obrazove profily vsech fDMF nad ulozenymi vystupy: zadne hlaseni o chybe XPath, regularniho vyrazu
        // nebo parsovani (tj. jen vysledky pravidel) a bezchybny vystup Kakadu/ImageMagick projde
        File[] fdmfDirs = ExternalUtilsTestSupport.FDMF_DIR.listFiles(File::isDirectory);
        int checked = 0;
        for (File fdmfDir : fdmfDirs) {
            for (String copy : new String[]{"mc", "uc"}) {
                if (!new File(new File(fdmfDir, "binaryFileProfiles"), copy).isDirectory()) {
                    continue;
                }
                for (String[] utilAndOutput : new String[][]{{"JPYLYZER", "tools_mc.xml"}, {"JHOVE", "tools_mc.xml"}, {"KAKADU", "tools_mc.txt"}, {"IMAGE_MAGICK", "synteticky_ok.txt"}}) {
                    ExternalUtil util = ExternalUtil.valueOf(utilAndOutput[0]);
                    if (!ExternalUtilsTestSupport.profileFile(fdmfDir.getName(), copy, util).exists()) {
                        continue;
                    }
                    CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(util, output(utilDir(util), utilAndOutput[1]));
                    List<String> errors = loadProfile(manager, fdmfDir.getName(), copy, util).validate("jp2k", DUMMY_FILE);
                    for (String error : errors) {
                        assertTrue(fdmfDir.getName() + "/" + copy + "/" + util + ": " + error,
                                error.contains("neodpovídá žádnému z omezení") || error.endsWith("kontrolovaná hodnota nenalezena"));
                    }
                    if (util == ExternalUtil.KAKADU || util == ExternalUtil.IMAGE_MAGICK) {
                        assertEquals(fdmfDir.getName() + "/" + copy + "/" + util, Collections.emptyList(), errors);
                    }
                    checked++;
                }
            }
        }
        assertTrue("zkontrolováno: " + checked, checked >= 200);
    }

    // pomocne metody pro vlastni profily

    private static String validationFirst(String name, String regexp, String rules) {
        return "<validation name=\"" + name + "\"><textDataExtraction resultType=\"STRING\"><firstNonempty><regexp>" + regexp
                + "</regexp></firstNonempty></textDataExtraction><rules>" + rules + "</rules></validation>";
    }

    private static String validationText(String name, String regexp, String rules) {
        return "<validation name=\"" + name + "\"><textDataExtraction resultType=\"STRING_LIST\"><allNonempty><regexp>" + regexp
                + "</regexp></allNonempty></textDataExtraction><rules>" + rules + "</rules></validation>";
    }

    private File writeProfile(String body) throws Exception {
        File file = tmp.newFile();
        String xml = "<binaryFileProfile xmlns=\"http://www.nkp.cz/pspValidator/2.9/binaryFileProfile\" validatorVersion=\"2.9\" dmf=\"test\">" + body + "</binaryFileProfile>";
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static BinaryFileProfile register(ExternalUtil util, File profileFile, String output) throws Exception {
        BinaryFileValidator validator = new BinaryFileValidator(new CannedOutputExternalUtilManager().withOutput(util, output));
        validator.registerProfile(ResourceType.IMAGE_MASTER_COPY, util, profileFile);
        return validator.getProfile(ResourceType.IMAGE_MASTER_COPY, util);
    }
}
