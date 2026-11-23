package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.externalUtils.*;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileValidator;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Validacni funkce checkBinaryFilesValidByExternalUtil.
 * <p>
 * Cast bez nastroju (vzdy bezi): stavy "nástroj vypnutý / není dostupný / není definováno spuštění", chybejici profil,
 * prazdny seznam souboru, kontrakt a prevod chyb profilu na ValidationProblem nad podstrcenym vystupem.
 * <p>
 * Cast se skutecnymi nastroji (profily fDMF fund_unit_0.2) nad src/test/resources/jpeg2000/*: nastroj se hleda
 * v PATH, nebo v adresari ze systemove vlastnosti (jpylyzer.path, jhove.path, kakadu.path, imagemagick.path),
 * napr. {@code ./gradlew :modules:sharedModule:test --tests '*VfCheckBinaryFilesValidByExternalUtilTest' -Djhove.path=/opt/jhove}.
 * Dostupnost se overi detekci verze jako v aplikaci; kdyz nastroj chybi, je test preskoceny (Assume), ne zeleny.
 * <p>
 * veraPDF a EPUBCheck (profily fDMF emonograph_3.0) nad
 * src/test/resources/epublikace: veraPDF z PATH nebo -Dverapdf.path, EPUBCheck jen s -Depubcheck.path (adresar
 * s epubcheck.jar, spousti se "java -jar"), napr. -Dverapdf.path=$(brew --prefix)/bin -Depubcheck.path=$(brew --prefix epubcheck)/libexec.
 * Gradle predava systemove vlastnosti *.path testum (build.gradle sharedModule).
 * <p>
 * Ocekavane vysledky jsou overene s Jpylyzer 2.2.0, JHOVE 1.29.0 a Kakadu 8.5 (8. 11. 2026), veraPDF 1.30
 * a EPUBCheck 5.4.0 (12. 11. 2026).
 */
public class VfCheckBinaryFilesValidByExternalUtilTest {

    private static final File EXAMPLES_ROOT_DIR = new File("src/test/resources/jpeg2000");
    private static final File TOOLS_JP2_DIR = new File("src/test/tools/jp2");
    private static final File EPUBLIKACE_DIR = new File("src/test/resources/epublikace");
    private static final String EPUB_PROFILES_FIXTURE = "emonograph_3.0";
    private static final String PDFA_CONSTRAINTS = "[musí začínat na 'PDF/A-1A',musí začínat na 'PDF/A-1a',musí začínat na 'PDF/A-1B',"
            + "musí začínat na 'PDF/A-1b',musí začínat na 'PDF/A-2A',musí začínat na 'PDF/A-2a',musí začínat na 'PDF/A-2B',"
            + "musí začínat na 'PDF/A-2b',musí začínat na 'PDF/A-2U',musí začínat na 'PDF/A-2u']";
    private static final Dmf DMF = new Dmf(Dmf.Type.FUND_UNIT, "0.2");

    private static final Map<ExternalUtil, String> PATH_PROPERTIES = new EnumMap<>(ExternalUtil.class);

    static {
        PATH_PROPERTIES.put(ExternalUtil.JPYLYZER, "jpylyzer.path");
        PATH_PROPERTIES.put(ExternalUtil.JHOVE, "jhove.path");
        PATH_PROPERTIES.put(ExternalUtil.KAKADU, "kakadu.path");
        PATH_PROPERTIES.put(ExternalUtil.IMAGE_MAGICK, "imagemagick.path");
        PATH_PROPERTIES.put(ExternalUtil.VERAPDF, "verapdf.path");
        PATH_PROPERTIES.put(ExternalUtil.EPUBCHECK, "epubcheck.path");
    }

    private static final Map<ExternalUtil, String> versions = new EnumMap<>(ExternalUtil.class);
    private static final Map<ExternalUtil, String> unavailableReasons = new EnumMap<>(ExternalUtil.class);
    private static Engine engine;
    private static Engine ePublicationEngine;
    private static ExternalUtilManager externalUtilManager;
    private static String imageMagickJp2Problem;

    @BeforeClass
    public static void setup() throws Exception {
        externalUtilManager = new ExternalUtilManagerFactory(ExternalUtilsTestSupport.EXTERNAL_UTILS_CONFIG)
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            String pathProperty = PATH_PROPERTIES.get(util);
            if (pathProperty == null) {
                // zvukove nastroje se tu netestuji
                externalUtilManager.setUtilAvailable(util, false);
                continue;
            }
            String path = System.getProperty(pathProperty);
            if (path != null && !path.trim().isEmpty()) {
                externalUtilManager.setPath(util, new File(path.trim()));
            }
            try {
                String version = externalUtilManager.runUtilVersionDetection(util);
                versions.put(util, version);
                externalUtilManager.setUtilAvailable(util, true);
            } catch (CliCommand.CliCommandException e) {
                unavailableReasons.put(util, e.getMessage());
                externalUtilManager.setUtilAvailable(util, false);
            }
        }
        System.out.println("VfCheckBinaryFilesValidByExternalUtilTest, dostupné nástroje: " + versions + ", nedostupné: " + unavailableReasons.keySet());

        ValidatorConfigurationManager configManager = new ValidatorConfigurationManager(ExternalUtilsTestSupport.VALIDATOR_CONFIG_DIR);
        FdmfConfiguration fdmfConfig = new FdmfRegistry(configManager).getFdmfConfig(DMF);
        fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        engine = new Engine(fdmfConfig.getBinaryFileValidator());

        BinaryFileValidator ePublicationValidator = new BinaryFileValidator(externalUtilManager);
        ePublicationValidator.registerProfile(ResourceType.ORIGINAL_COPY_PDF, ExternalUtil.VERAPDF,
                ExternalUtilsTestSupport.profileFile(EPUB_PROFILES_FIXTURE, "pdf", ExternalUtil.VERAPDF));
        ePublicationValidator.registerProfile(ResourceType.ORIGINAL_COPY_EPUB, ExternalUtil.EPUBCHECK,
                ExternalUtilsTestSupport.profileFile(EPUB_PROFILES_FIXTURE, "epub", ExternalUtil.EPUBCHECK));
        ePublicationEngine = new Engine(ePublicationValidator);

        if (externalUtilManager.isUtilAvailable(ExternalUtil.IMAGE_MAGICK)) {
            // ImageMagick bez JP2 delegata (napr. Homebrew bez OpenJPEG) hlasi u kazdeho JP2 "no decode delegate"
            String out = externalUtilManager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.IMAGE_MAGICK), new File(TOOLS_JP2_DIR, "mc.jp2"));
            if (out.contains("no decode delegate")) {
                imageMagickJp2Problem = out;
            }
        }
    }

    private static void assumeAvailable(ExternalUtil util) {
        Assume.assumeTrue(util.getUserFriendlyName() + " není dostupný (" + unavailableReasons.get(util) + "), nastav PATH nebo -D" + PATH_PROPERTIES.get(util),
                externalUtilManager.isUtilAvailable(util));
    }

    private static List<File> filesInDir(String dirName) {
        File[] array = new File(EXAMPLES_ROOT_DIR, dirName).listFiles(File::isFile);
        assertNotNull(dirName, array);
        Arrays.sort(array);
        return Arrays.asList(array);
    }

    private static File file(String dirName, String fileName) {
        File file = new File(new File(EXAMPLES_ROOT_DIR, dirName), fileName);
        assertTrue(file.getPath(), file.isFile());
        return file;
    }

    private static ValidationFunction buildValidationFunction(Engine engine, ExternalUtil util, ResourceType type, List<File> files) {
        return buildValidationFunction(engine, util, type, files, "jp2k", Level.WARNING);
    }

    private static ValidationFunction buildValidationFunction(Engine engine, ExternalUtil util, ResourceType type, List<File> files,
                                                              String execution, Level validationProblemLevel) {
        ValidationFunction function = new VfCheckBinaryFilesValidByExternalUtil("test", engine)
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_FILES, ValueType.FILE_LIST, new ValueEvaluation(files));
        if (validationProblemLevel != null) {
            function.withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_VALIDATION_PROBLEM_LEVEL, ValueType.LEVEL, new ValueEvaluation(validationProblemLevel));
        }
        return function
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_TYPE, ValueType.RESOURCE_TYPE, new ValueEvaluation(type))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_UTIL, ValueType.EXTERNAL_UTIL, new ValueEvaluation(util))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_EXECUTION, ValueType.STRING, new ValueEvaluation(execution));
    }

    private static ValidationResult validate(ExternalUtil util, ResourceType type, List<File> files) {
        return buildValidationFunction(engine, util, type, files).validate();
    }

    /**
     * Problemy podle souboru (nazev souboru -> zpravy).
     */
    private static Map<String, List<String>> problemsByFile(ValidationResult result) {
        Map<String, List<String>> map = new TreeMap<>();
        for (ValidationProblem problem : result.getProblems()) {
            assertEquals(problem.getFullMessage(), Level.WARNING, problem.getLevel());
            assertNotNull(problem.getFullMessage(), problem.getReferencedFile());
            map.computeIfAbsent(problem.getReferencedFile().getName(), k -> new ArrayList<>()).add(problem.getFullMessage());
        }
        return map;
    }

    private static void assertNoProblems(ValidationResult result) {
        assertEquals(Collections.emptyMap(), problemsByFile(result));
    }

    private static void assertEveryFileHasProblem(List<File> files, ValidationResult result, String expectedMessagePrefix) {
        Map<String, List<String>> problems = problemsByFile(result);
        for (File file : files) {
            List<String> fileProblems = problems.get(file.getName());
            assertNotNull("bez problému: " + file.getName(), fileProblems);
            if (expectedMessagePrefix != null) {
                assertTrue(file.getName() + ": " + fileProblems, fileProblems.stream().anyMatch(p -> p.startsWith(expectedMessagePrefix)));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // bez nastroju

    private static Engine engineWith(ExternalUtilManager manager, ExternalUtil util, ResourceType type, File profile) throws Exception {
        BinaryFileValidator validator = new BinaryFileValidator(manager);
        if (profile != null) {
            validator.registerProfile(type, util, profile);
        }
        return new Engine(validator);
    }

    @Test
    public void cannedOutputProblemsArePerFileWithLevelAndToolName() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.KAKADU, ExternalUtilsTestSupport.output("kakadu", "detekce_poskozeni_w0352gUC.txt"));
        Engine engine = engineWith(manager, ExternalUtil.KAKADU, ResourceType.IMAGE_USER_COPY,
                ExternalUtilsTestSupport.profileFile("fund_unit_0.2", "uc", ExternalUtil.KAKADU));
        List<File> files = Arrays.asList(new File("a.jp2"), new File("b.jp2"));
        ValidationResult result = buildValidationFunction(engine, ExternalUtil.KAKADU, ResourceType.IMAGE_USER_COPY, files).validate();
        assertEquals(2, result.getProblems().size());
        for (int i = 0; i < 2; i++) {
            ValidationProblem problem = result.getProblems().get(i);
            assertEquals(Level.WARNING, problem.getLevel());
            assertEquals(files.get(i), problem.getReferencedFile());
            assertEquals("Kakadu", problem.getToolName());
            assertTrue(problem.getFullMessage(), problem.getFullMessage().startsWith("no_errors: zakázaná hodnota nalezena: Kakadu Core Error:"));
            assertEquals(problem.getFullMessage(), problem.getSimpleMessage());
        }
    }

    private static final String KAKADU_CANNOT_OPEN = "soubor nelze otevřít (neexistuje, není čitelný, nebo na Windows cesta obsahuje "
            + "znak mimo znakovou sadu systému): zakázaná hodnota nalezena: Error in Kakadu File Format Support:\nUnable to open input file,";

    @Test
    public void cannedKakaduCannotOpenFileIsErrorEvenWithWarningLevel() throws Exception {
        // N2: soubor, ktery Kakadu neotevre, nesmi projit; pravidlo ma uroven WARNING, tahle chyba je vzdy ERROR
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.KAKADU, ExternalUtilsTestSupport.output("kakadu", "nelze_otevrit_windows_znakova_sada.txt"));
        Engine engine = engineWith(manager, ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY,
                ExternalUtilsTestSupport.profileFile("fund_unit_0.2", "mc", ExternalUtil.KAKADU));
        File file = new File("nevalidni/隨機中國文字.jp2");
        ValidationResult result = buildValidationFunction(engine, ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY,
                Collections.singletonList(file)).validate();
        assertEquals(result.getProblems().toString(), 1, result.getProblems().size());
        ValidationProblem problem = result.getProblems().get(0);
        assertEquals(Level.ERROR, problem.getLevel());
        assertEquals(file, problem.getReferencedFile());
        assertEquals("Kakadu", problem.getToolName());
        assertEquals(KAKADU_CANNOT_OPEN + "\n\"src\\test\\resources\\jpeg2000\\nevalidni\\??????.jp2\".", problem.getFullMessage());
    }

    @Test
    public void cannedOutputWithoutProblems() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.JHOVE, ExternalUtilsTestSupport.output("jhove", "tools_mc.xml"));
        Engine engine = engineWith(manager, ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY,
                ExternalUtilsTestSupport.profileFile("fund_unit_0.2", "mc", ExternalUtil.JHOVE));
        assertFalse(buildValidationFunction(engine, ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY,
                Collections.singletonList(new File(TOOLS_JP2_DIR, "mc.jp2"))).validate().hasProblems());
    }

    @Test
    public void utilDisabled() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.KAKADU, "");
        manager.setUtilDisabled(ExternalUtil.KAKADU);
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, null),
                ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File("a.jp2"))).validate();
        assertSingleProblem(result, Level.INFO, "nástroj Kakadu je vypnutý");
    }

    @Test
    public void utilsDisabledAsSetGiveSameInfoAsSingleDisable() throws Exception {
        //GUI vypina nastroje pro jednu validaci pres setUtilsDisabled, hlaska musi byt stejna jako po --disable-... v CLI
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.KAKADU, "");
        manager.setUtilsDisabled(EnumSet.of(ExternalUtil.KAKADU, ExternalUtil.JHOVE));
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, null),
                ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File("a.jp2"))).validate();
        assertSingleProblem(result, Level.INFO, "nástroj Kakadu je vypnutý");
    }

    @Test
    public void utilsDisabledSetIsReplacedNotAccumulated() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager();
        manager.setUtilsDisabled(EnumSet.of(ExternalUtil.KAKADU, ExternalUtil.JHOVE));
        assertTrue(manager.isUtilDisabled(ExternalUtil.KAKADU));
        assertTrue(manager.isUtilDisabled(ExternalUtil.JHOVE));
        manager.setUtilsDisabled(EnumSet.of(ExternalUtil.JHOVE));
        assertFalse(manager.isUtilDisabled(ExternalUtil.KAKADU));
        assertTrue(manager.isUtilDisabled(ExternalUtil.JHOVE));
        manager.setUtilsDisabled(null);
        for (ExternalUtil util : ExternalUtil.values()) {
            assertFalse(util.name(), manager.isUtilDisabled(util));
        }
    }

    @Test
    public void utilNotAvailable() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.JHOVE, "");
        manager.setUtilAvailable(ExternalUtil.JHOVE, false);
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, null),
                ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File("a.jp2"))).validate();
        assertSingleProblem(result, Level.INFO, "nástroj JHOVE není dostupný");
    }

    @Test
    public void executionNotDefined() throws Exception {
        // CannedOutputExternalUtilManager ma spusteni definovane jen pro nastroje s nastavenym vystupem
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager();
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, null),
                ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File("a.jp2"))).validate();
        assertSingleProblem(result, Level.INFO, "pro nástroj Jpylyzer není definováno spuštění 'jp2k'");
    }

    @Test
    public void profileMissing() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.JPYLYZER, "");
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, null),
                ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File("a.jp2"))).validate();
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
        assertTrue(result.getProblems().get(0).getFullMessage().startsWith("nenalezen profil binárního souboru pro typ IMAGE_MASTER_COPY a nástroj "));
    }

    @Test
    public void emptyFileList() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.KAKADU, "");
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, null),
                ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, Collections.<File>emptyList()).validate();
        assertSingleProblem(result, Level.ERROR, "prázdný seznam souborů");
    }

    @Test
    public void missingExecutionParamFailsContract() throws Exception {
        // puvodni verze tohoto testu parametr "execution" nezadavala, takze kazdy test "assertTrue(result.hasProblems())"
        // prosel diky nesplnenemu kontraktu, ne diky nalezene vade souboru
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.KAKADU, "");
        ValidationResult result = new VfCheckBinaryFilesValidByExternalUtil("test", engineWith(manager, ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, null))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_FILES, ValueType.FILE_LIST, new ValueEvaluation(Collections.singletonList(new File("a.jp2"))))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_TYPE, ValueType.RESOURCE_TYPE, new ValueEvaluation(ResourceType.IMAGE_MASTER_COPY))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_UTIL, ValueType.EXTERNAL_UTIL, new ValueEvaluation(ExternalUtil.KAKADU))
                .validate();
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).getFullMessage(), result.getProblems().get(0).getFullMessage().startsWith("nesplněn kontrakt validační funkce"));
    }

    // urovne z profilu a ignore_empty_file_list (pravidla VEP pro e-publikace)

    private static Engine ePublicationEngineWith(ExternalUtilManager manager) throws Exception {
        BinaryFileValidator validator = new BinaryFileValidator(manager);
        validator.registerProfile(ResourceType.ORIGINAL_COPY_PDF, ExternalUtil.VERAPDF,
                ExternalUtilsTestSupport.profileFile(EPUB_PROFILES_FIXTURE, "pdf", ExternalUtil.VERAPDF));
        validator.registerProfile(ResourceType.ORIGINAL_COPY_EPUB, ExternalUtil.EPUBCHECK,
                ExternalUtilsTestSupport.profileFile(EPUB_PROFILES_FIXTURE, "epub", ExternalUtil.EPUBCHECK));
        return new Engine(validator);
    }

    private static List<String> levelsAndMessages(ValidationResult result) {
        List<String> list = new ArrayList<>();
        for (ValidationProblem problem : result.getProblems()) {
            list.add(problem.getLevel() + ": " + problem.getFullMessage());
        }
        return list;
    }

    @Test
    public void levelFromProfileOverridesValidationProblemLevel() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.VERAPDF, ExternalUtilsTestSupport.output("verapdf", "noncompliant_dummy_flavour_3b.xml"));
        List<File> files = Collections.singletonList(new File("dummy.pdf"));
        // rules.xml VEP validation_problem_level nezadava (ERROR); "PDF/A v podporované verzi" ma v profilu level="WARNING"
        for (Level rulesLevel : new Level[]{null, Level.ERROR, Level.INFO}) {
            ValidationResult result = buildValidationFunction(ePublicationEngineWith(manager), ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF,
                    files, "pdf", rulesLevel).validate();
            Level expected = rulesLevel == null ? Level.ERROR : rulesLevel;
            assertEquals(Arrays.asList(
                    expected + ": Soubor musí být validní: hodnota \"false\" neodpovídá žádnému z omezení: [musí být přesně 'true']",
                    "WARNING: Soubor PDF/A by měl být v podporované verzi: hodnota \"PDF/A-3b validation profile\" neodpovídá žádnému z omezení: "
                            + PDFA_CONSTRAINTS
            ), levelsAndMessages(result));
            for (ValidationProblem problem : result.getProblems()) {
                assertEquals("veraPDF", problem.getToolName());
                assertEquals(files.get(0), problem.getReferencedFile());
            }
        }
    }

    // neoverena verze nastroje: jedna INFO na pravidlo, ostatni vysledky beze zmeny

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String VERAPDF_INVALID = "ERROR: Soubor musí být validní: hodnota \"false\" neodpovídá žádnému z omezení: [musí být přesně 'true']";
    private static final String VERAPDF_FLAVOUR = "WARNING: Soubor PDF/A by měl být v podporované verzi: hodnota \"PDF/A-3b validation profile\" "
            + "neodpovídá žádnému z omezení: " + PDFA_CONSTRAINTS;
    private static final List<File> TWO_PDFS = Arrays.asList(new File("a.pdf"), new File("b.pdf"));

    private static Map<ExternalUtil, VerifiedVersions> veraPdfVerified(OperatingSystem inheritedFrom) {
        Map<ExternalUtil, VerifiedVersions> map = new EnumMap<>(ExternalUtil.class);
        map.put(ExternalUtil.VERAPDF, new VerifiedVersions(Collections.singletonList(new VerifiedVersions.Series("1.30", "1.30.2")), inheritedFrom));
        return map;
    }

    private static CannedOutputExternalUtilManager veraPdfManager(Map<ExternalUtil, VerifiedVersions> verified, String output) throws Exception {
        CannedOutputExternalUtilManager manager = verified == null ? new CannedOutputExternalUtilManager() : new CannedOutputExternalUtilManager(verified);
        return manager.withOutput(ExternalUtil.VERAPDF, ExternalUtilsTestSupport.output("verapdf", output));
    }

    private static ValidationResult validateTwoPdfs(ExternalUtilManager manager) throws Exception {
        return buildValidationFunction(ePublicationEngineWith(manager), ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF,
                TWO_PDFS, "pdf", null).validate();
    }

    /** Úroveň a text každého problému s názvem souboru (nebo „-“ bez souboru). */
    private static List<String> levelsMessagesAndFiles(ValidationResult result) {
        List<String> list = new ArrayList<>();
        for (ValidationProblem problem : result.getProblems()) {
            list.add((problem.getReferencedFile() == null ? "-" : problem.getReferencedFile().getName()) + " "
                    + problem.getLevel() + ": " + problem.getFullMessage());
        }
        return list;
    }

    @Test
    public void unverifiedVersionGivesOneInfoPerRuleAndKeepsResults() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(null), "noncompliant_dummy_flavour_3b.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.31.0");
        ValidationResult result = validateTwoPdfs(manager);
        assertEquals(Arrays.asList(
                "- INFO: nástroj veraPDF je ve verzi 1.31.0, která nebyla s validátorem ověřena (ověřená řada: 1.30); výsledky kontroly se mohou lišit",
                "a.pdf " + VERAPDF_INVALID,
                "a.pdf " + VERAPDF_FLAVOUR,
                "b.pdf " + VERAPDF_INVALID,
                "b.pdf " + VERAPDF_FLAVOUR
        ), levelsMessagesAndFiles(result));
        ValidationProblem info = result.getProblems().get(0);
        assertEquals("veraPDF", info.getToolName());
        assertEquals("neověřená verze nástroje", info.getSimpleMessage());
        assertEquals("1.31.0", info.getActualValue());
        assertEquals("ověřená řada: 1.30", info.getValueSpec());
        assertNull(info.getReferencedFile());
    }

    @Test
    public void verifiedVersionGivesNoInfo() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(null), "noncompliant_dummy_flavour_3b.xml");
        // Homebrew veraPDF 1.30.2 vypise 1.30.0; Windows 1.30.4
        for (String version : new String[]{"veraPDF 1.30.0", "veraPDF 1.30.4"}) {
            manager.setUtilVersion(ExternalUtil.VERAPDF, version);
            assertEquals(version, Arrays.asList(
                    "a.pdf " + VERAPDF_INVALID,
                    "a.pdf " + VERAPDF_FLAVOUR,
                    "b.pdf " + VERAPDF_INVALID,
                    "b.pdf " + VERAPDF_FLAVOUR
            ), levelsMessagesAndFiles(validateTwoPdfs(manager)));
        }
    }

    @Test
    public void versionVerifiedOnOtherPlatformGivesNoInfo() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(OperatingSystem.MAC), "compliant_PDFA-2B.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.30.0");
        assertEquals(Collections.emptyList(), levelsMessagesAndFiles(validateTwoPdfs(manager)));
    }

    @Test
    public void unverifiedOnInheritingPlatformSaysWhereVerified() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(OperatingSystem.MAC), "compliant_PDFA-2B.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.28.1");
        assertEquals(Collections.singletonList(
                "- INFO: nástroj veraPDF je ve verzi 1.28.1, která nebyla s validátorem ověřena (ověřená řada na macOS: 1.30); výsledky kontroly se mohou lišit"
        ), levelsMessagesAndFiles(validateTwoPdfs(manager)));
    }

    @Test
    public void unrecognizedVersionGivesOneInfo() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(null), "compliant_PDFA-2B.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF (vývojová verze)");
        ValidationResult result = validateTwoPdfs(manager);
        assertEquals(Collections.singletonList(
                "- INFO: verzi nástroje veraPDF nelze rozpoznat z výstupu „veraPDF (vývojová verze)“ (ověřená řada: 1.30); výsledky kontroly se mohou lišit"
        ), levelsMessagesAndFiles(result));
        assertEquals("verzi nástroje nelze rozpoznat", result.getProblems().get(0).getSimpleMessage());
        assertEquals("veraPDF (vývojová verze)", result.getProblems().get(0).getActualValue());
        assertEquals("ověřená řada: 1.30", result.getProblems().get(0).getValueSpec());
    }

    @Test
    public void configWithoutListGivesNoInfo() throws Exception {
        // starsi externalUtils.xml bez verifiedVersions: nic se neoznacuje, ani nerozpoznana verze
        CannedOutputExternalUtilManager manager = veraPdfManager(null, "compliant_PDFA-2B.xml");
        for (String version : new String[]{"veraPDF 1.31.0", "veraPDF (vývojová verze)"}) {
            manager.setUtilVersion(ExternalUtil.VERAPDF, version);
            assertEquals(version, Collections.emptyList(), levelsMessagesAndFiles(validateTwoPdfs(manager)));
        }
    }

    @Test
    public void versionNotRecordedGivesNoInfo() throws Exception {
        // verzi zapisuje CLI a GUI po detekci; bez ni (napr. testy, vlastni pouziti jadra) se nic neoznacuje
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(null), "compliant_PDFA-2B.xml");
        assertEquals(Collections.emptyList(), levelsMessagesAndFiles(validateTwoPdfs(manager)));
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.31.0");
        manager.setUtilVersion(ExternalUtil.VERAPDF, null);
        assertEquals(Collections.emptyList(), levelsMessagesAndFiles(validateTwoPdfs(manager)));
    }

    @Test
    public void unverifiedVersionOfOtherToolGivesNoInfo() throws Exception {
        Map<ExternalUtil, VerifiedVersions> verified = veraPdfVerified(null);
        verified.put(ExternalUtil.EPUBCHECK, new VerifiedVersions(Collections.singletonList(new VerifiedVersions.Series("5.4", "5.4.0")), null));
        CannedOutputExternalUtilManager manager = veraPdfManager(verified, "compliant_PDFA-2B.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.30.4");
        manager.setUtilVersion(ExternalUtil.EPUBCHECK, "EPUBCheck v5.5.0");
        assertEquals(Collections.emptyList(), levelsMessagesAndFiles(validateTwoPdfs(manager)));
    }

    @Test
    public void unavailableDisabledAndEmptyListWinOverVersion() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(null), "compliant_PDFA-2B.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.31.0");

        manager.setUtilAvailable(ExternalUtil.VERAPDF, false);
        assertEquals(Collections.singletonList("- INFO: nástroj veraPDF není dostupný"), levelsMessagesAndFiles(validateTwoPdfs(manager)));
        manager.setUtilAvailable(ExternalUtil.VERAPDF, true);

        manager.setUtilDisabled(ExternalUtil.VERAPDF);
        assertEquals(Collections.singletonList("- INFO: nástroj veraPDF je vypnutý"), levelsMessagesAndFiles(validateTwoPdfs(manager)));
        manager.setUtilsDisabled(null);

        // prazdny seznam souboru: nastroj se nepouzije, o verzi se nic nehlasi
        Engine engine = ePublicationEngineWith(manager);
        assertEquals(Collections.singletonList("- ERROR: prázdný seznam souborů"), levelsMessagesAndFiles(buildValidationFunction(engine,
                ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF, Collections.<File>emptyList(), "pdf", null).validate()));
        assertEquals(Collections.emptyList(), levelsMessagesAndFiles(withIgnoreEmptyFileList(buildValidationFunction(engine,
                ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF, Collections.<File>emptyList(), "pdf", null), true).validate()));
    }

    @Test
    public void missingProfileWinsOverVersion() throws Exception {
        CannedOutputExternalUtilManager manager = veraPdfManager(veraPdfVerified(null), "compliant_PDFA-2B.xml");
        manager.setUtilVersion(ExternalUtil.VERAPDF, "veraPDF 1.31.0");
        ValidationResult result = buildValidationFunction(engineWith(manager, ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF, null),
                ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF, TWO_PDFS, "pdf", null).validate();
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
        assertTrue(result.getProblems().get(0).getFullMessage().startsWith("nenalezen profil binárního souboru"));
    }

    /**
     * Skutečná konfigurace a falešný JHOVE ({@link FakeExternalUtils}, na Windows dávka): detekce verze jako v CLI
     * (zápis verze do manažeru), pak validace dvou JP2 profilem fund_unit_0.2/mc nad výstupem s nálezy. Neověřená
     * verze přidá právě jednu INFO před nálezy, ověřená žádnou; nálezy jsou ve všech
     * případech stejné jako bez zapsané verze.
     */
    @Test
    public void unverifiedJhoveWithFakeToolOnEveryOsConfig() throws Exception {
        File config = FakeExternalUtils.hostConfig(ExternalUtilsTestSupport.EXTERNAL_UTILS_CONFIG, tmp.getRoot());
        List<File> files = Arrays.asList(new File("x.jp2"), new File("y.jp2"));
        String unverifiedInfo = "- INFO: nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady: 1.29, 1.34); výsledky kontroly se mohou lišit";
        String unverifiedInfoWindows = "- INFO: nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřená řada: 1.34); výsledky kontroly se mohou lišit";
        String unverifiedInfoLinux = "- INFO: nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady: 1.28, 1.34); výsledky kontroly se mohou lišit";
        Map<OperatingSystem, String> expectedInfo = new EnumMap<>(OperatingSystem.class);
        expectedInfo.put(OperatingSystem.MAC, unverifiedInfo);
        expectedInfo.put(OperatingSystem.WINDOWS, unverifiedInfoWindows);
        expectedInfo.put(OperatingSystem.LINUX, unverifiedInfoLinux);
        for (OperatingSystem os : OperatingSystem.values()) {
            String program = os == OperatingSystem.WINDOWS ? "jhove.bat" : "jhove";
            File dir = tmp.newFolder("jhove-" + os.name());
            ExternalUtilManager manager = new ExternalUtilManagerFactory(config).buildExternalUtilManager(os);
            manager.setPath(ExternalUtil.JHOVE, dir);
            Engine engine = engineWith(manager, ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY,
                    ExternalUtilsTestSupport.profileFile("fund_unit_0.2", "mc", ExternalUtil.JHOVE));
            FakeExternalUtils.create(dir, program, ExternalUtilsTestSupport.outputFile("jhove", "odlis_od_prof_mc_LRCP.xml"), null, 0);
            List<String> baseline = levelsMessagesAndFiles(buildValidationFunction(engine, ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, files).validate());
            assertFalse(os.name(), baseline.isEmpty());
            for (String line : baseline) {
                assertTrue(os + ": " + line, line.startsWith("x.jp2 WARNING: ") || line.startsWith("y.jp2 WARNING: "));
            }

            for (String version : new String[]{"1.34.0", "1.36.0"}) {
                File versionOut = tmp.newFile();
                Files.write(versionOut.toPath(), ("Jhove (Rel. " + version + ", 2026-01-15)\n").getBytes(StandardCharsets.UTF_8));
                FakeExternalUtils.create(dir, program, versionOut, null, 0);
                manager.setUtilVersion(ExternalUtil.JHOVE, manager.runUtilVersionDetection(ExternalUtil.JHOVE));
                FakeExternalUtils.create(dir, program, ExternalUtilsTestSupport.outputFile("jhove", "odlis_od_prof_mc_LRCP.xml"), null, 0);
                List<String> actual = levelsMessagesAndFiles(buildValidationFunction(engine, ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, files).validate());
                List<String> expected = new ArrayList<>();
                if (version.equals("1.36.0")) {
                    expected.add(expectedInfo.get(os));
                }
                expected.addAll(baseline);
                assertEquals(os + " " + version, expected, actual);
            }
        }
    }

    private static ValidationFunction withIgnoreEmptyFileList(ValidationFunction function, Boolean ignore) {
        if (ignore != null) {
            function.withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_IGNORE_EMPTY_FILE_LIST, ValueType.BOOLEAN, new ValueEvaluation(ignore));
        }
        return function;
    }

    @Test
    public void ignoreEmptyFileList() throws Exception {
        // e-publikace ma soubor bud PDF, nebo EPUB; pravidla VEP maji u obou nastroju ignore_empty_file_list=TRUE
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.EPUBCHECK, "");
        Engine engine = ePublicationEngineWith(manager);
        assertFalse(withIgnoreEmptyFileList(buildValidationFunction(engine, ExternalUtil.EPUBCHECK, ResourceType.ORIGINAL_COPY_EPUB,
                Collections.<File>emptyList(), "epub", null), true).validate().hasProblems());
        assertSingleProblem(withIgnoreEmptyFileList(buildValidationFunction(engine, ExternalUtil.EPUBCHECK, ResourceType.ORIGINAL_COPY_EPUB,
                Collections.<File>emptyList(), "epub", null), false).validate(), Level.ERROR, "prázdný seznam souborů");
        assertSingleProblem(withIgnoreEmptyFileList(buildValidationFunction(engine, ExternalUtil.EPUBCHECK, ResourceType.ORIGINAL_COPY_EPUB,
                Collections.<File>emptyList(), "epub", null), null).validate(), Level.ERROR, "prázdný seznam souborů");
    }

    @Test
    public void ignoreEmptyFileListBeforeToolAvailability() throws Exception {
        // prazdny seznam se posoudi drive nez dostupnost nastroje: balicek s PDF a bez EPUBCheck nedostane INFO za EPUB
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.EPUBCHECK, "");
        manager.setUtilAvailable(ExternalUtil.EPUBCHECK, false);
        Engine engine = ePublicationEngineWith(manager);
        assertFalse(withIgnoreEmptyFileList(buildValidationFunction(engine, ExternalUtil.EPUBCHECK, ResourceType.ORIGINAL_COPY_EPUB,
                Collections.<File>emptyList(), "epub", null), true).validate().hasProblems());
        // neprazdny seznam a nedostupny nastroj: jedna INFO hlaska za pravidlo, ne chyba u kazdeho souboru (nalez 27)
        assertSingleProblem(withIgnoreEmptyFileList(buildValidationFunction(engine, ExternalUtil.EPUBCHECK, ResourceType.ORIGINAL_COPY_EPUB,
                Arrays.asList(new File("a.epub"), new File("b.epub")), "epub", null), true).validate(), Level.INFO, "nástroj EPUBCheck není dostupný");
    }

    @Test
    public void unexpectedProfileExceptionIsErrorOfFile() throws Exception {
        // null vystup u XML profilu: drive spolknuto (nalez 23), dnes ERROR u souboru
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.VERAPDF, null);
        ValidationResult result = buildValidationFunction(ePublicationEngineWith(manager), ExternalUtil.VERAPDF, ResourceType.ORIGINAL_COPY_PDF,
                Collections.singletonList(new File("a.pdf")), "pdf", Level.WARNING).validate();
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
        assertEquals(new File("a.pdf"), result.getProblems().get(0).getReferencedFile());
        assertTrue(result.getProblems().get(0).getFullMessage(), result.getProblems().get(0).getFullMessage().startsWith("neočekávaná chyba při zpracování výstupu nástroje veraPDF"));
    }

    private static void assertSingleProblem(ValidationResult result, Level level, String message) {
        assertEquals(1, result.getProblems().size());
        assertEquals(level, result.getProblems().get(0).getLevel());
        assertEquals(message, result.getProblems().get(0).getFullMessage());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Jpylyzer

    @Test
    public void jpylyzerOkMc() {
        assumeAvailable(ExternalUtil.JPYLYZER);
        assertNoProblems(validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, filesInDir("ok_mc")));
    }

    @Test
    public void jpylyzerOkUc() {
        assumeAvailable(ExternalUtil.JPYLYZER);
        // UC1 a UC2 maji kompresni pomer 2.67, profil UC fund_unit_0.2 vyzaduje alespon 6
        Map<String, List<String>> problems = problemsByFile(validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_USER_COPY, filesInDir("ok_uc")));
        assertEquals(new TreeSet<>(Arrays.asList("UC1.jp2", "UC2.jp2")), problems.keySet());
        for (List<String> fileProblems : problems.values()) {
            assertEquals(1, fileProblems.size());
            assertTrue(fileProblems.get(0), fileProblems.get(0).startsWith("Kompresní poměr: hodnota \"2.67\""));
        }
    }

    @Test
    public void jpylyzerProfileMismatchMc() {
        assumeAvailable(ExternalUtil.JPYLYZER);
        List<File> files = filesInDir("odlis_od_prof_mc");
        Map<String, List<String>> problems = problemsByFile(validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, files));
        // precintstarsidata.jp2 profil MC splnuje (v adresari je omylem nebo jako stara data)
        Set<String> expected = new TreeSet<>();
        files.forEach(f -> expected.add(f.getName()));
        expected.remove("precintstarsidata.jp2");
        assertEquals(expected, problems.keySet());
        assertTrue(problems.get("LRCP.jp2").contains("Order: hodnota \"LRCP\" neodpovídá žádnému z omezení: [musí být přesně 'RPCL']"));
        assertTrue(problems.get("bypass.jp2").get(0).startsWith("Bypass: hodnota \"no\""));
    }

    @Test
    public void jpylyzerProfileMismatchUc() {
        assumeAvailable(ExternalUtil.JPYLYZER);
        List<File> files = filesInDir("odlis_od_prof_uc");
        assertEveryFileHasProblem(files, validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_USER_COPY, files), null);
    }

    @Test
    public void jpylyzerInvalid() {
        assumeAvailable(ExternalUtil.JPYLYZER);
        List<File> files = new ArrayList<>(filesInDir("nevalidni"));
        // 隨機中國文字.jp2 je podle Jpylyzeru validni (JHOVE ho odmitne kvuli UUID List Boxu), profil ale porusuje jinde
        File chinese = file("nevalidni", "隨機中國文字.jp2");
        files.remove(chinese);
        assertEveryFileHasProblem(files, validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, files),
                "Valid: hodnota \"False\" neodpovídá žádnému z omezení");
        Map<String, List<String>> chineseProblems = problemsByFile(validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(chinese)));
        assertFalse(chineseProblems.isEmpty());
        assertTrue(chineseProblems.values().iterator().next().stream().noneMatch(p -> p.startsWith("Valid:")));
    }

    @Test
    public void jpylyzerIncorrect() {
        assumeAvailable(ExternalUtil.JPYLYZER);
        List<File> files = filesInDir("uplne_spatne");
        assertEveryFileHasProblem(files, validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, files), null);
    }

    @Test
    public void jpylyzerToolsJp2() {
        // mc.jp2 a uc.jp2 pro synteticke balicky profily fund_unit_0.2 nesplnuji (chybi resolution box, bez bypass,
        // uc.jp2 navic jine dlazdice, precincts, vrstvy); viz BinaryFileProfileTest
        assumeAvailable(ExternalUtil.JPYLYZER);
        Map<String, List<String>> mc = problemsByFile(validate(ExternalUtil.JPYLYZER, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File(TOOLS_JP2_DIR, "mc.jp2"))));
        assertEquals(3, mc.get("mc.jp2").size());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // JHOVE

    @Test
    public void jhoveOkMc() {
        assumeAvailable(ExternalUtil.JHOVE);
        assertNoProblems(validate(ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, filesInDir("ok_mc")));
    }

    @Test
    public void jhoveOkUc() {
        assumeAvailable(ExternalUtil.JHOVE);
        assertNoProblems(validate(ExternalUtil.JHOVE, ResourceType.IMAGE_USER_COPY, filesInDir("ok_uc")));
    }

    @Test
    public void jhoveToolsJp2() {
        assumeAvailable(ExternalUtil.JHOVE);
        assertNoProblems(validate(ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File(TOOLS_JP2_DIR, "mc.jp2"))));
    }

    @Test
    public void jhoveProfileMismatchMc() {
        assumeAvailable(ExternalUtil.JHOVE);
        // JHOVE nevidi bypass, precincts ani SOP/EPH; tyto soubory profil JHOVE splni
        List<File> files = filesInDir("odlis_od_prof_mc");
        Map<String, List<String>> problems = problemsByFile(validate(ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, files));
        assertEquals(new TreeSet<>(Arrays.asList("LRCP.jp2", "LRCPa.jp2", "RLCP.jp2", "codeblock.jp2", "codeblock2.jp2", "codeblock3.jp2",
                "ireversible.jp2", "ireversible2.jp2", "ireversible3.jp2", "ireversible4.jp2", "ireversible5.jp2",
                "layers.jp2", "layers2.jp2", "levels.jp2", "levels2.jp2", "tiles.jp2", "tiles2.jp2", "toles3.jp2")), problems.keySet());
        assertEquals(Collections.singletonList("Order: hodnota \"0\" neodpovídá žádnému z omezení: [musí být přesně '2']"), problems.get("LRCP.jp2"));
    }

    @Test
    public void jhoveProfileMismatchUc() {
        // JHOVE odchylky souboru v odlis_od_prof_uc (SOP, bypass, rozliseni) nepozna
        assumeAvailable(ExternalUtil.JHOVE);
        assertNoProblems(validate(ExternalUtil.JHOVE, ResourceType.IMAGE_USER_COPY, filesInDir("odlis_od_prof_uc")));
    }

    @Test
    public void jhoveInvalid() {
        assumeAvailable(ExternalUtil.JHOVE);
        List<File> files = filesInDir("nevalidni");
        ValidationResult result = validate(ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, files);
        assertEveryFileHasProblem(files, result, null);
        // t4040i.jpf a u1910f.jp2 jsou podle JHOVE "Well-Formed and valid", ostatni ne
        Map<String, List<String>> problems = problemsByFile(result);
        for (File file : files) {
            boolean notWellFormed = problems.get(file.getName()).stream().anyMatch(p -> p.startsWith("valid: hodnota \"Not well-formed\""));
            assertEquals(file.getName(), !Arrays.asList("t4040i.jpf", "u1910f.jp2").contains(file.getName()), notWellFormed);
        }
    }

    @Test
    public void jhoveIncorrect() {
        assumeAvailable(ExternalUtil.JHOVE);
        List<File> files = filesInDir("uplne_spatne");
        assertEveryFileHasProblem(files, validate(ExternalUtil.JHOVE, ResourceType.IMAGE_MASTER_COPY, files), null);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Kakadu

    @Test
    public void kakaduOkMc() {
        assumeAvailable(ExternalUtil.KAKADU);
        assertNoProblems(validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, filesInDir("ok_mc")));
    }

    @Test
    public void kakaduOkUc() {
        assumeAvailable(ExternalUtil.KAKADU);
        assertNoProblems(validate(ExternalUtil.KAKADU, ResourceType.IMAGE_USER_COPY, filesInDir("ok_uc")));
    }

    @Test
    public void kakaduErrorDetection() {
        assumeAvailable(ExternalUtil.KAKADU);
        List<File> files = filesInDir("detekce_poskozeni");
        assertEveryFileHasProblem(files, validate(ExternalUtil.KAKADU, ResourceType.IMAGE_USER_COPY, files),
                "no_errors: zakázaná hodnota nalezena: Kakadu Core Error:");
    }

    @Test
    public void kakaduInvalid() {
        // Kakadu dekoduje i vetsinu souboru, ktere Jpylyzer/JHOVE odmitnou; chybu hlasi jen u nekterych
        assumeAvailable(ExternalUtil.KAKADU);
        List<File> files = new ArrayList<>(filesInDir("nevalidni"));
        File chinese = file("nevalidni", "隨機中國文字.jp2");
        files.remove(chinese);
        Map<String, List<String>> problems = problemsByFile(validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, files));
        assertEquals(new TreeSet<>(Arrays.asList("u5463f.jp2", "w1282h.jp2", "w7200f.jp2")), problems.keySet());
        // 隨機中國文字.jp2 Kakadu dekoduje bez chyby; na Windows s kodovou strankou bez techto znaku ho ale neotevre
        // (overeni 23. 11. 2026, N2) a profil to hlasi jako ERROR
        List<ValidationProblem> chineseProblems = validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(chinese)).getProblems();
        if (Platform.detectOs().getOperatingSystem() != OperatingSystem.WINDOWS) {
            assertEquals(chineseProblems.toString(), Collections.emptyList(), chineseProblems);
        } else if (!chineseProblems.isEmpty()) {
            assertEquals(chineseProblems.toString(), 1, chineseProblems.size());
            assertEquals(Level.ERROR, chineseProblems.get(0).getLevel());
            assertTrue(chineseProblems.get(0).getFullMessage(), chineseProblems.get(0).getFullMessage().startsWith(KAKADU_CANNOT_OPEN));
        }
    }

    @Test
    public void kakaduNonexistentFileIsError() {
        // skutecny kdu_expand nad souborem, ktery neexistuje: "Error in Kakadu File Format Support: Unable to open
        // input file" (N2); dva soubory, aby bylo videt, ze chyba patri ke kazdemu zvlast
        assumeAvailable(ExternalUtil.KAKADU);
        File dir = new File(EXAMPLES_ROOT_DIR, "nevalidni");
        List<File> files = Arrays.asList(new File(dir, "neexistuje.jp2"), new File(dir, "neexistuje ani tento.jp2"));
        for (File missing : files) {
            assertFalse(missing.getPath(), missing.exists());
        }
        ValidationResult result = validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, files);
        assertEquals(result.getProblems().toString(), 2, result.getProblems().size());
        for (int i = 0; i < files.size(); i++) {
            ValidationProblem problem = result.getProblems().get(i);
            assertEquals(Level.ERROR, problem.getLevel());
            assertEquals(files.get(i), problem.getReferencedFile());
            String message = problem.getFullMessage();
            assertTrue(message, message.startsWith(KAKADU_CANNOT_OPEN));
            // nazev souboru je v hlaseni (Kakadu dlouhou cestu zalomi i uprostred slova, porovnava se bez bilych znaku)
            assertTrue(message, message.replaceAll("\\s", "").contains(files.get(i).getName().replaceAll("\\s", "") + "\"."));
        }
    }

    @Test
    public void kakaduIncorrectPasses() {
        // soubory v uplne_spatne Kakadu dekoduje bez chyby (vady jsou jen v parametrech komprese)
        assumeAvailable(ExternalUtil.KAKADU);
        assertNoProblems(validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, filesInDir("uplne_spatne")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // ImageMagick (bez JP2 delegata i s nim: ImageMagick 7.1 na macOS a Linuxu, 6.9 na Linuxu)

    private static void assumeImageMagickWithJp2() {
        assumeAvailable(ExternalUtil.IMAGE_MAGICK);
        Assume.assumeTrue("ImageMagick bez JP2 delegáta: " + imageMagickJp2Problem, imageMagickJp2Problem == null);
    }

    @Test
    public void imageMagickWithoutJp2DelegateReportsEveryFile() {
        assumeAvailable(ExternalUtil.IMAGE_MAGICK);
        Assume.assumeTrue("ImageMagick má JP2 delegáta", imageMagickJp2Problem != null);
        List<File> files = filesInDir("ok_mc");
        assertEveryFileHasProblem(files, validate(ExternalUtil.IMAGE_MAGICK, ResourceType.IMAGE_MASTER_COPY, files),
                "no_errors: zakázaná hodnota nalezena: identify: no decode delegate for this image format");
    }

    @Test
    public void imageMagickToolsJp2() {
        assumeImageMagickWithJp2();
        assertNoProblems(validate(ExternalUtil.IMAGE_MAGICK, ResourceType.IMAGE_MASTER_COPY, Collections.singletonList(new File(TOOLS_JP2_DIR, "mc.jp2"))));
    }

    @Test
    public void imageMagickErrorDetection() {
        assumeImageMagickWithJp2();
        List<File> files = filesInDir("detekce_poskozeni");
        assertFalse("adresar detekce_poskozeni musi obsahovat soubory", files.isEmpty());
        ValidationResult result = buildValidationFunction(engine, ExternalUtil.IMAGE_MAGICK, ResourceType.IMAGE_USER_COPY, files,
                "jp2k", Level.ERROR).validate();
        // kazdy poskozeny soubor ma ERROR z profilu imageMagick.xml (zakazana hodnota v no_errors s hlaskou ImageMagicku);
        // nestaci libovolny problem (napr. INFO o neoverene verzi). Nalez N1: ImageMagick 6.9 z PATH pise
        // "identify-im6.q16:" a profil s puvodnim vzorem "identify:" nenasel nic
        for (File file : files) {
            List<String> errors = new ArrayList<>();
            for (ValidationProblem problem : result.getProblems()) {
                if (problem.getLevel() == Level.ERROR && file.equals(problem.getReferencedFile())
                        && problem.getFullMessage().startsWith("no_errors: zakázaná hodnota nalezena: identify")) {
                    errors.add(problem.getFullMessage());
                }
            }
            assertFalse("bez ERROR z profilu: " + file.getName() + ", problémy: " + levelsMessagesAndFiles(result), errors.isEmpty());
        }
        assertNoProblemContains(result, "Cannot run program");
        assertNoProblemContains(result, "nesplněn kontrakt");
    }

    private static void assertNoProblemContains(ValidationResult result, String fragment) {
        for (ValidationProblem p : result.getProblems()) {
            assertFalse("neocekavana hlaska: " + p.getFullMessage(), p.getFullMessage().contains(fragment));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // veraPDF a EPUBCheck (skutecne nastroje)

    private static ValidationResult validateEPublication(ExternalUtil util, File file) {
        boolean pdf = util == ExternalUtil.VERAPDF;
        return buildValidationFunction(ePublicationEngine, util, pdf ? ResourceType.ORIGINAL_COPY_PDF : ResourceType.ORIGINAL_COPY_EPUB,
                Collections.singletonList(file), pdf ? "pdf" : "epub", null).validate();
    }

    private File emptyFile(String name) throws Exception {
        File dir = java.nio.file.Files.createTempDirectory("kv-epub").toFile();
        dir.deleteOnExit();
        File file = new File(dir, name);
        assertTrue(file.createNewFile());
        file.deleteOnExit();
        return file;
    }

    @Test
    public void veraPdfCompliantPdfA1b() {
        assumeAvailable(ExternalUtil.VERAPDF);
        // dummy_PDFA-1B.pdf (22 kB, z test resources VEP) je PDF/A-1b; bez WARNING diky oprave R1 (profil VEP cekal jen "PDF/A-1B")
        assertEquals(Collections.<String>emptyList(),
                levelsAndMessages(validateEPublication(ExternalUtil.VERAPDF, new File(EPUBLIKACE_DIR, "dummy_PDFA-1B.pdf"))));
    }

    @Test
    public void veraPdfPdfWithoutPdfA() {
        assumeAvailable(ExternalUtil.VERAPDF);
        // synteticky_bez_pdfa.pdf (329 B): platne PDF 1.4 bez PDF/A, veraPDF zvoli PDF/A-1b a soubor nevyhovi
        // (verze "PDF/A-1b" je po oprave R1 podporovana, zustava jen chyba validity)
        assertEquals(Collections.singletonList("ERROR: Soubor musí být validní: hodnota \"false\" neodpovídá žádnému z omezení: [musí být přesně 'true']"),
                levelsAndMessages(validateEPublication(ExternalUtil.VERAPDF, new File(EPUBLIKACE_DIR, "synteticky_bez_pdfa.pdf"))));
    }

    @Test
    public void veraPdfEmptyFile() throws Exception {
        assumeAvailable(ExternalUtil.VERAPDF);
        assertEquals(Arrays.asList(
                        "ERROR: Soubor musí být validní: kontrolovaná hodnota nenalezena",
                        "ERROR: Soubor PDF/A by měl být v podporované verzi: kontrolovaná hodnota nenalezena"),
                levelsAndMessages(validateEPublication(ExternalUtil.VERAPDF, emptyFile("prazdny.pdf"))));
    }

    @Test
    public void epubCheckSyntheticEpub3() {
        assumeAvailable(ExternalUtil.EPUBCHECK);
        // synteticky_epub3.epub (1,8 kB): Well-formed EPUB 3.4 s fontem font/woff (core media type EPUB 3.3, projde)
        List<String> problems = levelsAndMessages(validateEPublication(ExternalUtil.EPUBCHECK, new File(EPUBLIKACE_DIR, "synteticky_epub3.epub")));
        assertEquals(problems.toString(), 1, problems.size());
        assertEquals("WARNING: Soubor Epub by měl být v podporované verzi: hodnota \"3.4\" neodpovídá žádnému z omezení: [musí být přesně '2.0.1']", problems.get(0));
    }

    @Test
    public void epubCheckEmptyFile() throws Exception {
        assumeAvailable(ExternalUtil.EPUBCHECK);
        List<String> problems = levelsAndMessages(validateEPublication(ExternalUtil.EPUBCHECK, emptyFile("prazdny.epub")));
        assertEquals(problems.toString(), 4, problems.size());
        assertEquals("ERROR: Soubor musí být validní: hodnota \"Not well-formed\" neodpovídá žádnému z omezení: [musí být přesně 'Well-formed']", problems.get(0));
        assertEquals("ERROR: Soubor Epub by měl být v podporované verzi: kontrolovaná hodnota nenalezena", problems.get(1));
        assertTrue(problems.get(2), problems.get(2).startsWith("WARNING: Detekce chybových hlášek: zakázaná hodnota nalezena: PKG-008, FATAL, "));
    }
}
