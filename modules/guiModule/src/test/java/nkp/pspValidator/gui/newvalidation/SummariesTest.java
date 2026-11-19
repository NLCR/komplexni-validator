package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.*;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Texty dialogu Nová validace (karta rozpoznání, souhrny sbalených sekcí) a určení verze, proti které se bude validovat.
 */
public class SummariesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;
    private VersionSettings versions;

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("cfg"));
        registry = GuiTestSupport.registry();
        versions = new VersionSettings(config, registry);
    }

    @Test
    public void recognizedWithVersionFromPackage() {
        Recognition r = Recognition.recognized(Dmf.Type.FUND_UNIT, "0.2", null);
        assertEquals("Rozpoznáno v balíku", Summaries.recognitionTitle(r));
        assertEquals("Jednotky fondu 0.2", Summaries.recognitionBadge(r));
        assertEquals("Typ podle METS, verze podle info.xml. Validovat se bude proti této verzi.", Summaries.recognitionDetail(r, versions));
        assertEquals("podle balíku, nic není vynuceno", Summaries.versionsSummary(r, versions));
        assertEquals(new Dmf(Dmf.Type.FUND_UNIT, "0.2"), Summaries.effectiveDmf(r, versions));
    }

    @Test
    public void forcedVersionWins() {
        versions.setForcedVersion(Dmf.Type.FUND_UNIT, "0.1");
        Recognition r = Recognition.recognized(Dmf.Type.FUND_UNIT, "0.2", null);
        assertEquals("Typ podle METS, verze podle info.xml. Vynucena verze 0.1, validovat se bude proti ní.", Summaries.recognitionDetail(r, versions));
        assertEquals("vynucena verze 0.1", Summaries.versionsSummary(r, versions));
        assertEquals(new Dmf(Dmf.Type.FUND_UNIT, "0.1"), Summaries.effectiveDmf(r, versions));
    }

    @Test
    public void missingVersionUsesPreferredOrDefault() {
        Recognition r = Recognition.recognized(Dmf.Type.MONOGRAPH, null, null);
        assertEquals("Monografie, verze neuvedena", Summaries.recognitionBadge(r));
        assertEquals("Typ podle METS, balík verzi neuvádí. Použije se výchozí verze 2.3.", Summaries.recognitionDetail(r, versions));
        assertEquals(new Dmf(Dmf.Type.MONOGRAPH, "2.3"), Summaries.effectiveDmf(r, versions));
        config.setBoolean(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, true);
        config.setString(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, "1.4");
        assertEquals("Typ podle METS, balík verzi neuvádí. Použije se preferovaná verze 1.4.", Summaries.recognitionDetail(r, versions));
        assertEquals(new Dmf(Dmf.Type.MONOGRAPH, "1.4"), Summaries.effectiveDmf(r, versions));
        assertEquals("1.4", versions.versionWhenMissing(Dmf.Type.MONOGRAPH));
    }

    @Test
    public void unreadableVersionEndsWithErrorUnlessForced() {
        Recognition r = Recognition.recognized(Dmf.Type.PERIODICAL, null, "nenalezen soubor INFO pro zjištění verze standardu DMF");
        assertEquals("Typ podle METS. Verzi nelze zjistit: nenalezen soubor INFO pro zjištění verze standardu DMF. Bez vynucené verze validace skončí chybou.",
                Summaries.recognitionDetail(r, versions));
        assertNull(Summaries.effectiveDmf(r, versions));
        versions.setForcedVersion(Dmf.Type.PERIODICAL, "2.2");
        assertEquals(new Dmf(Dmf.Type.PERIODICAL, "2.2"), Summaries.effectiveDmf(r, versions));
    }

    @Test
    public void versionWithoutFdmf() {
        Recognition r = Recognition.recognized(Dmf.Type.FUND_UNIT, "9.9", null);
        assertTrue(Summaries.recognitionDetail(r, versions).contains("Pro verzi 9.9 není k dispozici fDMF"));
        assertEquals("pro Jednotky fondu 9.9 není fDMF", Summaries.skippedSummary(new Dmf(Dmf.Type.FUND_UNIT, "9.9"), null));
    }

    @Test
    public void notRecognized() {
        Recognition r = Recognition.notRecognized("Typ dokumentu nelze zjistit: x");
        assertEquals("Balík nelze rozpoznat", Summaries.recognitionTitle(r));
        assertNull(Summaries.recognitionBadge(r));
        assertEquals("Typ dokumentu nelze zjistit: x", Summaries.recognitionDetail(r, versions));
        assertNull(Summaries.effectiveDmf(r, versions));
        assertEquals("nic není vynuceno", Summaries.versionsSummary(r, versions));
        versions.setForcedVersion(Dmf.Type.MONOGRAPH, "2.0");
        versions.setForcedVersion(Dmf.Type.PERIODICAL, "2.0");
        assertEquals("vynuceno u 2 typů", Summaries.versionsSummary(r, versions));
        versions.setForcedVersion(Dmf.Type.PERIODICAL, null);
        assertEquals("vynuceno u 1 typu", Summaries.versionsSummary(null, versions));
        assertEquals("Balík není vybrán", Summaries.recognitionTitle(Recognition.invalidPath("Vyberte adresář balíku.")));
    }

    @Test
    public void toolsSummaryForType() {
        Map<ExternalUtil, ToolStatus> statuses = allStatuses(ToolStatus.found("1.0"));
        statuses.put(KAKADU, ToolStatus.error("x"));
        Set<ExternalUtil> relevant = EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU);
        assertEquals("pro Jednotky fondu se použijí 3 ze 4", Summaries.toolsSummary("Jednotky fondu", relevant, statuses, Collections.<ExternalUtil>emptySet()));
        assertEquals("pro Jednotky fondu se použijí 2 ze 4", Summaries.toolsSummary("Jednotky fondu", relevant, statuses, EnumSet.of(JHOVE)));
        assertEquals("pro Jednotky fondu se použije 1 ze 4", Summaries.toolsSummary("Jednotky fondu", relevant, statuses, EnumSet.of(JHOVE, IMAGE_MAGICK)));
        assertEquals("pro E-monografie se použije 0 ze 2", Summaries.toolsSummary("E-monografie", EnumSet.of(VERAPDF, EPUBCHECK),
                allStatuses(ToolStatus.notFound("Cannot run program")), Collections.<ExternalUtil>emptySet()));
        assertEquals("pro Monografie se použije 5 z 5", Summaries.toolsSummary("Monografie", EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU, VERAPDF),
                statuses(ToolStatus.found("1")), Collections.<ExternalUtil>emptySet()));
    }

    @Test
    public void toolsSummaryWithoutTypeAndWhileChecking() {
        Map<ExternalUtil, ToolStatus> statuses = allStatuses(ToolStatus.found("1.0"));
        statuses.put(MP3VAL, ToolStatus.notFound("x"));
        statuses.put(SHNTOOL, ToolStatus.notFound("x"));
        statuses.put(CHECKMATE, ToolStatus.notFound("x"));
        statuses.put(KAKADU, ToolStatus.error("x"));
        assertEquals("5 dostupných, 4 nedostupné", Summaries.toolsSummary(null, null, statuses, Collections.<ExternalUtil>emptySet()));
        assertEquals("9 dostupných", Summaries.toolsSummary(null, null, allStatuses(ToolStatus.found("1")), Collections.<ExternalUtil>emptySet()));
        statuses.put(JHOVE, ToolStatus.checking());
        assertEquals("zjišťuji dostupnost nástrojů…", Summaries.toolsSummary("Monografie", EnumSet.of(JHOVE), statuses, Collections.<ExternalUtil>emptySet()));
        statuses.remove(JHOVE);
        assertEquals("zjišťuji dostupnost nástrojů…", Summaries.toolsSummary(null, null, statuses, Collections.<ExternalUtil>emptySet()));
    }

    @Test
    public void skippedSummary() {
        Dmf dmf = new Dmf(Dmf.Type.FUND_UNIT, "0.2");
        assertEquals("určí se podle typu a verze balíku", Summaries.skippedSummary(null, null));
        assertEquals("žádná", Summaries.skippedSummary(dmf, Collections.<String>emptyList()));
        assertEquals("1 celá sekce", Summaries.skippedSummary(dmf, Arrays.asList("a")));
        assertEquals("3 celé sekce", Summaries.skippedSummary(dmf, Arrays.asList("a", "b", "c")));
        assertEquals("5 celých sekcí", Summaries.skippedSummary(dmf, Arrays.asList("a", "b", "c", "d", "e")));
    }

    @Test
    public void typesAndGroups() {
        assertEquals("Jednotky fondu", DocumentTypes.name(Dmf.Type.FUND_UNIT));
        int count = 0;
        for (DocumentTypes.Group group : DocumentTypes.Group.values()) {
            assertFalse(group.name(), DocumentTypes.typesOf(group).isEmpty());
            count += DocumentTypes.typesOf(group).size();
        }
        assertEquals(Dmf.Type.values().length, count);
        assertEquals(Arrays.asList(Dmf.Type.EMONOGRAPH, Dmf.Type.EPERIODICAL, Dmf.Type.COMPERIODICAL), DocumentTypes.typesOf(DocumentTypes.Group.ELECTRONIC));
        List<String> monographs = DocumentTypes.availableVersions(registry, Dmf.Type.MONOGRAPH);
        assertEquals("2.3", monographs.get(0));
        assertEquals("1.0", monographs.get(monographs.size() - 1));
        for (Dmf.Type type : Dmf.Type.values()) {
            //vychozi verze z DmfDetector ma vzdy fDMF
            assertTrue(type.name(), DocumentTypes.availableVersions(registry, type).contains(DocumentTypes.defaultVersion(type)));
        }
    }

    @Test
    public void plural() {
        assertEquals("sekcí", Summaries.plural(0, "sekce", "sekce", "sekcí"));
        assertEquals("typu", Summaries.plural(1, "typu", "typů", "typů"));
        assertEquals("ze", Summaries.fromPreposition(4));
        assertEquals("z", Summaries.fromPreposition(5));
        assertEquals("ze", Summaries.fromPreposition(7));
    }

    private static Map<ExternalUtil, ToolStatus> allStatuses(ToolStatus status) {
        Map<ExternalUtil, ToolStatus> result = new EnumMap<>(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            result.put(util, status);
        }
        return result;
    }

    private static Map<ExternalUtil, ToolStatus> statuses(ToolStatus status) {
        return allStatuses(status);
    }
}
