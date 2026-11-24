package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileProfile;
import nkp.pspValidator.shared.externalUtils.validation.Validation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.*;
import static org.junit.Assert.*;

/**
 * Nástroj, který soubor nezkontroloval (spadl, nespustil se, skončil neobvyklým kódem, nevypsal znak zpracování
 * souboru), nesmí projít bez hlášky: konfigurace externalUtils.xml (exitCodes, processedFileEvidence), posouzení
 * výsledku procesu ({@link UtilHandler#evaluateExecution}) a profil binárního souboru.
 * <p>
 * Spuštění nástroje se testuje falešnými nástroji ({@link FakeExternalUtils}: na Unixu skript /bin/sh, na Windows
 * dávka), které skončí daným kódem, takže test běží na všech systémech; posuzuje se podle platformy z konfigurace
 * (manažer pro MAC, LINUX nebo WINDOWS), ne podle systému, na kterém test běží. Záporné kódy Windows dávka ani skript
 * vrátit neumí, ty se testují s uloženým výsledkem procesu ({@link CannedOutputExternalUtilManager#withResult}).
 * Profily Kakadu a ImageMagicku všech fDMF se ověřují nad uloženými skutečnými výstupy úspěšného běhu všech
 * dostupných řad (src/test/resources/externalUtils/{kakadu,imageMagick}/uspech_stdout, popis v README.md tamtéž).
 */
public class ToolFailureDetectionTest {

    private static final File DUMMY_FILE = new File("src/test/tools/jp2/mc.jp2");
    private static final String FDMF = "fund_unit_0.2";
    private static final String LIBRARY_NOT_LOADED = "dyld[4242]: Library not loaded: ../../lib/Mac-arm-64-gcc/libkdu_v85R.so\n"
            + "  Referenced from: <1F2E3D4C> /Users/x/kakadu/bin/Mac-arm-64-gcc/kdu_expand\n";
    private static final String KAKADU_NOT_CHECKED_134 = "nástroj Kakadu soubor nezkontroloval: skončil s kódem 134 (pád nástroje, signál 6 SIGABRT), "
            + "výstup: dyld[4242]: Library not loaded: ../../lib/Mac-arm-64-gcc/libkdu_v85R.so Referenced from: <1F2E3D4C> /Users/x/kakadu/bin/Mac-arm-64-gcc/kdu_expand";
    private static final String IMAGE_MAGICK_NOT_CHECKED_139 = "nástroj ImageMagick soubor nezkontroloval: skončil s kódem 139 (pád nástroje, signál 11 SIGSEGV), bez výstupu";
    private static final String KAKADU_NO_EVIDENCE = "nástroj Kakadu soubor nezkontroloval: ve výstupu chybí údaj o zpracování souboru (kód 0), bez výstupu";
    private static final String IMAGE_MAGICK_NO_EVIDENCE_1 = "nástroj ImageMagick soubor nezkontroloval: ve výstupu chybí údaj o zpracování souboru (kód 1), bez výstupu";
    private static final String KAKADU_NO_CODESTREAM = "chyba Kakadu při čtení souboru (soubor se nezkontroloval): zakázaná hodnota nalezena: "
            + "Error in Kakadu File Format Support:\nJPX data source appears to contain no codestreams at all.";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File fakeUtilsDir;

    @Before
    public void setUp() throws Exception {
        fakeUtilsDir = tmp.newFolder("fakeUtils");
    }

    private ExternalUtilManager manager(OperatingSystem os) throws Exception {
        return manager(EXTERNAL_UTILS_CONFIG, os);
    }

    private ExternalUtilManager manager(File config, OperatingSystem os) throws Exception {
        File hostConfig = FakeExternalUtils.hostConfig(config, tmp.newFolder());
        return new ExternalUtilManagerFactory(hostConfig).buildExternalUtilManager(os);
    }

    private File textFile(String content) throws Exception {
        File file = tmp.newFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static UtilHandler handler(OperatingSystem os, ExternalUtil util, String execution) throws Exception {
        UtilHandler handler = new ExternalUtilManagerFactory(EXTERNAL_UTILS_CONFIG).buildExternalUtilManager(os)
                .getExecutionHandler(new ExternalUtilExecution(execution, util));
        assertNotNull(os + "/" + util + "/" + execution, handler);
        return handler;
    }

    private static List<String> toStrings(List<Validation.Problem> problems) {
        List<String> result = new ArrayList<>();
        for (Validation.Problem problem : problems) {
            result.add(problem.toString());
        }
        return result;
    }

    /** Problémy profilu fdmf/copy nad uloženým výsledkem procesu posouzeným podle konfigurace pro os. */
    private static List<String> problems(OperatingSystem os, ExternalUtil util, String fdmf, String copy, CliCommand.Result result) throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withResult(util, handler(os, util, "jp2k"), result);
        return toStrings(loadProfile(manager, fdmf, copy, util).validateProblems("jp2k", DUMMY_FILE));
    }

    private static List<String[]> fdmfCopies(ExternalUtil util) {
        List<String[]> result = new ArrayList<>();
        File[] fdmfDirs = FDMF_DIR.listFiles(File::isDirectory);
        Arrays.sort(fdmfDirs);
        for (File fdmfDir : fdmfDirs) {
            for (String copy : new String[]{"mc", "uc"}) {
                if (profileFile(fdmfDir.getName(), copy, util).isFile()) {
                    result.add(new String[]{fdmfDir.getName(), copy});
                }
            }
        }
        return result;
    }

    private static List<String> storedOutputs(String util) {
        File dir = new File(new File(OUTPUTS_DIR, util), "uspech_stdout");
        String[] names = dir.list((d, name) -> name.endsWith(".txt"));
        assertNotNull(dir.getPath(), names);
        Arrays.sort(names);
        return Arrays.asList(names);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // konfigurace externalUtils.xml

    /**
     * Běžné kódy změřené 23. 11. 2026 na macOS a v Debianu 13 a Ubuntu 24.04 (viz README.md v externalUtils); na Windows
     * jen pád (kódy neověřeny). Hodnoty ručně podle měření, ne z konfigurace.
     */
    @Test
    public void configHasExitCodesForEveryExecution() throws Exception {
        Map<String, Set<Integer>> expectedUnix = new LinkedHashMap<>();
        expectedUnix.put("JPYLYZER/jp2k", new TreeSet<>(Collections.singletonList(0)));
        expectedUnix.put("IMAGE_MAGICK/jp2k", new TreeSet<>(Arrays.asList(0, 1)));
        expectedUnix.put("JHOVE/jp2k", new TreeSet<>(Collections.singletonList(0)));
        expectedUnix.put("JHOVE/wav", new TreeSet<>(Collections.singletonList(0)));
        expectedUnix.put("KAKADU/jp2k", new TreeSet<>(Arrays.asList(0, 255)));
        expectedUnix.put("MP3VAL/mp3", new TreeSet<>(Collections.singletonList(0)));
        expectedUnix.put("SHNTOOL/wav", new TreeSet<>(Arrays.asList(0, 1)));
        expectedUnix.put("CHECKMATE/mp3", new TreeSet<>(Arrays.asList(0, 1)));
        expectedUnix.put("VERAPDF/pdf", new TreeSet<>(Arrays.asList(0, 1, 7, 8)));
        expectedUnix.put("EPUBCHECK/epub", new TreeSet<>(Arrays.asList(0, 1)));
        for (OperatingSystem os : OperatingSystem.values()) {
            for (Map.Entry<String, Set<Integer>> entry : expectedUnix.entrySet()) {
                String[] utilAndExecution = entry.getKey().split("/");
                UtilHandler handler = handler(os, ExternalUtil.valueOf(utilAndExecution[0]), utilAndExecution[1]);
                ExitCodePolicy policy = handler.getExitCodePolicy();
                assertNotNull(os + "/" + entry.getKey(), policy);
                assertEquals(os + "/" + entry.getKey(), os, policy.getOs());
                assertEquals(os + "/" + entry.getKey(), os == OperatingSystem.WINDOWS ? null : entry.getValue(), policy.getNormalCodes());
            }
        }
    }

    @Test
    public void configHasProcessedEvidenceOnlyForKakaduAndImageMagick() throws Exception {
        for (OperatingSystem os : OperatingSystem.values()) {
            for (ExternalUtil util : ExternalUtil.values()) {
                for (String execution : new String[]{"jp2k", "wav", "mp3", "pdf", "epub"}) {
                    UtilHandler handler = new ExternalUtilManagerFactory(EXTERNAL_UTILS_CONFIG).buildExternalUtilManager(os)
                            .getExecutionHandler(new ExternalUtilExecution(execution, util));
                    if (handler == null) {
                        continue;
                    }
                    UtilHandler.ProcessedEvidence evidence = handler.getProcessedEvidence();
                    String where = os + "/" + util + "/" + execution;
                    if (util == ExternalUtil.KAKADU) {
                        assertNotNull(where, evidence);
                        assertEquals(where, Stream.STDOUT, evidence.getStream());
                        assertEquals(where, "Consumed [0-9,]+ tile-part\\(s\\)", evidence.getRegexp());
                    } else if (util == ExternalUtil.IMAGE_MAGICK && os != OperatingSystem.WINDOWS) {
                        assertNotNull(where, evidence);
                        assertEquals(where, Stream.STDOUT, evidence.getStream());
                        assertEquals(where, "(?m)^\\s*Geometry: [0-9]", evidence.getRegexp());
                    } else {
                        // ImageMagick na Windows: výstup neověřen, znak zpracování se nekontroluje
                        assertNull(where, evidence);
                    }
                }
            }
        }
    }

    /** Konfigurace externalUtils.xml bez nových elementů (kopie starší než 3.0 v adresáři konfigurace uživatele). */
    private File oldConfig() throws Exception {
        String xml = new String(Files.readAllBytes(EXTERNAL_UTILS_CONFIG.toPath()), StandardCharsets.UTF_8);
        String old = xml.replaceAll("\\s*<exitCodes[^>]*/>", "").replaceAll("\\s*<processedFileEvidence[^>]*>[^<]*</processedFileEvidence>", "");
        assertFalse(old.contains("<exitCodes"));
        assertFalse(old.contains("<processedFileEvidence"));
        File file = new File(tmp.newFolder("stara"), "externalUtils.xml");
        Files.write(file.toPath(), old.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    public void oldConfigWithoutNewElementsBehavesAsBefore() throws Exception {
        File oldConfig = oldConfig();
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = new ExternalUtilManagerFactory(oldConfig).buildExternalUtilManager(os);
            UtilHandler handler = manager.getExecutionHandler(new ExternalUtilExecution("jp2k", ExternalUtil.KAKADU));
            assertNull(os.name(), handler.getExitCodePolicy());
            assertNull(os.name(), handler.getProcessedEvidence());
        }
        // pád Kakadu i ImageMagicku projde se starou konfigurací bez hlášky, jako před 3.0
        fakeUtil("kdu_expand", textFile(""), textFile(LIBRARY_NOT_LOADED), 134);
        fakeUtil("identify", null, null, 139);
        ExternalUtilManager manager = manager(oldConfig, OperatingSystem.MAC);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        assertEquals(Collections.emptyList(), loadProfile(manager, FDMF, "mc", ExternalUtil.KAKADU).validate("jp2k", DUMMY_FILE));
        assertEquals(Collections.emptyList(), loadProfile(manager, FDMF, "mc", ExternalUtil.IMAGE_MAGICK).validate("jp2k", DUMMY_FILE));
    }

    private void fakeUtil(String program, File stdout, File stderr, int exitCode) throws Exception {
        FakeExternalUtils.create(fakeUtilsDir, program, stdout, stderr, exitCode);
    }

    private File configWithKakaduMacExecution(String extraElements) throws Exception {
        String xml = new String(Files.readAllBytes(EXTERNAL_UTILS_CONFIG.toPath()), StandardCharsets.UTF_8);
        String marker = "<command>${UTIL_PATH}kdu_expand -i ${TARGET_FILE}</command>";
        int executionStart = xml.indexOf("<execution name=\"jp2k\">", xml.indexOf("<util name=\"KAKADU\">"));
        int macStart = xml.indexOf("<operatingSystem name=\"MAC\">", executionStart);
        int commandAt = xml.indexOf(marker, macStart);
        int osEnd = xml.indexOf("</operatingSystem>", commandAt);
        int parserEnd = xml.indexOf("</parser>", commandAt) + "</parser>".length();
        assertTrue(executionStart > 0 && macStart > 0 && commandAt > 0 && parserEnd < osEnd);
        String changed = xml.substring(0, parserEnd) + extraElements + xml.substring(osEnd);
        File file = new File(tmp.newFolder(), "externalUtils.xml");
        Files.write(file.toPath(), changed.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private void assertInvalidConfig(String extraElements, String expectedMessage) throws Exception {
        try {
            new ExternalUtilManagerFactory(configWithKakaduMacExecution(extraElements));
            fail(extraElements);
        } catch (ValidatorConfigurationException e) {
            assertEquals(extraElements, expectedMessage, e.getMessage());
        }
    }

    @Test
    public void invalidConfigIsRejected() throws Exception {
        assertInvalidConfig("<exitCodes normal=\"0 x\"/>", "KAKADU, spuštění jp2k, MAC: exitCodes: neplatný návratový kód 'x'");
        assertInvalidConfig("<exitCodes normal=\"0,1.5\"/>", "KAKADU, spuštění jp2k, MAC: exitCodes: neplatný návratový kód '1.5'");
        assertInvalidConfig("<processedFileEvidence stream=\"STDIN\">x</processedFileEvidence>",
                "KAKADU, spuštění jp2k, MAC: processedFileEvidence: neplatný proud 'STDIN' (STDOUT nebo STDERR)");
        assertInvalidConfig("<processedFileEvidence>x</processedFileEvidence>",
                "KAKADU, spuštění jp2k, MAC: processedFileEvidence: neplatný proud '' (STDOUT nebo STDERR)");
        assertInvalidConfig("<processedFileEvidence stream=\"STDOUT\">  </processedFileEvidence>",
                "KAKADU, spuštění jp2k, MAC: processedFileEvidence: chybí regulární výraz");
        assertInvalidConfig("<processedFileEvidence stream=\"STDOUT\">Consumed (</processedFileEvidence>",
                "KAKADU, spuštění jp2k, MAC: processedFileEvidence: neplatný regulární výraz: Unclosed group");
    }

    @Test
    public void validConfigVariantsAreAccepted() throws Exception {
        // prázdný atribut normal = jen pád; evidence na STDERR
        ExternalUtilManager manager = new ExternalUtilManagerFactory(configWithKakaduMacExecution(
                "<exitCodes normal=\"\"/><processedFileEvidence stream=\"STDERR\">hotovo</processedFileEvidence>")).buildExternalUtilManager(OperatingSystem.MAC);
        UtilHandler handler = manager.getExecutionHandler(new ExternalUtilExecution("jp2k", ExternalUtil.KAKADU));
        assertNull(handler.getExitCodePolicy().getNormalCodes());
        assertEquals(Stream.STDERR, handler.getProcessedEvidence().getStream());
        assertNull(handler.evaluateExecution(new CliCommand.Result(1, "", "hotovo\n")).getMissingEvidence());
        assertEquals("ve výstupu chybí údaj o zpracování souboru (kód 0), výstup: hotovo",
                handler.evaluateExecution(new CliCommand.Result(0, "hotovo\n", "")).getMissingEvidence());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // simulace pádu skutečným procesem (falešný nástroj)

    @Test
    public void kakaduLibraryNotLoadedIsSingleError() throws Exception {
        // macOS: kdu_expand bez knihoven (dyld) skončí SIGABRT (134) a vypíše jen hlášku dyld; dřív 0 problémů
        fakeUtil("kdu_expand", textFile(""), textFile(LIBRARY_NOT_LOADED), 134);
        for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.MAC, OperatingSystem.LINUX}) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
            for (String copy : new String[]{"mc", "uc"}) {
                List<Validation.Problem> problems = loadProfile(manager, FDMF, copy, ExternalUtil.KAKADU).validateProblems("jp2k", DUMMY_FILE);
                assertEquals(os + "/" + copy, Collections.singletonList("ERROR: " + KAKADU_NOT_CHECKED_134), toStrings(problems));
            }
        }
    }

    @Test
    public void imageMagickSegfaultWithoutOutputIsSingleError() throws Exception {
        fakeUtil("identify", null, null, 139);
        for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.MAC, OperatingSystem.LINUX}) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
            List<Validation.Problem> problems = loadProfile(manager, FDMF, "uc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE);
            assertEquals(os.name(), Collections.singletonList("ERROR: " + IMAGE_MAGICK_NOT_CHECKED_139), toStrings(problems));
        }
    }

    @Test
    public void imageMagickSuccessfulRunPasses() throws Exception {
        // skutečný stdout identify -verbose (7.1.2 macOS) a prázdný stderr: bez problému
        fakeUtil("identify", outputFile("imageMagick", "uspech_stdout/im712_macos.txt"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        assertEquals(Collections.emptyList(), loadProfile(manager, FDMF, "mc", ExternalUtil.IMAGE_MAGICK).validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void imageMagickSilentFailureOnEmptyFileIsError() throws Exception {
        // identify -verbose nad prázdným souborem: kód 1, nic na stdout ani stderr (macOS, Debian, Ubuntu); dřív prošlo
        fakeUtil("identify", null, null, 1);
        for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.MAC, OperatingSystem.LINUX}) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
            assertEquals(os.name(), Collections.singletonList("ERROR: " + IMAGE_MAGICK_NO_EVIDENCE_1),
                    toStrings(loadProfile(manager, FDMF, "mc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE)));
        }
    }

    @Test
    public void imageMagickDamagedFileKeepsProfileErrorsOnly() throws Exception {
        // poškozený JP2: kód 1, hlášky na stderr, nic na stdout; chybí znak zpracování, ale profil hlásí 6 chyb
        // a hláška o nezkontrolovaném souboru se nepřidá (stejný počet jako před 3.0)
        fakeUtil("identify", null, outputFile("imageMagick", "im71_detekce_poskozeni_w0352gUC.txt"), 1);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        List<Validation.Problem> problems = loadProfile(manager, FDMF, "uc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE);
        assertEquals(problems.toString(), 6, problems.size());
        for (Validation.Problem problem : problems) {
            assertNull(problem.toString(), problem.getLevel());
            assertTrue(problem.getMessage(), problem.getMessage().startsWith("no_errors: zakázaná hodnota nalezena: identify: "));
        }
    }

    @Test
    public void kakaduWithoutConsumedLineIsError() throws Exception {
        // kód 0, ale bez řádku „Consumed … tile-part(s)“ (nástroj soubor nezpracoval)
        fakeUtil("kdu_expand", null, null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        assertEquals(Collections.singletonList("ERROR: " + KAKADU_NO_EVIDENCE),
                toStrings(loadProfile(manager, FDMF, "mc", ExternalUtil.KAKADU).validateProblems("jp2k", DUMMY_FILE)));
    }

    @Test
    public void kakaduSuccessfulRunPasses() throws Exception {
        fakeUtil("kdu_expand", outputFile("kakadu", "uspech_stdout/mac_arm64_8.5.txt"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        assertEquals(Collections.emptyList(), loadProfile(manager, FDMF, "mc", ExternalUtil.KAKADU).validate("jp2k", DUMMY_FILE));
    }

    @Test
    public void kakaduFileWithoutCodestreamIsSingleError() throws Exception {
        // nález z 23. 11.: JP2 bez kódového proudu, kód 255, „Error in Kakadu File Format Support“ (dřív 0 problémů);
        // hlásí ho nová validace profilu, hláška o chybějícím řádku Consumed se nepřidá
        fakeUtil("kdu_expand", textFile(""), outputFile("kakadu", "bez_kodoveho_proudu.txt"), 255);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        assertEquals(Collections.singletonList("ERROR: " + KAKADU_NO_CODESTREAM),
                toStrings(loadProfile(manager, FDMF, "uc", ExternalUtil.KAKADU).validateProblems("jp2k", DUMMY_FILE)));
    }

    @Test
    public void veraPdfUnexpectedCodeIsSingleError() throws Exception {
        // veraPDF 4 = žádný soubor ke kontrole (stdout prázdný); dřív chyba parsování XML
        fakeUtil("verapdf", textFile(""), textFile("SEVERE: File /x.pdf doesn't exist\n"), 4);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.VERAPDF, fakeUtilsDir);
        assertEquals(Collections.singletonList("ERROR: nástroj veraPDF soubor nezkontroloval: skončil s kódem 4 (neočekávaný kód, běžné jsou 0, 1, 7, 8), "
                        + "výstup: SEVERE: File /x.pdf doesn't exist"),
                toStrings(loadProfile(manager, "emonograph_3.0", "pdf", ExternalUtil.VERAPDF).validateProblems("pdf", new File("x.pdf"))));
    }

    @Test
    public void veraPdfParsingFailureCodeKeepsProfileResult() throws Exception {
        // veraPDF 7 = PDF nejde zpracovat, výsledek je v XML: profil se vyhodnotí jako dřív
        fakeUtil("verapdf", outputFile("verapdf", "noncompliant_dummy.xml"), null, 7);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.VERAPDF, fakeUtilsDir);
        List<String> errors = loadProfile(manager, "emonograph_3.0", "pdf", ExternalUtil.VERAPDF).validate("pdf", new File("x.pdf"));
        assertTrue(errors.toString(), errors.contains("Formát PDF/A: soubor není ve formátu PDF/A (DMF doporučuje PDF/A-1 nebo PDF/A-2)"));
        assertTrue(errors.toString(), errors.stream().noneMatch(e -> e.contains("nezkontroloval")));
    }

    @Test
    public void shntoolUnreadableWavKeepsProfileResult() throws Exception {
        // shntool 1 u WAV, který nepřečte (prázdný soubor ve vzorku gramodesek 0.3): profil hlásí jako dřív, kód je běžný
        fakeUtil("shntool", textFile(""), textFile("shntool [info]: warning: none of the builtin format modules handle input file: [x.wav]\n"), 1);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.SHNTOOL, fakeUtilsDir);
        // stejných 6 hlášek na soubor jako před 3.0 (CLI se skutečným shntool 3.0.10, audio_gram_0.3/1234567890, 23. 11. 2026)
        assertEquals(Arrays.asList(
                "WAVE format musí mít hodnotu '(Microsoft PCM)': kontrolovaná hodnota nenalezena",
                "Bits/sample musí mít hodnotu '24': kontrolovaná hodnota nenalezena",
                "Samples/sec musí mít hodnotu '96000': kontrolovaná hodnota nenalezena",
                "File is compressed musí mít hodnotu 'no': kontrolovaná hodnota nenalezena",
                "Inconsistent header musí mít hodnotu 'no': kontrolovaná hodnota nenalezena",
                "File probably truncated musí mít hodnotu 'no': kontrolovaná hodnota nenalezena"
        ), loadProfile(manager, "audio_gram_0.3", "sa", ExternalUtil.SHNTOOL).validate("wav", new File("x.wav")));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Windows: záporné kódy (uložený výsledek procesu)

    @Test
    public void windowsCrashCodesAreErrorOtherCodesKeepProfile() throws Exception {
        CliCommand.Result accessViolation = new CliCommand.Result(-1073741819, "", "");
        CliCommand.Result dllNotFound = new CliCommand.Result(-1073741515, "", "");
        assertEquals(Collections.singletonList("ERROR: nástroj Kakadu soubor nezkontroloval: skončil s kódem 0xC0000005 (pád nástroje), bez výstupu"),
                problems(OperatingSystem.WINDOWS, ExternalUtil.KAKADU, FDMF, "mc", accessViolation));
        assertEquals(Collections.singletonList("ERROR: nástroj ImageMagick soubor nezkontroloval: skončil s kódem 0xC0000135 (chybí knihovna DLL nástroje), bez výstupu"),
                problems(OperatingSystem.WINDOWS, ExternalUtil.IMAGE_MAGICK, FDMF, "mc", dllNotFound));
        // kdu_expand.exe při chybě -1 (ověřeno na Windows): profil hlásí chybu Kakadu jako dřív
        String damaged = output("kakadu", "detekce_poskozeni_w0352gUC.txt");
        assertEquals(Collections.singletonList("no_errors: zakázaná hodnota nalezena: Kakadu Core Error:\nExpected to find EPH marker following packet header.  Found 0x0 instead."),
                problems(OperatingSystem.WINDOWS, ExternalUtil.KAKADU, FDMF, "uc", new CliCommand.Result(-1, "", damaged)));
        // ImageMagick na Windows bez znaku zpracování (výstup neověřen): prázdný výstup s kódem 1 projde jako dřív
        assertEquals(Collections.emptyList(), problems(OperatingSystem.WINDOWS, ExternalUtil.IMAGE_MAGICK, FDMF, "mc", new CliCommand.Result(1, "", "")));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // všechny profily Kakadu a ImageMagicku nad uloženými skutečnými výstupy

    @Test
    public void everyKakaduProfileAcceptsSuccessfulRunOfEverySeries() throws Exception {
        List<String[]> profiles = fdmfCopies(ExternalUtil.KAKADU);
        assertEquals(78, profiles.size());
        List<String> outputs = storedOutputs("kakadu");
        // 7.9.1, 7.10.7, 8.0.3, 8.1, 8.2.1, 8.5, 8.6.2, 8.7 (Linux x86-64), 8.1, 8.5, 8.6.2, 8.7 (Linux arm64),
        // 8.5, 8.7 (macOS), 7.9.1, 8.5, 8.7 (Windows)
        assertEquals(17, outputs.size());
        for (OperatingSystem os : OperatingSystem.values()) {
            for (String name : outputs) {
                CliCommand.Result result = new CliCommand.Result(0, output("kakadu", "uspech_stdout/" + name), "");
                for (String[] profile : profiles) {
                    assertEquals(os + "/" + name + "/" + profile[0] + "/" + profile[1], Collections.emptyList(),
                            problems(os, ExternalUtil.KAKADU, profile[0], profile[1], result));
                }
            }
        }
    }

    @Test
    public void everyImageMagickProfileAcceptsSuccessfulRunOfEverySeries() throws Exception {
        List<String[]> profiles = fdmfCopies(ExternalUtil.IMAGE_MAGICK);
        assertEquals(78, profiles.size());
        List<String> outputs = storedOutputs("imageMagick");
        assertEquals(Arrays.asList("im69_ubuntu2404.txt", "im711_debian13.txt", "im712_macos.txt"), outputs);
        for (OperatingSystem os : OperatingSystem.values()) {
            for (String name : outputs) {
                CliCommand.Result result = new CliCommand.Result(0, output("imageMagick", "uspech_stdout/" + name), "");
                for (String[] profile : profiles) {
                    assertEquals(os + "/" + name + "/" + profile[0] + "/" + profile[1], Collections.emptyList(),
                            problems(os, ExternalUtil.IMAGE_MAGICK, profile[0], profile[1], result));
                }
            }
        }
    }

    @Test
    public void everyKakaduProfileReportsFailures() throws Exception {
        CliCommand.Result libraryNotLoaded = new CliCommand.Result(134, "", LIBRARY_NOT_LOADED);
        CliCommand.Result emptyOutput = new CliCommand.Result(0, "", "");
        CliCommand.Result damaged = new CliCommand.Result(255, "", output("kakadu", "detekce_poskozeni_w0352gUC.txt"));
        CliCommand.Result noCodestream = new CliCommand.Result(255, "", output("kakadu", "bez_kodoveho_proudu.txt"));
        CliCommand.Result cannotOpen = new CliCommand.Result(255, "", output("kakadu", "nelze_otevrit_neexistuje.txt"));
        for (String[] profile : fdmfCopies(ExternalUtil.KAKADU)) {
            for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.MAC, OperatingSystem.LINUX}) {
                String where = os + "/" + profile[0] + "/" + profile[1];
                assertEquals(where, Collections.singletonList("ERROR: " + KAKADU_NOT_CHECKED_134),
                        problems(os, ExternalUtil.KAKADU, profile[0], profile[1], libraryNotLoaded));
            }
            for (OperatingSystem os : OperatingSystem.values()) {
                String where = os + "/" + profile[0] + "/" + profile[1];
                assertEquals(where, Collections.singletonList("ERROR: " + KAKADU_NO_EVIDENCE),
                        problems(os, ExternalUtil.KAKADU, profile[0], profile[1], emptyOutput));
                assertEquals(where, Collections.singletonList("no_errors: zakázaná hodnota nalezena: Kakadu Core Error:\nExpected to find EPH marker following packet header.  Found 0x0 instead."),
                        problems(os, ExternalUtil.KAKADU, profile[0], profile[1], damaged));
                assertEquals(where, Collections.singletonList("ERROR: " + KAKADU_NO_CODESTREAM),
                        problems(os, ExternalUtil.KAKADU, profile[0], profile[1], noCodestream));
                List<String> cannotOpenProblems = problems(os, ExternalUtil.KAKADU, profile[0], profile[1], cannotOpen);
                assertEquals(where + ": " + cannotOpenProblems, 1, cannotOpenProblems.size());
                assertTrue(where + ": " + cannotOpenProblems, cannotOpenProblems.get(0).startsWith("ERROR: soubor nelze otevřít "));
            }
        }
    }

    @Test
    public void everyImageMagickProfileReportsFailures() throws Exception {
        CliCommand.Result segfault = new CliCommand.Result(139, "", "");
        CliCommand.Result silentFailure = new CliCommand.Result(1, "", "");
        CliCommand.Result damaged = new CliCommand.Result(1, "", output("imageMagick", "im69_path_detekce_poskozeni_w0352gUC.txt"));
        for (String[] profile : fdmfCopies(ExternalUtil.IMAGE_MAGICK)) {
            for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.MAC, OperatingSystem.LINUX}) {
                String where = os + "/" + profile[0] + "/" + profile[1];
                assertEquals(where, Collections.singletonList("ERROR: " + IMAGE_MAGICK_NOT_CHECKED_139),
                        problems(os, ExternalUtil.IMAGE_MAGICK, profile[0], profile[1], segfault));
                assertEquals(where, Collections.singletonList("ERROR: " + IMAGE_MAGICK_NO_EVIDENCE_1),
                        problems(os, ExternalUtil.IMAGE_MAGICK, profile[0], profile[1], silentFailure));
                List<String> damagedProblems = problems(os, ExternalUtil.IMAGE_MAGICK, profile[0], profile[1], damaged);
                assertEquals(where + ": " + damagedProblems, 6, damagedProblems.size());
                assertTrue(where + ": " + damagedProblems, damagedProblems.stream().allMatch(p -> p.startsWith("no_errors: zakázaná hodnota nalezena: identify-im6.q16: ")));
            }
        }
    }

    @Test
    public void kakaduOtherErrorsAreReportedOnce() throws Exception {
        // „Kakadu Error:“ (chyba aplikace) a „Error in Kakadu Compressed I/O Support:“ (syntetické, tvar podle řetězců
        // v kdu_expand 8.7): každá jedna chyba ERROR nové validace; spolu s Core Error dvě různé chyby
        String appError = "Kakadu Error:\nUnable to write output file.";
        String ioError = "Error in Kakadu Compressed I/O Support:\nRead failure.";
        String coreThenIo = "Kakadu Core Error:\nCode-stream must start with an SOC marker!\n" + ioError;
        String name = "chyba Kakadu při čtení souboru (soubor se nezkontroloval): zakázaná hodnota nalezena: ";
        assertEquals(Collections.singletonList("ERROR: " + name + appError),
                problems(OperatingSystem.LINUX, ExternalUtil.KAKADU, FDMF, "mc", new CliCommand.Result(255, "", appError + "\n")));
        assertEquals(Collections.singletonList("ERROR: " + name + ioError),
                problems(OperatingSystem.LINUX, ExternalUtil.KAKADU, FDMF, "mc", new CliCommand.Result(255, "", ioError + "\n")));
        assertEquals(Arrays.asList(
                        "no_errors: zakázaná hodnota nalezena: " + coreThenIo,
                        "ERROR: " + name + ioError),
                problems(OperatingSystem.LINUX, ExternalUtil.KAKADU, FDMF, "mc", new CliCommand.Result(255, "", coreThenIo + "\n")));
    }

    @Test
    public void outputExcerptIsShortened() throws Exception {
        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            longLine.append("abcdefghij");
        }
        String failure = handler(OperatingSystem.LINUX, ExternalUtil.JHOVE, "jp2k")
                .evaluateExecution(new CliCommand.Result(1, "stdout radek\n", "\n  " + longLine + "\n")).getFailure();
        // 300 znaků výstupu a výpustka; stdout za dlouhým stderr se už nevejde
        assertEquals("skončil s kódem 1 (neočekávaný kód, běžné jsou 0), výstup: " + longLine.substring(0, 300) + "…", failure);
        failure = handler(OperatingSystem.LINUX, ExternalUtil.JHOVE, "jp2k")
                .evaluateExecution(new CliCommand.Result(2, "na stdout\n\n", "na stderr\n")).getFailure();
        assertEquals("skončil s kódem 2 (neočekávaný kód, běžné jsou 0), výstup: na stderr na stdout", failure);
    }

    @Test
    public void cannotRunToolIsSingleError() throws Exception {
        // nástroj nejde spustit (cesta neexistuje): jedna chyba ERROR, text výjimky JDK se liší podle systému
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, new File(fakeUtilsDir, "neexistuje"));
        BinaryFileProfile profile = loadProfile(manager, FDMF, "mc", ExternalUtil.JHOVE);
        List<Validation.Problem> problems = profile.validateProblems("jp2k", DUMMY_FILE);
        assertEquals(problems.toString(), 1, problems.size());
        assertEquals(Level.ERROR, problems.get(0).getLevel());
        assertTrue(problems.get(0).getMessage(), problems.get(0).getMessage().startsWith("nástroj JHOVE soubor nezkontroloval: nejde spustit: Cannot run program \""));
    }
}
