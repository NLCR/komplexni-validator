package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
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

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
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
 * <p>
 * Profily veraPDF a EPUBCheck jsou zatim fixture vep-2.4_emonograph_3.0 (kopie profilu VEP 2.4 s jmennym prostorem 3.0),
 * vystupy veraPDF 1.30 a EPUBCheck 5.4.0.
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
            case VERAPDF:
                return "verapdf";
            case EPUBCHECK:
                return "epubcheck";
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
    public void unexpectedExceptionIsReported() throws Exception {
        // drive "finally { return totalErrors; }" kazdou neocekavanou vyjimku spolkl a soubor prosel bez chyby (nalez 23);
        // dnes je to problem urovne ERROR. Zde null vystup u XML profilu (NPE v XmlUtils.buildDocumentFromString).
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.JPYLYZER, null);
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.JPYLYZER);
        List<Validation.Problem> problems = profile.validateProblems("jp2k", DUMMY_FILE);
        assertEquals(problems.toString(), 1, problems.size());
        assertEquals(Level.ERROR, problems.get(0).getLevel());
        assertTrue(problems.get(0).getMessage(), problems.get(0).getMessage().startsWith("neočekávaná chyba při zpracování výstupu nástroje Jpylyzer: java.lang.NullPointerException"));
    }

    @Test
    public void unexpectedExceptionInConstraintIsReportedAndOtherValidationsContinue() throws Exception {
        // chybne nakonfigurovane omezeni (isInIntRange nad resultType STRING) vyhodi ClassCastException; drive se spolkla
        // a dalsi validace se nevyhodnotily (nalez 23), dnes je to chyba ERROR teto validace a pokracuje se
        File profileFile = writeProfile("<fromText>"
                + validationText("prvni", "(?m)^A.*$", "<mustNotExist/>")
                + validationText("druha", "[0-9]+", "<mustMatchAny><isInIntRange><min>1</min></isInIntRange></mustMatchAny>")
                + validationText("treti", "(?m)^C.*$", "<mustNotExist/>")
                + "</fromText>");
        BinaryFileProfile profile = register(ExternalUtil.KAKADU, profileFile, "A1\n5\nC3");
        List<Validation.Problem> problems = profile.validateProblems("jp2k", DUMMY_FILE);
        assertEquals(problems.toString(), 3, problems.size());
        assertEquals("prvni: zakázaná hodnota nalezena: A1", problems.get(0).toString());
        assertEquals(Level.ERROR, problems.get(1).getLevel());
        assertTrue(problems.get(1).getMessage(), problems.get(1).getMessage().startsWith("druha: neočekávaná chyba při vyhodnocení: java.lang.ClassCastException"));
        assertEquals("treti: zakázaná hodnota nalezena: C3", problems.get(2).toString());
    }

    @Test
    public void toolAndParsingProblemsHaveLevelFromRules() throws Exception {
        // chyba spusteni nastroje, parsovani vystupu a extrakce: uroven null, tedy validation_problem_level z rules.xml
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withFailure(ExternalUtil.VERAPDF, "Cannot run program \"verapdf\": error=2, No such file or directory");
        List<Validation.Problem> problems = loadProfile(manager, PDF_FIXTURE, "pdf", ExternalUtil.VERAPDF).validateProblems("pdf", DUMMY_FILE);
        assertEquals(1, problems.size());
        assertNull(problems.get(0).getLevel());
        manager.withOutput(ExternalUtil.VERAPDF, "toto není xml");
        problems = loadProfile(manager, PDF_FIXTURE, "pdf", ExternalUtil.VERAPDF).validateProblems("pdf", DUMMY_FILE);
        assertEquals(1, problems.size());
        assertNull(problems.get(0).getLevel());
        assertTrue(problems.get(0).getMessage(), problems.get(0).getMessage().startsWith("chyba parsování xml (výstup nástroje VERAPDF)"));
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
        File profileFile = writeProfile("<fromText>" + validationFirst("x", "a", "<mustMatchAny><contains>a</contains></mustMatchAny>") + "</fromText>");
        try {
            new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.KAKADU, profileFile);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("unknown constraint contains", e.getMessage());
        }
    }

    @Test
    public void startsWithAndLevelsInTextProfile() throws Exception {
        File profileFile = writeProfile("<fromText>"
                + validationFirst("verze", "(?m)^verze:.*$", "<mustExist level=\"ERROR\"/><mustMatchAny level=\"WARNING\"><startsWith>verze: 2.</startsWith></mustMatchAny>")
                + validationText("hlasky", "(?m)^INFO.*$", "<mustNotExist level=\"INFO\"/>")
                + validationText("chyby", "(?m)^E.*$", "<mustNotExist/>")
                + "</fromText>");
        assertEquals(Collections.emptyList(), register(ExternalUtil.KAKADU, profileFile, "verze: 2.1").validateProblems("jp2k", DUMMY_FILE));
        assertEquals(Arrays.asList(
                        "WARNING: verze: hodnota \"verze: 3.0\" neodpovídá žádnému z omezení: [musí začínat na 'verze: 2.']",
                        "INFO: hlasky: zakázaná hodnota nalezena: INFO a",
                        "chyby: zakázaná hodnota nalezena: E1"),
                toStrings(register(ExternalUtil.KAKADU, profileFile, "verze: 3.0\nINFO a\nE1").validateProblems("jp2k", DUMMY_FILE)));
        assertEquals(Collections.singletonList("ERROR: verze: kontrolovaná hodnota nenalezena"),
                toStrings(register(ExternalUtil.KAKADU, profileFile, "nic").validateProblems("jp2k", DUMMY_FILE)));
    }

    @Test
    public void invalidLevelFailsOnRegistration() throws Exception {
        // XSD level nepusti (FdmfConfiguration profil validuje), parser to ale hlida i sam
        File profileFile = writeProfile("<fromText>" + validationFirst("x", "a", "<mustExist level=\"FATAL\"/>") + "</fromText>");
        try {
            new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.IMAGE_MASTER_COPY, ExternalUtil.KAKADU, profileFile);
            fail();
        } catch (ValidatorConfigurationException e) {
            assertEquals("neplatná úroveň pravidla mustExist: FATAL", e.getMessage());
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

    // veraPDF 1.30 (fixture profilu vep-2.4_emonograph_3.0/pdf/veraPdf.xml)

    private static final String PDF_FIXTURE = "vep-2.4_emonograph_3.0";
    private static final String R1_WARNING = "WARNING: Soubor PDF/A by měl být v podporované verzi: hodnota \"%s\" neodpovídá žádnému z omezení: "
            + "[musí začínat na 'PDF/A-1A',musí začínat na 'PDF/A-1B',musí začínat na 'PDF/A-2A',musí začínat na 'PDF/A-2B',musí začínat na 'PDF/A-2U']";
    private static final String NOT_COMPLIANT = "Soubor musí být validní: hodnota \"false\" neodpovídá žádnému z omezení: [musí být přesně 'true']";

    private static List<String> problems(ExternalUtil util, String copy, String outputName) throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(util, output(utilDir(util), outputName));
        BinaryFileProfile profile = loadProfile(manager, PDF_FIXTURE, copy, util);
        return toStrings(profile.validateProblems(copy, DUMMY_FILE));
    }

    private static List<String> toStrings(List<Validation.Problem> problems) {
        List<String> result = new ArrayList<>();
        for (Validation.Problem problem : problems) {
            result.add(problem.toString());
        }
        return result;
    }

    @Test
    public void veraPdfCompliantPdfA1bGetsWarningBecauseOfLowercaseLevel() throws Exception {
        // riziko R1 (zafixovane dnesni chovani, oprava profilu je krok P4): veraPDF od 1.2x vypisuje "PDF/A-1b validation
        // profile" s malym pismenem urovne, profil VEP ceka "PDF/A-1B" (startsWith rozlisuje velikost pismen), takze
        // i vyhovujici PDF/A-1b dostane WARNING "nepodporovaná verze"
        assertEquals(Collections.singletonList(String.format(R1_WARNING, "PDF/A-1b validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "compliant_PDFA-1B.xml"));
        assertEquals(Collections.singletonList(String.format(R1_WARNING, "PDF/A-1a validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "compliant_PDFA-1A.xml"));
        assertEquals(Collections.singletonList(String.format(R1_WARNING, "PDF/A-2b validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "compliant_PDFA-2B.xml"));
        assertEquals(Collections.singletonList(String.format(R1_WARNING, "PDF/A-2u validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "compliant_PDFA-2U.xml"));
    }

    @Test
    public void veraPdfCompliantPdfA3bIsUnsupportedVersion() throws Exception {
        // PDF/A-3 profil VEP nepodporuje (zakomentovano), WARNING by byl i po oprave R1
        assertEquals(Collections.singletonList(String.format(R1_WARNING, "PDF/A-3b validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "compliant_PDFA-3B.xml"));
    }

    @Test
    public void veraPdfNonCompliant() throws Exception {
        // bez urovne v profilu (mustMatchAny u isCompliant) plati validation_problem_level z rules.xml
        assertEquals(Arrays.asList(NOT_COMPLIANT, String.format(R1_WARNING, "PDF/A-1b validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "noncompliant_dummy.xml"));
        assertEquals(Arrays.asList(NOT_COMPLIANT, String.format(R1_WARNING, "PDF/A-2b validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "noncompliant_dummy_flavour_2b.xml"));
        assertEquals(Arrays.asList(NOT_COMPLIANT, String.format(R1_WARNING, "PDF/A-3b validation profile")),
                problems(ExternalUtil.VERAPDF, "pdf", "noncompliant_dummy_flavour_3b.xml"));
    }

    @Test
    public void veraPdfEmptyOrUnreadableFile() throws Exception {
        // prazdny nebo necitelny soubor: veraPDF vypise report bez validationReport (taskException PARSE)
        List<String> expected = Arrays.asList(
                "Soubor musí být validní: kontrolovaná hodnota nenalezena",
                "Soubor PDF/A by měl být v podporované verzi: kontrolovaná hodnota nenalezena");
        assertEquals(expected, problems(ExternalUtil.VERAPDF, "pdf", "empty.xml"));
        assertEquals(expected, problems(ExternalUtil.VERAPDF, "pdf", "not_pdf.xml"));
    }

    // EPUBCheck 5.4.0 (fixture profilu vep-2.4_emonograph_3.0/epub/epubCheck.xml)

    private static final String NOT_WELL_FORMED = "Soubor musí být validní: hodnota \"Not well-formed\" neodpovídá žádnému z omezení: [musí být přesně 'Well-formed']";

    @Test
    public void epubCheckWellFormedEpub2() throws Exception {
        assertEquals(Collections.emptyList(), problems(ExternalUtil.EPUBCHECK, "epub", "wellformed_epub2_Sway.xml"));
    }

    @Test
    public void epubCheckNotWellFormedEpub2() throws Exception {
        assertEquals(Arrays.asList(NOT_WELL_FORMED,
                        "WARNING: Detekce chybových hlášek: zakázaná hodnota nalezena: RSC-012, ERROR, [Fragment identifier is not defined.], toc.ncx (18-75)"),
                problems(ExternalUtil.EPUBCHECK, "epub", "notwellformed_epub2_sample1.xml"));
        // vzorek e-monografie 3.0 z official-samples: 48x RSC-005, kazda hlaska samostatny WARNING
        List<String> emono = problems(ExternalUtil.EPUBCHECK, "epub", "notwellformed_epub2_emono30.xml");
        assertEquals(49, emono.size());
        assertEquals(NOT_WELL_FORMED, emono.get(0));
        for (String problem : emono.subList(1, emono.size())) {
            assertTrue(problem, problem.startsWith("WARNING: Detekce chybových hlášek: zakázaná hodnota nalezena: RSC-005, ERROR, "));
        }
    }

    @Test
    public void epubCheckEmptyOrUnreadableFile() throws Exception {
        // necitelny EPUB nema repInfo/version; mustExist bez urovne -> uroven z rules.xml (nalez 28: null se k isExactly
        // nedostane, zastavi ho mustExist; kdyby se dostal, isExactly vrati false misto NullPointerException)
        for (String output : new String[]{"empty.xml", "not_epub.xml"}) {
            List<String> problems = problems(ExternalUtil.EPUBCHECK, "epub", output);
            assertEquals(output + ": " + problems, 4, problems.size());
            assertEquals(NOT_WELL_FORMED, problems.get(0));
            assertEquals("Soubor Epub by měl být v podporované verzi: kontrolovaná hodnota nenalezena", problems.get(1));
            assertTrue(problems.get(2), problems.get(2).startsWith("WARNING: Detekce chybových hlášek: zakázaná hodnota nalezena: PKG-008, FATAL, "));
            assertTrue(problems.get(3), problems.get(3).startsWith("WARNING: Detekce chybových hlášek: zakázaná hodnota nalezena: PKG-003, ERROR, "));
        }
    }

    @Test
    public void epubCheckEpub3VersionAndMediaTypes() throws Exception {
        // syntetický EPUB 3.4 s fontem font/woff: verze neni 2.0.1 a font/woff neni v seznamu (profil ma
        // "application/font‐wof" s U+2010 a preklepem, R3), kazdy nepovoleny typ media je samostatny WARNING
        assertEquals(Arrays.asList(
                        "WARNING: Soubor Epub by měl být v podporované verzi: hodnota \"3.4\" neodpovídá žádnému z omezení: [musí být přesně '2.0.1']",
                        "WARNING: Kontrola typů obsažených objektů (Epub 3): hodnota \"font/woff\" neodpovídá žádnému z omezení: ["
                                + "musí být přesně 'text/css',musí být přesně 'image/gif',musí být přesně 'image/jpeg',musí být přesně 'image/png',"
                                + "musí být přesně 'image/svg+xml',musí být přesně 'application/xhtml+xml',musí být přesně 'application/xml',"
                                + "musí být přesně 'application/x-dtbncx+xml',musí být přesně 'application/vnd.ms-opentype',"
                                + "musí být přesně 'application/font‐wof',musí být přesně 'application/smil+xml',musí být přesně 'application/pls+xml',"
                                + "musí být přesně 'audio/mpeg',musí být přesně 'audio/mp4']"),
                problems(ExternalUtil.EPUBCHECK, "epub", "wellformed_epub3_synteticky.xml"));
    }

    @Test
    public void epubCheckDrm() throws Exception {
        // syntetický vystup s RSC-004 (EPUBCheck 5.4.0 ma RSC-004 jako INFO a do XML ho nevypise, viz README.md)
        assertEquals(Arrays.asList(
                        "ERROR: Kontrola na nepřítomnost DRM: zakázaná hodnota nalezena: RSC-004, ERROR, [File \"OEBPS/c1.xhtml\" could not be decrypted.], Sway.epub",
                        "WARNING: Detekce chybových hlášek: zakázaná hodnota nalezena: RSC-004, ERROR, [File \"OEBPS/c1.xhtml\" could not be decrypted.], Sway.epub"),
                problems(ExternalUtil.EPUBCHECK, "epub", "synteticky_drm_RSC-004.xml"));
    }

    @Test
    public void ePublicationFixtureProfilesAreValidByXsd() throws Exception {
        // FdmfConfiguration validuje profily proti binaryFileProfile.xsd (level, startsWith); fixture musi projit stejne
        javax.xml.validation.Validator xsdValidator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new File(ExternalUtilsTestSupport.VALIDATOR_CONFIG_DIR, "xsd/binaryFileProfile.xsd")).newValidator();
        for (String[] copyAndUtil : new String[][]{{"pdf", "VERAPDF"}, {"epub", "EPUBCHECK"}}) {
            xsdValidator.validate(new StreamSource(ExternalUtilsTestSupport.profileFile(PDF_FIXTURE, copyAndUtil[0], ExternalUtil.valueOf(copyAndUtil[1]))));
        }
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
        String xml = "<binaryFileProfile xmlns=\"http://www.nkp.cz/pspValidator/3.0/binaryFileProfile\" validatorVersion=\"3.0\" dmf=\"test\">" + body + "</binaryFileProfile>";
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static BinaryFileProfile register(ExternalUtil util, File profileFile, String output) throws Exception {
        BinaryFileValidator validator = new BinaryFileValidator(new CannedOutputExternalUtilManager().withOutput(util, output));
        validator.registerProfile(ResourceType.IMAGE_MASTER_COPY, util, profileFile);
        return validator.getProfile(ResourceType.IMAGE_MASTER_COPY, util);
    }
}
