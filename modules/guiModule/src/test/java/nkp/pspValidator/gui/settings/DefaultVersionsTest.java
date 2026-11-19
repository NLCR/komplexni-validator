package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.gui.newvalidation.DmfParamsBuilder;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.FdmfRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Záložka Výchozí verze DMF: volby, výběr podle uloženého stavu, verze, která se použije, poznámky a uložení
 * do klíčů {@code prefer.*}. Očekávané verze jsou vypsané ručně podle adresářů fDMF v repozitáři (monografie 1.0 až 2.3,
 * výchozí verze validátoru monografií 2.3, periodik 2.2).
 */
public class DefaultVersionsTest {

    private static final List<String> MONOGRAPH_VERSIONS = Arrays.asList("2.3", "2.2", "2.1", "2.0", "1.4", "1.3.2", "1.3.1", "1.3", "1.2", "1.0");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.getRoot());
        registry = GuiTestSupport.registry();
    }

    private DefaultVersions draft() {
        return new DefaultVersions(new VersionSettings(config, registry));
    }

    private DmfDetector.Params params() {
        return DmfParamsBuilder.build(new VersionSettings(config, registry));
    }

    // ===== čisté funkce =====

    @Test
    public void choices() {
        assertEquals(Arrays.asList(new DefaultVersions.Choice(null, "výchozí validátoru (2.2)"),
                        new DefaultVersions.Choice("2.2", "2.2"), new DefaultVersions.Choice("1.9", "1.9")),
                DefaultVersions.choices("2.2", Arrays.asList("2.2", "1.9")));
        //typ bez dostupne verze: jen vychozi validatoru
        assertEquals(Collections.singletonList(new DefaultVersions.Choice(null, "výchozí validátoru (0.1)")),
                DefaultVersions.choices("0.1", Collections.<String>emptyList()));
    }

    @Test
    public void initialSelection() {
        List<String> available = Arrays.asList("2.3", "2.2");
        assertNull(DefaultVersions.initialSelection(false, "2.2", available));
        assertNull(DefaultVersions.initialSelection(false, null, available));
        assertEquals("2.2", DefaultVersions.initialSelection(true, "2.2", available));
        assertEquals("2.3", DefaultVersions.initialSelection(true, "9.9", available));
        assertEquals("2.3", DefaultVersions.initialSelection(true, null, available));
        assertNull(DefaultVersions.initialSelection(true, "2.2", Collections.<String>emptyList()));
    }

    @Test
    public void staleNote() {
        List<String> available = Arrays.asList("2.3", "2.2");
        assertNull(DefaultVersions.staleNote(false, "9.9", available));
        assertNull(DefaultVersions.staleNote(true, "2.2", available));
        assertNull(DefaultVersions.staleNote(true, "9.9", Collections.<String>emptyList()));
        assertEquals("Uložená verze 9.9 není k dispozici, použije se nejnovější 2.3.", DefaultVersions.staleNote(true, "9.9", available));
        assertEquals("Uložená verze chybí, použije se nejnovější 2.3.", DefaultVersions.staleNote(true, null, available));
        assertEquals("Uložená verze chybí, použije se nejnovější 2.3.", DefaultVersions.staleNote(true, " ", available));
    }

    @Test
    public void usedVersion() {
        assertEquals("1.4", DefaultVersions.usedVersion("1.4", "2.3"));
        assertEquals("2.3", DefaultVersions.usedVersion(null, "2.3"));
    }

    @Test
    public void note() {
        List<String> available = Arrays.asList("2.3", "2.2");
        assertNull(DefaultVersions.note(null, "2.3", available, null, false));
        assertNull(DefaultVersions.note("2.2", "2.3", available, null, true));
        assertEquals("Pro tento typ není k dispozici žádná verze fDMF.",
                DefaultVersions.note(null, "2.3", Collections.<String>emptyList(), null, false));
        assertEquals("stará", DefaultVersions.note("2.3", "2.3", available, "stará", false));
        //po zmene radku uz upozorneni na ulozenou verzi neplati
        assertNull(DefaultVersions.note("2.3", "2.3", available, "stará", true));
        assertEquals("Výchozí verze validátoru 2.4 není k dispozici.", DefaultVersions.note(null, "2.4", available, null, false));
        assertNull(DefaultVersions.note("2.2", "2.4", available, null, false));
    }

    // ===== koncept nad nastavením =====

    @Test
    public void emptySettings() {
        DefaultVersions draft = draft();
        for (Dmf.Type type : Dmf.Type.values()) {
            assertNull(type.name(), draft.selected(type).version);
            assertNull(type.name(), draft.note(type));
            assertTrue(type.name(), draft.hasVersions(type));
        }
        assertEquals("výchozí validátoru (2.3)", draft.selected(Dmf.Type.MONOGRAPH).label);
        assertEquals("2.3", draft.usedVersion(Dmf.Type.MONOGRAPH));
        assertEquals("2.2", draft.usedVersion(Dmf.Type.PERIODICAL));
        assertEquals(11, draft.choices(Dmf.Type.MONOGRAPH).size());
        assertEquals("výchozí validátoru (2.3)", draft.choices(Dmf.Type.MONOGRAPH).get(0).label);
        for (int i = 0; i < MONOGRAPH_VERSIONS.size(); i++) {
            assertEquals(MONOGRAPH_VERSIONS.get(i), draft.choices(Dmf.Type.MONOGRAPH).get(i + 1).version);
        }
        assertFalse(draft.isChanged());
        draft.save();
        assertNull(config.getStringOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, null));
        assertNull(config.getStringOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, null));
    }

    @Test
    public void selectAndSave() {
        DefaultVersions draft = draft();
        draft.select(Dmf.Type.MONOGRAPH, "1.4");
        assertTrue(draft.isChanged());
        assertEquals("1.4", draft.usedVersion(Dmf.Type.MONOGRAPH));
        assertEquals("1.4", draft.selected(Dmf.Type.MONOGRAPH).label);
        //pred ulozenim se validaci nic nemeni
        assertNull(params().preferredDmfMonVersion);
        assertNull(config.getStringOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, null));

        draft.save();
        assertEquals("true", config.getStringOrDefault("prefer.monograph.version.enabled", null));
        assertEquals("1.4", config.getStringOrDefault("prefer.monograph.version.code", null));
        DmfDetector.Params params = params();
        assertEquals("1.4", params.preferredDmfMonVersion);
        assertNull(params.forcedDmfMonVersion);
        assertNull(params.preferredDmfPerVersion);
        //ostatni typy se nezapsaly
        assertNull(config.getStringOrDefault("prefer.periodical.version.enabled", null));
        assertEquals("1.4", new VersionSettings(config, registry).versionWhenMissing(Dmf.Type.MONOGRAPH));
    }

    @Test
    public void selectBackIsNoChange() {
        DefaultVersions draft = draft();
        draft.select(Dmf.Type.PERIODICAL, "1.9");
        draft.select(Dmf.Type.PERIODICAL, null);
        assertFalse(draft.isChanged());
    }

    @Test
    public void unknownVersionIgnored() {
        DefaultVersions draft = draft();
        draft.select(Dmf.Type.MONOGRAPH, "9.9");
        assertFalse(draft.isChanged());
        assertNull(draft.selected(Dmf.Type.MONOGRAPH).version);
    }

    @Test
    public void storedButDisabledShowsValidatorDefault() {
        //stav jako v nastaveni po dialozich 2.x: kod ulozeny, preferovani vypnute
        config.setString(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, "1.4");
        DefaultVersions draft = draft();
        assertNull(draft.selected(Dmf.Type.MONOGRAPH).version);
        assertEquals("2.3", draft.usedVersion(Dmf.Type.MONOGRAPH));
        assertNull(draft.note(Dmf.Type.MONOGRAPH));
        assertNull(params().preferredDmfMonVersion);
    }

    @Test
    public void resetAllDisablesAndKeepsCode() {
        config.setBoolean(ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, true);
        config.setString(ConfigurationManager.PROP_PREFER_PER_VERSION_CODE, "1.9");
        DefaultVersions draft = draft();
        assertEquals("1.9", draft.selected(Dmf.Type.PERIODICAL).version);
        assertEquals("1.9", draft.usedVersion(Dmf.Type.PERIODICAL));
        assertEquals("1.9", params().preferredDmfPerVersion);

        draft.resetAll();
        assertTrue(draft.isChanged());
        assertEquals("2.2", draft.usedVersion(Dmf.Type.PERIODICAL));
        draft.save();
        assertEquals("false", config.getStringOrDefault("prefer.periodical.version.enabled", null));
        assertEquals("1.9", config.getStringOrDefault("prefer.periodical.version.code", null));
        assertNull(params().preferredDmfPerVersion);
        //typy, ktere uz byly na vychozi verzi, se resetem nezapsaly
        assertNull(config.getStringOrDefault("prefer.monograph.version.enabled", null));
    }

    @Test
    public void staleStoredVersion() {
        config.setBoolean(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, true);
        config.setString(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, "9.9");
        //validace pouzije nejnovejsi dostupnou (chovani drivejsich dialogu)
        assertEquals("2.3", params().preferredDmfMonVersion);
        DefaultVersions draft = draft();
        assertEquals("2.3", draft.selected(Dmf.Type.MONOGRAPH).version);
        assertEquals("2.3", draft.usedVersion(Dmf.Type.MONOGRAPH));
        assertEquals("Uložená verze 9.9 není k dispozici, použije se nejnovější 2.3.", draft.note(Dmf.Type.MONOGRAPH));
        //bez zasahu se nic neprepise
        assertFalse(draft.isChanged());
        draft.save();
        assertEquals("9.9", config.getStringOrDefault("prefer.monograph.version.code", null));

        //vyber teze verze ulozenou hodnotu opravi
        draft.select(Dmf.Type.MONOGRAPH, "2.3");
        assertNull(draft.note(Dmf.Type.MONOGRAPH));
        assertTrue(draft.isChanged());
        draft.save();
        assertEquals("2.3", config.getStringOrDefault("prefer.monograph.version.code", null));
        assertEquals("true", config.getStringOrDefault("prefer.monograph.version.enabled", null));
        assertEquals("2.3", params().preferredDmfMonVersion);
    }

    @Test
    public void forcedVersionUntouched() {
        //vynucena verze zustava v dialogu Nova validace, zalozka ji nemeni
        config.setBoolean(ConfigurationManager.PROP_FORCE_MON_VERSION_ENABLED, true);
        config.setString(ConfigurationManager.PROP_FORCE_MON_VERSION_CODE, "2.0");
        DefaultVersions draft = draft();
        draft.select(Dmf.Type.MONOGRAPH, "1.4");
        draft.save();
        DmfDetector.Params params = params();
        assertEquals("2.0", params.forcedDmfMonVersion);
        assertEquals("1.4", params.preferredDmfMonVersion);
    }
}
