package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilExecution;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Issue #104: soubory MASTER-COPY-AUDIO (adresar mastercopy_audio, v gramodeskach 0.3 master_copy_audio) se hledaji
 * jako *.wav a maji se kontrolovat stejnymi nastroji jako SOURCE-AUDIO, tedy JHOVE a shntool (spusteni "wav"), ne
 * nastroji pro MP3 (MP3val, Checkmate).
 * <p>
 * Podle DMF jsou archivni kopie zvuku „např. ve formátu WAV“ (DMF Zvuk 1.0 kap. 2 bod 2 a kap. 5.3, stejne
 * fonovalecky 0.3 a gramodesky 0.3 az 0.5), v hlavnim METS maji MIMETYPE audio/wav (DMF Zvuk 1.0 kap. 7.6.1)
 * a pro WAV plati audioDataEncoding PCM (kap. 7.5.4). Parametry (vzorkovaci frekvence, bitova hloubka) DMF uvadi jen
 * u SOURCE-AUDIO (96 kHz, 24 bit, kap. 2 bod 1), proto profil shntool pro MASTER-COPY-AUDIO hlasi odchylku
 * od 24 bit a 96 kHz jen jako WARNING, format PCM, kompresi, hlavicku a zkraceni jako ERROR.
 * <p>
 * Pravidla se ctou z konfigurace fDMF stejne jako v aplikaci (FdmfConfiguration, ValidatorFactory, Engine) nad
 * minimalnim balickem s jednim souborem MASTER-COPY-AUDIO. Bez nastroju se misto spusteni podstrci ulozeny vystup
 * (src/test/resources/externalUtils/jhove/wav_*.xml a shntool/wav_*.txt, viz README.md tamtez). Se skutecnymi
 * nastroji (JHOVE z -Djhove.path nebo PATH, shntool z -Dshntool.path nebo PATH) se kontroluje vygenerovany WAV;
 * kdyz nastroj chybi, je test preskoceny (Assume).
 */
public class AudioMasterCopyToolsTest {

    private static final String SECTION = "Zvuková data";
    private static final String RULE_PREFIX = "MASTER-COPY-AUDIO_FILES_VALID_BY_";
    private static final String RULE_JHOVE = "MASTER-COPY-AUDIO_FILES_VALID_BY_JHOVE";
    private static final String RULE_SHNTOOL = "MASTER-COPY-AUDIO_FILES_VALID_BY_SHNTOOL";
    private static final String PSP_ID = "1234567890";
    private static final String MCA_FILE_NAME = "mca_" + PSP_ID + "_0001.wav";
    private static final File INFO_FILE = new File("src/test/resources/audio_gram_0.3/1234567890/info_1234567890.xml");

    /**
     * Vsechny zvukove fDMF a nazev adresare MASTER-COPY-AUDIO-DIR podle DMF: gramodesky 0.3 „master_copy_audio“
     * (DMF Zvuk 0.3 kap. 2), ostatni „mastercopy_audio“ (prejmenovani od DMF 0.4, zmenovy list DMF Zvuk 1.0).
     */
    private static final Map<String, String> AUDIO_FDMF = new LinkedHashMap<>();

    static {
        AUDIO_FDMF.put("audio_disc_1.0", "mastercopy_audio");
        AUDIO_FDMF.put("audio_fono_0.3", "mastercopy_audio");
        AUDIO_FDMF.put("audio_fono_1.0", "mastercopy_audio");
        AUDIO_FDMF.put("audio_gram_0.3", "master_copy_audio");
        AUDIO_FDMF.put("audio_gram_0.4", "mastercopy_audio");
        AUDIO_FDMF.put("audio_gram_0.5", "mastercopy_audio");
        AUDIO_FDMF.put("audio_gram_1.0", "mastercopy_audio");
        AUDIO_FDMF.put("audio_no_carrier_1.0", "mastercopy_audio");
    }

    private static FdmfRegistry registry;
    private static ValidatorConfigurationManager configManager;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setup() throws Exception {
        // vlastni registry: initBinaryFileProfiles prepisuje BinaryFileValidator fDMF, sdilena konfigurace ostatnich testu
        // (FdmfTestSupport.sharedRegistry) tak zustane netknuta
        configManager = FdmfTestSupport.configManager();
        registry = new FdmfRegistry(configManager);
    }

    /**
     * Zaznam spusteni nastroju: "<nastroj>/<spusteni>/<nazev souboru>".
     */
    private static class RecordingManager extends CannedOutputExternalUtilManager {
        final List<String> executions = new ArrayList<>();

        RecordingManager() {
            for (ExternalUtil util : ExternalUtil.values()) {
                setUtilAvailable(util, true);
            }
        }

        @Override
        public String runUtilExecution(ExternalUtilExecution utilExecution, File targetFile) throws CliCommand.CliCommandException {
            executions.add(utilExecution.getUtil() + "/" + utilExecution.getName() + "/" + targetFile.getName());
            return super.runUtilExecution(utilExecution, targetFile);
        }
    }

    private File buildPackage(String mcaDirName, byte[] mcaContent) throws IOException {
        File root = new File(tmp.newFolder(), PSP_ID);
        assertTrue(root.mkdir());
        Files.copy(INFO_FILE.toPath(), new File(root, INFO_FILE.getName()).toPath());
        File mcaDir = new File(root, mcaDirName);
        assertTrue(mcaDir.mkdir());
        Files.write(new File(mcaDir, MCA_FILE_NAME).toPath(), mcaContent);
        return root;
    }

    /**
     * Pravidla sekce Zvukova data pro MASTER-COPY-AUDIO tak, jak je postavi aplikace z konfigurace fDMF.
     */
    private static List<nkp.pspValidator.shared.engine.Rule> masterCopyAudioRules(String fdmfDir, ExternalUtilManager manager, File pspRoot) throws Exception {
        FdmfConfiguration fdmfConfig = registry.getFdmfConfig(FdmfTestSupport.dmfFromDirName(fdmfDir));
        fdmfConfig.initBinaryFileProfiles(manager);
        Engine engine = ValidatorFactory.buildValidator(fdmfConfig, pspRoot, configManager.getDictionaryManager()).getEngine();
        RulesSection section = null;
        for (RulesSection s : engine.getRuleSections()) {
            if (SECTION.equals(s.getName())) {
                section = s;
            }
        }
        assertNotNull(fdmfDir + ": sekce " + SECTION, section);
        List<nkp.pspValidator.shared.engine.Rule> result = new ArrayList<>();
        for (nkp.pspValidator.shared.engine.Rule rule : engine.getRules(section)) {
            if (rule.getName().startsWith(RULE_PREFIX)) {
                result.add(rule);
            }
        }
        return result;
    }

    private static List<String> problems(nkp.pspValidator.shared.engine.Rule rule) {
        List<String> result = new ArrayList<>();
        for (ValidationProblem problem : rule.getResult().getProblems()) {
            result.add(problem.getLevel() + ": " + problem.getFullMessage());
        }
        return result;
    }

    /**
     * Problemy pravidel MASTER-COPY-AUDIO pri danem vystupu JHOVE a shntool (pravidlo -> problemy).
     */
    private Map<String, List<String>> problemsWithCannedOutputs(String fdmfDir, String jhoveOutput, String shntoolOutput,
                                                                List<String> executions) throws Exception {
        RecordingManager manager = new RecordingManager();
        manager.withOutput(ExternalUtil.JHOVE, ExternalUtilsTestSupport.output("jhove", jhoveOutput))
                .withOutput(ExternalUtil.SHNTOOL, ExternalUtilsTestSupport.output("shntool", shntoolOutput));
        File root = buildPackage(AUDIO_FDMF.get(fdmfDir), new byte[0]);
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (nkp.pspValidator.shared.engine.Rule rule : masterCopyAudioRules(fdmfDir, manager, root)) {
            result.put(rule.getName(), problems(rule));
        }
        assertEquals(fdmfDir, Arrays.asList(RULE_JHOVE, RULE_SHNTOOL), new ArrayList<>(result.keySet()));
        if (executions != null) {
            executions.addAll(manager.executions);
        }
        return result;
    }

    private static List<String> levels(List<String> problems) {
        List<String> result = new ArrayList<>();
        for (String problem : problems) {
            result.add(problem.substring(0, problem.indexOf(':')));
        }
        return result;
    }

    @Test
    public void allAudioFdmfAreCovered() {
        List<String> found = new ArrayList<>();
        for (File dir : FdmfTestSupport.fdmfDirs()) {
            if (dir.getName().startsWith("audio_")) {
                found.add(dir.getName());
            }
        }
        assertEquals(new ArrayList<>(AUDIO_FDMF.keySet()), found);
    }

    @Test
    public void masterCopyAudioCheckedByJhoveAndShntool() throws Exception {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            List<String> executions = new ArrayList<>();
            Map<String, List<String>> problems = problemsWithCannedOutputs(fdmfDir, "wav_ok_24bit_96kHz.xml", "wav_ok_24bit_96kHz.txt", executions);
            Map<String, List<String>> expected = new LinkedHashMap<>();
            expected.put(RULE_JHOVE, Collections.emptyList());
            expected.put(RULE_SHNTOOL, Collections.emptyList());
            assertEquals(fdmfDir, expected, problems);
            assertEquals(fdmfDir, Arrays.asList("JHOVE/wav/" + MCA_FILE_NAME, "SHNTOOL/wav/" + MCA_FILE_NAME), executions);
        }
    }

    @Test
    public void masterCopyAudioRuleDescriptionsNameWavAndTools() throws Exception {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            File root = buildPackage(AUDIO_FDMF.get(fdmfDir), new byte[0]);
            List<nkp.pspValidator.shared.engine.Rule> rules = masterCopyAudioRules(fdmfDir, new RecordingManager(), root);
            assertEquals(fdmfDir, 2, rules.size());
            assertEquals(fdmfDir, "Všechny soubory MASTER-COPY-AUDIO (wav) musí být validní vůči profilu pro binární soubory pro nástroj JHOVE.",
                    rules.get(0).getDescription().trim());
            assertEquals(fdmfDir, "Všechny soubory MASTER-COPY-AUDIO (wav) musí být validní vůči profilu pro binární soubory pro nástroj shntool.",
                    rules.get(1).getDescription().trim());
        }
    }

    @Test
    public void masterCopyAudioProfilesForMp3Removed() {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            assertTrue(fdmfDir, ExternalUtilsTestSupport.profileFile(fdmfDir, "mca", ExternalUtil.JHOVE).isFile());
            assertTrue(fdmfDir, ExternalUtilsTestSupport.profileFile(fdmfDir, "mca", ExternalUtil.SHNTOOL).isFile());
            assertFalse(fdmfDir, ExternalUtilsTestSupport.profileFile(fdmfDir, "mca", ExternalUtil.MP3VAL).exists());
            assertFalse(fdmfDir, ExternalUtilsTestSupport.profileFile(fdmfDir, "mca", ExternalUtil.CHECKMATE).exists());
            // uzivatelska kopie (MP3) zustava u MP3val a Checkmate
            assertTrue(fdmfDir, ExternalUtilsTestSupport.profileFile(fdmfDir, "uca", ExternalUtil.MP3VAL).isFile());
            assertTrue(fdmfDir, ExternalUtilsTestSupport.profileFile(fdmfDir, "uca", ExternalUtil.CHECKMATE).isFile());
        }
    }

    @Test
    public void masterCopyAudioWithoutDirIsInfoOnly() throws Exception {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            // adresar je nepovinny (DMF Zvuk 1.0 kap. 2 bod 2 „ne/povinné“, kap. 5.3)
            File root = tmp.newFolder(fdmfDir, PSP_ID);
            Files.copy(INFO_FILE.toPath(), new File(root, INFO_FILE.getName()).toPath());
            RecordingManager manager = new RecordingManager();
            Map<String, List<String>> problems = new LinkedHashMap<>();
            for (nkp.pspValidator.shared.engine.Rule rule : masterCopyAudioRules(fdmfDir, manager, root)) {
                problems.put(rule.getName(), problems(rule));
            }
            Map<String, List<String>> expected = new LinkedHashMap<>();
            expected.put(RULE_JHOVE, Collections.singletonList("INFO: prázdný seznam souborů"));
            expected.put(RULE_SHNTOOL, Collections.singletonList("INFO: prázdný seznam souborů"));
            assertEquals(fdmfDir, expected, problems);
            assertEquals(fdmfDir, Collections.emptyList(), manager.executions);
        }
    }

    @Test
    public void truncatedWavIsError() throws Exception {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            Map<String, List<String>> problems = problemsWithCannedOutputs(fdmfDir, "wav_truncated.xml", "wav_truncated.txt", null);
            List<String> jhove = problems.get(RULE_JHOVE);
            assertEquals(fdmfDir + ": " + jhove, 1, jhove.size());
            assertTrue(fdmfDir + ": " + jhove, jhove.get(0).startsWith("ERROR: Status: ") && jhove.get(0).contains("Not well-formed"));
            List<String> shntool = problems.get(RULE_SHNTOOL);
            assertEquals(fdmfDir + ": " + shntool, 1, shntool.size());
            assertTrue(fdmfDir + ": " + shntool, shntool.get(0).startsWith("ERROR: File probably truncated musí mít hodnotu 'no'")
                    && shntool.get(0).contains("yes (missing 34953334 bytes)"));
        }
    }

    @Test
    public void mp3InMasterCopyAudioIsError() throws Exception {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            Map<String, List<String>> problems = problemsWithCannedOutputs(fdmfDir, "wav_mp3_renamed.xml", "wav_mp3_renamed.txt", null);
            // JHOVE: status Not well-formed, format chybi
            assertEquals(fdmfDir + ": " + problems, Arrays.asList("ERROR", "ERROR"), levels(problems.get(RULE_JHOVE)));
            assertTrue(fdmfDir + ": " + problems, problems.get(RULE_JHOVE).get(0).startsWith("ERROR: Status: ")
                    && problems.get(RULE_JHOVE).get(0).contains("Not well-formed"));
            assertTrue(fdmfDir + ": " + problems, problems.get(RULE_JHOVE).get(1).startsWith("ERROR: Identifikace formátu: "));
            // shntool soubor neprecte (prazdny STDOUT): zadna z 6 kontrolovanych hodnot neni nalezena
            assertEquals(fdmfDir + ": " + problems, Arrays.asList("ERROR", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR"), levels(problems.get(RULE_SHNTOOL)));
        }
    }

    @Test
    public void otherSampleRateAndBitDepthIsWarningOnly() throws Exception {
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            Map<String, List<String>> problems = problemsWithCannedOutputs(fdmfDir, "wav_16bit_44kHz.xml", "wav_16bit_44kHz.txt", null);
            assertEquals(fdmfDir, Collections.emptyList(), problems.get(RULE_JHOVE));
            List<String> shntool = problems.get(RULE_SHNTOOL);
            assertEquals(fdmfDir + ": " + shntool, 2, shntool.size());
            assertTrue(fdmfDir + ": " + shntool, shntool.get(0).startsWith("WARNING: Bits/sample by měl mít hodnotu '24'")
                    && shntool.get(0).contains("16"));
            assertTrue(fdmfDir + ": " + shntool, shntool.get(1).startsWith("WARNING: Samples/sec by měl mít hodnotu '96000'")
                    && shntool.get(1).contains("44100"));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // se skutecnymi nastroji

    /**
     * WAV PCM little endian, mono; dataSizeInHeader &gt; skutecna delka dat dela zkraceny soubor.
     */
    private static byte[] wav(int sampleRate, int bitsPerSample, int frames, int dataSizeInHeader) {
        int blockAlign = bitsPerSample / 8;
        int dataSize = frames * blockAlign;
        ByteBuffer b = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataSizeInHeader);
        b.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        b.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(sampleRate).putInt(sampleRate * blockAlign).putShort((short) blockAlign).putShort((short) bitsPerSample);
        b.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataSizeInHeader);
        for (int i = 0; i < dataSize; i++) {
            b.put((byte) (i % 7));
        }
        return b.array();
    }

    private static ExternalUtilManager realToolsManager() throws Exception {
        ExternalUtilManager manager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            manager.setUtilAvailable(util, false);
        }
        String[][] tools = {{"JHOVE", "jhove.path"}, {"SHNTOOL", "shntool.path"}};
        for (String[] tool : tools) {
            ExternalUtil util = ExternalUtil.valueOf(tool[0]);
            String path = System.getProperty(tool[1]);
            if (path != null && !path.trim().isEmpty()) {
                manager.setPath(util, new File(path.trim()));
            }
            try {
                manager.runUtilVersionDetection(util);
                manager.setUtilAvailable(util, true);
            } catch (CliCommand.CliCommandException e) {
                Assume.assumeTrue(util.getUserFriendlyName() + " není dostupný (" + e.getMessage() + "), nastav PATH nebo -D" + tool[1], false);
            }
        }
        return manager;
    }

    private Map<String, List<String>> problemsWithRealTools(String fdmfDir, byte[] wav) throws Exception {
        ExternalUtilManager manager = realToolsManager();
        File root = buildPackage(AUDIO_FDMF.get(fdmfDir), wav);
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (nkp.pspValidator.shared.engine.Rule rule : masterCopyAudioRules(fdmfDir, manager, root)) {
            result.put(rule.getName(), problems(rule));
        }
        assertEquals(fdmfDir, Arrays.asList(RULE_JHOVE, RULE_SHNTOOL), new ArrayList<>(result.keySet()));
        return result;
    }

    @Test
    public void realToolsValidWavHasNoProblems() throws Exception {
        byte[] wav = wav(96000, 24, 9600, 9600 * 3);
        for (String fdmfDir : AUDIO_FDMF.keySet()) {
            Map<String, List<String>> expected = new LinkedHashMap<>();
            expected.put(RULE_JHOVE, Collections.emptyList());
            expected.put(RULE_SHNTOOL, Collections.emptyList());
            assertEquals(fdmfDir, expected, problemsWithRealTools(fdmfDir, wav));
        }
    }

    @Test
    public void realToolsTruncatedWavIsError() throws Exception {
        // hlavicka slibuje 2x vic dat, nez soubor obsahuje
        byte[] wav = wav(96000, 24, 9600, 2 * 9600 * 3);
        Map<String, List<String>> problems = problemsWithRealTools("audio_gram_1.0", wav);
        assertEquals(problems.toString(), Collections.singletonList("ERROR"), levels(problems.get(RULE_JHOVE)));
        assertTrue(problems.toString(), problems.get(RULE_JHOVE).get(0).contains("Not well-formed"));
        assertEquals(problems.toString(), Collections.singletonList("ERROR"), levels(problems.get(RULE_SHNTOOL)));
        assertTrue(problems.toString(), problems.get(RULE_SHNTOOL).get(0).startsWith("ERROR: File probably truncated musí mít hodnotu 'no'"));
    }

    @Test
    public void realToolsOtherParametersAreWarnings() throws Exception {
        byte[] wav = wav(44100, 16, 4410, 4410 * 2);
        Map<String, List<String>> problems = problemsWithRealTools("audio_fono_0.3", wav);
        assertEquals(problems.toString(), Collections.emptyList(), problems.get(RULE_JHOVE));
        assertEquals(problems.toString(), Arrays.asList("WARNING", "WARNING"), levels(problems.get(RULE_SHNTOOL)));
    }
}
