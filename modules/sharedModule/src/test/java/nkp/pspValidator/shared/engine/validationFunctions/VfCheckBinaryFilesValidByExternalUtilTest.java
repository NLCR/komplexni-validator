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
import org.junit.Test;

import java.io.File;
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
 * Pozor: u Jpylyzeru na macOS a Linuxu se cesta pri detekci verze ignoruje (externalUtils.xml nema ${UTIL_PATH},
 * viz ExternalUtilManagerTest.jpylyzerPathIsIgnoredOnMac), jpylyzer proto musi byt v PATH.
 * <p>
 * Ocekavane vysledky jsou overene s Jpylyzer 2.2.0, JHOVE 1.29.0 a Kakadu 8.5 (8. 11. 2026).
 */
public class VfCheckBinaryFilesValidByExternalUtilTest {

    private static final File EXAMPLES_ROOT_DIR = new File("src/test/resources/jpeg2000");
    private static final File TOOLS_JP2_DIR = new File("src/test/tools/jp2");
    private static final Dmf DMF = new Dmf(Dmf.Type.FUND_UNIT, "0.2");

    private static final Map<ExternalUtil, String> PATH_PROPERTIES = new EnumMap<>(ExternalUtil.class);

    static {
        PATH_PROPERTIES.put(ExternalUtil.JPYLYZER, "jpylyzer.path");
        PATH_PROPERTIES.put(ExternalUtil.JHOVE, "jhove.path");
        PATH_PROPERTIES.put(ExternalUtil.KAKADU, "kakadu.path");
        PATH_PROPERTIES.put(ExternalUtil.IMAGE_MAGICK, "imagemagick.path");
    }

    private static final Map<ExternalUtil, String> versions = new EnumMap<>(ExternalUtil.class);
    private static final Map<ExternalUtil, String> unavailableReasons = new EnumMap<>(ExternalUtil.class);
    private static Engine engine;
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
        return new VfCheckBinaryFilesValidByExternalUtil("test", engine)
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_FILES, ValueType.FILE_LIST, new ValueEvaluation(files))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_VALIDATION_PROBLEM_LEVEL, ValueType.LEVEL, new ValueEvaluation(Level.WARNING))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_TYPE, ValueType.RESOURCE_TYPE, new ValueEvaluation(type))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_UTIL, ValueType.EXTERNAL_UTIL, new ValueEvaluation(util))
                .withValueParam(VfCheckBinaryFilesValidByExternalUtil.PARAM_EXECUTION, ValueType.STRING, new ValueEvaluation("jp2k"));
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
        Map<String, List<String>> problems = problemsByFile(validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, filesInDir("nevalidni")));
        assertEquals(new TreeSet<>(Arrays.asList("u5463f.jp2", "w1282h.jp2", "w7200f.jp2")), problems.keySet());
    }

    @Test
    public void kakaduIncorrectPasses() {
        // soubory v uplne_spatne Kakadu dekoduje bez chyby (vady jsou jen v parametrech komprese)
        assumeAvailable(ExternalUtil.KAKADU);
        assertNoProblems(validate(ExternalUtil.KAKADU, ResourceType.IMAGE_MASTER_COPY, filesInDir("uplne_spatne")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // ImageMagick (s JP2 delegatem neovereno, na Macu pri psani testu byl jen ImageMagick bez OpenJPEG)

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
        ValidationResult result = validate(ExternalUtil.IMAGE_MAGICK, ResourceType.IMAGE_USER_COPY, files);
        assertTrue(result.hasProblems());
        // neoverene (ImageMagick s JP2 delegatem na tomto stroji chybi): poskozeni ma hlasit profil imageMagick.xml
        // pres zakazanou hodnotu v no_errors, ne chyba spusteni nastroje
        assertNoProblemContains(result, "Cannot run program");
        assertNoProblemContains(result, "nesplněn kontrakt");
    }

    private static void assertNoProblemContains(ValidationResult result, String fragment) {
        for (ValidationProblem p : result.getProblems()) {
            assertFalse("neocekavana hlaska: " + p.getMessage(), p.getMessage().contains(fragment));
        }
    }
}
