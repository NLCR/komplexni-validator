package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.loadProfile;
import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.output;
import static org.junit.Assert.*;

/**
 * Obrazové kopie zvukových dokumentů: kontroly, které DMF zvukových dokumentů formuluje jen jako doporučení, jsou
 * v profilech Jpylyzeru a JHOVE všech osmi audio fDMF úrovně WARNING s názvem „… (doporučení DMF)“:
 * <ul>
 *     <li>rozlišení (DMF kap. 2, bod 4: „doporučené rozlišení je 300 PPI a více“; minimum 300 PPI jen pro knihy
 *     a periodika), včetně chybějícího údaje o rozlišení,</li>
 *     <li>parametry kódování JPEG 2000 (DMF kap. 5.5: „Produkce obrazových dat by se měla řídit doporučenými
 *     standardy“): dlaždice, precincts, SOP, EPH, pořadí progrese, vrstvy, úrovně, code block, bypass, kompresní
 *     poměr UC.</li>
 * </ul>
 * Platnost souboru (Valid), barevná hloubka, transformace a profily ne-audio fDMF se nemění (ERROR z rules.xml).
 * <p>
 * Výstupy nástrojů jsou skutečné výstupy Jpylyzeru 2.2.1 a JHOVE 1.34.0 nad oficiálními vzorky NK
 * (src/test/resources/externalUtils/zvukove-dokumenty/README.md). Očekávané hodnoty jsou určené ručně z hodnot
 * ve výstupech nástroje a z omezení profilu.
 */
public class AudioImageRecommendationLevelsTest {

    private static final List<String> AUDIO_FDMF = Arrays.asList(
            "audio_gram_0.3", "audio_gram_0.4", "audio_gram_0.5", "audio_gram_1.0",
            "audio_fono_0.3", "audio_fono_1.0", "audio_disc_1.0", "audio_no_carrier_1.0");
    private static final String NON_AUDIO_FDMF = "monograph_2.2";
    private static final String DIR = "zvukove-dokumenty";
    private static final File DUMMY_FILE = new File("src/test/tools/jp2/mc.jp2");
    private static final String S = " (doporučení DMF)";

    private Locale originalLocale;

    @Before
    public void setLocale() {
        // mez isInFloatRange se v hlasce zapisuje s carkou nezavisle na Locale (nalez N4)
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    // Rozliseni (skupiny S2 a S3)

    @Test
    public void gramophoneResolution180PpiIsWarningInAllAudioFdmf() throws Exception {
        // gramodeska 0.5, mc_1234567890_0001.jp2: capture resolution 7086.61 px/m (180 PPI), ostatni parametry
        // odpovidaji profilu MC; stejne pro UC (dlazdice 1024, 12 vrstev, kompresni pomer 11.98)
        List<String> expected = Arrays.asList(
                "WARNING: Rozlišení vertik." + S + ": hodnota \"7086.61\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]",
                "WARNING: Rozlišení horiz." + S + ": hodnota \"7086.61\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]");
        for (String fdmf : AUDIO_FDMF) {
            assertEquals(fdmf + " mc", expected, problems(fdmf, "mc", ExternalUtil.JPYLYZER, "jpylyzer_gram05_mc_0001.xml"));
            assertEquals(fdmf + " uc", expected, problems(fdmf, "uc", ExternalUtil.JPYLYZER, "jpylyzer_gram05_uc_0001.xml"));
        }
    }

    @Test
    public void gramophoneResolution180PpiStaysErrorInMonograph() throws Exception {
        assertRulesXmlLevelIsError(NON_AUDIO_FDMF, "MASTER-COPY_FILES_VALID_BY_JPYLYZER");
        assertRulesXmlLevelIsError(NON_AUDIO_FDMF, "USER-COPY_FILES_VALID_BY_JPYLYZER");
        List<String> expected = Arrays.asList(
                "ERROR: Rozlišení vertik.: hodnota \"7086.61\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]",
                "ERROR: Rozlišení horiz.: hodnota \"7086.61\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]");
        assertEquals(expected, problems(NON_AUDIO_FDMF, "mc", ExternalUtil.JPYLYZER, "jpylyzer_gram05_mc_0001.xml"));
        assertEquals(expected, problems(NON_AUDIO_FDMF, "uc", ExternalUtil.JPYLYZER, "jpylyzer_gram05_uc_0001.xml"));
    }

    @Test
    public void resolutionBoundaryInAudioFdmf() throws Exception {
        // mez profilu 11732 px/m (297,99 PPI), presne 300 PPI je 11811,02 px/m; mez se nemeni
        String original = output(DIR, "jpylyzer_gram05_mc_0001.xml");
        assertTrue(original.contains("<hRescInPixelsPerMeter>7086.61</hRescInPixelsPerMeter>"));
        for (String fdmf : AUDIO_FDMF) {
            assertEquals(fdmf, Collections.emptyList(), problemsForOutput(fdmf, "mc", withResolution(original, "11732.0")));
            assertEquals(fdmf, Collections.emptyList(), problemsForOutput(fdmf, "mc", withResolution(original, "11811.02")));
            assertEquals(fdmf, Arrays.asList(
                    "WARNING: Rozlišení vertik." + S + ": hodnota \"11731.99\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]",
                    "WARNING: Rozlišení horiz." + S + ": hodnota \"11731.99\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]"
            ), problemsForOutput(fdmf, "mc", withResolution(original, "11731.99")));
        }
    }

    @Test
    public void resolutionBoundaryInMonograph() throws Exception {
        String original = output(DIR, "jpylyzer_gram05_mc_0001.xml");
        assertEquals(Collections.emptyList(), problemsForOutput(NON_AUDIO_FDMF, "mc", withResolution(original, "11732.0")));
        assertEquals(Arrays.asList(
                "ERROR: Rozlišení vertik.: hodnota \"11731.99\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]",
                "ERROR: Rozlišení horiz.: hodnota \"11731.99\" neodpovídá žádnému z omezení: [musí být alespoň 11732,000000]"
        ), problemsForOutput(NON_AUDIO_FDMF, "mc", withResolution(original, "11731.99")));
    }

    // Parametry JPEG 2000 (skupina S1) a chybejici udaj o rozliseni (S3)

    @Test
    public void cylinderJpylyzerMcIsWarningsOnlyInAllAudioFdmf() throws Exception {
        // fonovalecky 0.3, mc_…_0002.jp2: bez resolution boxu, jedna dlazdice 5000 x 3333, precincts default
        // (8x 32768), SOP a EPH no, LRCP, 1 vrstva (MC chce 1), 7 urovni, bypass no; code block 64, 5-3 reversible
        List<String> expected = Arrays.asList(
                "WARNING: Rozlišení vertik." + S + ": kontrolovaná hodnota nenalezena",
                "WARNING: Rozlišení horiz." + S + ": kontrolovaná hodnota nenalezena",
                "WARNING: Velikost dlaždic (1)" + S + ": hodnota \"5000\" neodpovídá žádnému z omezení: [musí být přesně '4096']",
                "WARNING: Velikost dlaždic (2)" + S + ": hodnota \"3333\" neodpovídá žádnému z omezení: [musí být přesně '4096']",
                "WARNING: Precincts (regiony)" + S + ": hodnota \"default\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes',musí být přesně 'user defined']",
                "WARNING: Velikost precincts (1)" + S + ": hodnota \"" + PRECINCTS_DEFAULT + "\" neodpovídá žádnému z omezení: [" + PRECINCTS_SPEC + "]",
                "WARNING: Velikost precincts (2)" + S + ": hodnota \"" + PRECINCTS_DEFAULT + "\" neodpovídá žádnému z omezení: [" + PRECINCTS_SPEC + "]",
                "WARNING: Sop" + S + ": hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes']",
                "WARNING: Eph" + S + ": hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes',musí být přesně 'YES']",
                "WARNING: Order" + S + ": hodnota \"LRCP\" neodpovídá žádnému z omezení: [musí být přesně 'RPCL']",
                "WARNING: Levels" + S + ": hodnota \"7\" neodpovídá žádnému z omezení: [musí být přesně '5',musí být přesně '6']",
                "WARNING: Bypass" + S + ": hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes']");
        for (String fdmf : AUDIO_FDMF) {
            assertEquals(fdmf, expected, problems(fdmf, "mc", ExternalUtil.JPYLYZER, "jpylyzer_fono_mc_0002.xml"));
        }
    }

    @Test
    public void cylinderJpylyzerUcIsWarningsOnlyInAllAudioFdmf() throws Exception {
        // uc_…_0002.jp2: jako MC, ale UC profil chce dlazdice 1024 a 12 vrstev, SOP a EPH nekontroluje,
        // 9-7 irreversible a kompresni pomer 46.15 (alespon 6) odpovidaji
        List<String> expected = Arrays.asList(
                "WARNING: Rozlišení vertik." + S + ": kontrolovaná hodnota nenalezena",
                "WARNING: Rozlišení horiz." + S + ": kontrolovaná hodnota nenalezena",
                "WARNING: Velikost dlaždic (1)" + S + ": hodnota \"5000\" neodpovídá žádnému z omezení: [musí být přesně '1024']",
                "WARNING: Velikost dlaždic (2)" + S + ": hodnota \"3333\" neodpovídá žádnému z omezení: [musí být přesně '1024']",
                "WARNING: Precincts (regiony)" + S + ": hodnota \"default\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes',musí být přesně 'user defined']",
                "WARNING: Velikost precincts (1)" + S + ": hodnota \"" + PRECINCTS_DEFAULT + "\" neodpovídá žádnému z omezení: [" + PRECINCTS_SPEC + "]",
                "WARNING: Velikost precincts (2)" + S + ": hodnota \"" + PRECINCTS_DEFAULT + "\" neodpovídá žádnému z omezení: [" + PRECINCTS_SPEC + "]",
                "WARNING: Order" + S + ": hodnota \"LRCP\" neodpovídá žádnému z omezení: [musí být přesně 'RPCL']",
                "WARNING: Layers" + S + ": hodnota \"1\" neodpovídá žádnému z omezení: [musí být přesně '12']",
                "WARNING: Levels" + S + ": hodnota \"7\" neodpovídá žádnému z omezení: [musí být přesně '5',musí být přesně '6']",
                "WARNING: Bypass" + S + ": hodnota \"no\" neodpovídá žádnému z omezení: [musí být přesně 'Yes',musí být přesně 'yes']");
        for (String fdmf : AUDIO_FDMF) {
            assertEquals(fdmf, expected, problems(fdmf, "uc", ExternalUtil.JPYLYZER, "jpylyzer_fono_uc_0002.xml"));
        }
    }

    @Test
    public void cylinderJhoveIsWarningsOnlyInAllAudioFdmf() throws Exception {
        // JHOVE: XTSize 5000, YTSize 3333, ProgressionOrder 0 (LRCP), 1 vrstva, 7 urovni; rozliseni JHOVE profil
        // nekontroluje (pravidla v komentari)
        List<String> expectedMc = Arrays.asList(
                "WARNING: Velikost dlaždic (1)" + S + ": hodnota \"5000\" neodpovídá žádnému z omezení: [musí být přesně '4096']",
                "WARNING: Velikost dlaždic (2)" + S + ": hodnota \"3333\" neodpovídá žádnému z omezení: [musí být přesně '4096']",
                "WARNING: Order" + S + ": hodnota \"0\" neodpovídá žádnému z omezení: [musí být přesně '2']",
                "WARNING: Levels" + S + ": hodnota \"7\" neodpovídá žádnému z omezení: [musí být přesně '5',musí být přesně '6']");
        List<String> expectedUc = Arrays.asList(
                "WARNING: Velikost dlaždic (1)" + S + ": hodnota \"5000\" neodpovídá žádnému z omezení: [musí být přesně '1024']",
                "WARNING: Velikost dlaždic (2)" + S + ": hodnota \"3333\" neodpovídá žádnému z omezení: [musí být přesně '1024']",
                "WARNING: Order" + S + ": hodnota \"0\" neodpovídá žádnému z omezení: [musí být přesně '2']",
                "WARNING: Layers" + S + ": hodnota \"1\" neodpovídá žádnému z omezení: [musí být přesně '12']",
                "WARNING: Levels" + S + ": hodnota \"7\" neodpovídá žádnému z omezení: [musí být přesně '5',musí být přesně '6']");
        for (String fdmf : AUDIO_FDMF) {
            assertEquals(fdmf, expectedMc, problems(fdmf, "mc", ExternalUtil.JHOVE, "jhove_fono_mc_0002.xml"));
            assertEquals(fdmf, expectedUc, problems(fdmf, "uc", ExternalUtil.JHOVE, "jhove_fono_uc_0002.xml"));
            // gramodeska: JHOVE nic nehlasi (rozliseni JHOVE profil nekontroluje, parametry odpovidaji)
            assertEquals(fdmf, Collections.emptyList(), problems(fdmf, "mc", ExternalUtil.JHOVE, "jhove_gram05_mc_0001.xml"));
            assertEquals(fdmf, Collections.emptyList(), problems(fdmf, "uc", ExternalUtil.JHOVE, "jhove_gram05_uc_0001.xml"));
        }
    }

    @Test
    public void cylinderParametersStayErrorInMonograph() throws Exception {
        assertRulesXmlLevelIsError(NON_AUDIO_FDMF, "MASTER-COPY_FILES_VALID_BY_JHOVE");
        assertEquals(Arrays.asList(
                "ERROR: Velikost dlaždic (1): hodnota \"5000\" neodpovídá žádnému z omezení: [musí být přesně '4096']",
                "ERROR: Velikost dlaždic (2): hodnota \"3333\" neodpovídá žádnému z omezení: [musí být přesně '4096']",
                "ERROR: Order: hodnota \"0\" neodpovídá žádnému z omezení: [musí být přesně '2']",
                "ERROR: Levels: hodnota \"7\" neodpovídá žádnému z omezení: [musí být přesně '5',musí být přesně '6']"
        ), problems(NON_AUDIO_FDMF, "mc", ExternalUtil.JHOVE, "jhove_fono_mc_0002.xml"));
        List<String> jpylyzer = problems(NON_AUDIO_FDMF, "mc", ExternalUtil.JPYLYZER, "jpylyzer_fono_mc_0002.xml");
        assertEquals(jpylyzer.toString(), 12, jpylyzer.size());
        assertEquals("ERROR: Rozlišení vertik.: kontrolovaná hodnota nenalezena", jpylyzer.get(0));
        assertEquals("ERROR: Order: hodnota \"LRCP\" neodpovídá žádnému z omezení: [musí být přesně 'RPCL']", jpylyzer.get(9));
        for (String problem : jpylyzer) {
            assertTrue(problem, problem.startsWith("ERROR: ") && !problem.contains(S));
        }
    }

    // Platnost souboru se nemeni

    @Test
    public void invalidFileStaysErrorInAudioFdmf() throws Exception {
        // prazdny soubor (jpylyzer/nevalidni_u5463f.xml): Valid False a vsechny vlastnosti chybi
        Set<String> recommendations = new HashSet<>(Arrays.asList(
                "Rozlišení vertik.", "Rozlišení horiz.", "Velikost dlaždic (1)", "Velikost dlaždic (2)", "Precincts (regiony)",
                "Velikost precincts (1)", "Velikost precincts (2)", "Sop", "Eph", "Order", "Layers", "Levels",
                "Code block width", "Code block height", "Bypass"));
        for (String fdmf : AUDIO_FDMF) {
            assertRulesXmlLevelIsError(fdmf, "MASTER-COPY_FILES_VALID_BY_JPYLYZER");
            List<String> problems = problemsForToolOutput(fdmf, "mc", ExternalUtil.JPYLYZER, output("jpylyzer", "nevalidni_u5463f.xml"));
            assertEquals(fdmf, "ERROR: Valid: hodnota \"False\" neodpovídá žádnému z omezení: [musí být přesně 'True']", problems.get(0));
            assertEquals(fdmf + " " + problems, 24, problems.size());
            int warnings = 0;
            for (String problem : problems.subList(1, problems.size())) {
                String name = problem.substring(problem.indexOf(' ') + 1, problem.indexOf(": kontrolovaná hodnota nenalezena"));
                if (problem.startsWith("WARNING: ")) {
                    assertTrue(problem, name.endsWith(S) && recommendations.contains(name.substring(0, name.length() - S.length())));
                    warnings++;
                } else {
                    assertTrue(problem, problem.startsWith("ERROR: ") && !recommendations.contains(name));
                }
            }
            assertEquals(fdmf, recommendations.size(), warnings);
        }
    }

    @Test
    public void auditOfChangedValidationsInAudioProfiles() throws Exception {
        // v kazdem audio profilu Jpylyzeru a JHOVE jsou WARNING presne validace s nazvem "… (doporučení DMF)"
        // a obe jejich pravidla; ostatni pravidla atribut level nemaji (uroven z rules.xml)
        Map<String, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put("mc/jpylyzer", 15);
        expectedCounts.put("uc/jpylyzer", 14);
        expectedCounts.put("mc/jhove", 7);
        expectedCounts.put("uc/jhove", 7);
        for (String fdmf : AUDIO_FDMF) {
            for (String key : expectedCounts.keySet()) {
                File file = new File(ExternalUtilsTestSupport.FDMF_DIR, fdmf + "/binaryFileProfiles/" + key + ".xml");
                Document doc = parse(file);
                NodeList validations = doc.getElementsByTagNameNS("*", "validation");
                int recommended = 0;
                for (int i = 0; i < validations.getLength(); i++) {
                    Element validation = (Element) validations.item(i);
                    boolean isRecommendation = validation.getAttribute("name").endsWith(S);
                    if (isRecommendation) {
                        recommended++;
                    }
                    NodeList rules = ((Element) validation.getElementsByTagNameNS("*", "rules").item(0)).getChildNodes();
                    for (int j = 0; j < rules.getLength(); j++) {
                        if (rules.item(j) instanceof Element) {
                            Element rule = (Element) rules.item(j);
                            assertEquals(file + " " + validation.getAttribute("name"), isRecommendation ? "WARNING" : "",
                                    rule.getAttribute("level"));
                        }
                    }
                }
                assertEquals(file.getPath(), expectedCounts.get(key).intValue(), recommended);
            }
        }
    }

    private static final String PRECINCTS_DEFAULT = "[32768,32768,32768,32768,32768,32768,32768,32768]";
    private static final String PRECINCTS_SPEC = "musí obsahovat nejvýše 2× '256', ostatní hodnoty '128'";

    private static String withResolution(String jpylyzerOutput, String pixelsPerMeter) {
        return jpylyzerOutput
                .replace("<vRescInPixelsPerMeter>7086.61</vRescInPixelsPerMeter>", "<vRescInPixelsPerMeter>" + pixelsPerMeter + "</vRescInPixelsPerMeter>")
                .replace("<hRescInPixelsPerMeter>7086.61</hRescInPixelsPerMeter>", "<hRescInPixelsPerMeter>" + pixelsPerMeter + "</hRescInPixelsPerMeter>");
    }

    private static List<String> problems(String fdmf, String copy, ExternalUtil util, String outputName) throws Exception {
        return problemsForToolOutput(fdmf, copy, util, output(DIR, outputName));
    }

    private static List<String> problemsForOutput(String fdmf, String copy, String jpylyzerOutput) throws Exception {
        return problemsForToolOutput(fdmf, copy, ExternalUtil.JPYLYZER, jpylyzerOutput);
    }

    /**
     * Úroveň a text problémů; úroveň null (z rules.xml) se zapíše jako ERROR, protože pravidla Jpylyzeru a JHOVE
     * v rules.xml validation_problem_level nemají (ověřuje {@link #assertRulesXmlLevelIsError}).
     */
    private static List<String> problemsForToolOutput(String fdmf, String copy, ExternalUtil util, String toolOutput) throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(util, toolOutput);
        BinaryFileProfile profile = loadProfile(manager, fdmf, copy, util);
        List<String> result = new ArrayList<>();
        for (Validation.Problem problem : profile.validateProblems("jp2k", DUMMY_FILE)) {
            Level level = problem.getLevel() == null ? Level.ERROR : problem.getLevel();
            result.add(level + ": " + problem.getMessage());
        }
        return result;
    }

    private static void assertRulesXmlLevelIsError(String fdmf, String ruleName) throws Exception {
        File rulesFile = new File(ExternalUtilsTestSupport.FDMF_DIR, fdmf + "/rules.xml");
        Document doc = parse(rulesFile);
        NodeList rules = doc.getElementsByTagNameNS("*", "rule");
        for (int i = 0; i < rules.getLength(); i++) {
            Element rule = (Element) rules.item(i);
            if (ruleName.equals(rule.getAttribute("name"))) {
                NodeList values = rule.getElementsByTagNameNS("*", "value");
                for (int j = 0; j < values.getLength(); j++) {
                    assertNotEquals(fdmf + " " + ruleName, "validation_problem_level", ((Element) values.item(j)).getAttribute("name"));
                }
                return;
            }
        }
        fail("pravidlo " + ruleName + " v " + rulesFile + " chybí");
    }

    private static Document parse(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file);
    }
}
