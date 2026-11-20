package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.gui.newvalidation.OutputSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

/** Záložka Adresáře a protokoly: koncept a uložení jen změněných hodnot do dosavadních klíčů. */
public class DirsDraftTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("root"));
    }

    @Test
    public void emptySettings() {
        File defaultLogDir = config.getFileOrNull("validation.log_dir");
        assertNotNull(defaultLogDir);
        DirsDraft draft = new DirsDraft(config);
        assertEquals(defaultLogDir, draft.getLogDir());
        assertEquals(config.getFileOrNull("validatorConfig.dir"), draft.getValidatorConfigDir());
        assertFalse(draft.isCreateXmlLog());
        assertFalse(draft.isCreateTxtLog());
        assertEquals(OutputSettings.Verbosity.ALL, draft.getVerbosity());
        assertFalse(draft.isChanged());
        draft.save();
        //vychozi hodnoty se nezapisuji
        assertNull(config.getStringOrDefault("psp_validation.create_xml_log", null));
        assertNull(config.getStringOrDefault("psp_validation.create_txt_log", null));
        assertNull(config.getStringOrDefault("text_log.verbosity", null));
    }

    @Test
    public void onlyChangedValuesAreSaved() throws Exception {
        DirsDraft draft = new DirsDraft(config);
        draft.setCreateXmlLog(true);
        assertTrue(draft.isChanged());
        assertNull(config.getStringOrDefault("psp_validation.create_xml_log", null));
        draft.save();
        assertEquals("true", config.getStringOrDefault("psp_validation.create_xml_log", null));
        assertNull(config.getStringOrDefault("psp_validation.create_txt_log", null));
        assertNull(config.getStringOrDefault("text_log.verbosity", null));
    }

    @Test
    public void txtLogAndVerbosity() {
        DirsDraft draft = new DirsDraft(config);
        draft.setCreateTxtLog(true);
        draft.setVerbosity(OutputSettings.Verbosity.SECTIONS_WITH_ERRORS);
        draft.save();
        assertEquals("true", config.getStringOrDefault("psp_validation.create_txt_log", null));
        assertEquals("1", config.getStringOrDefault("text_log.verbosity", null));
        assertNull(config.getStringOrDefault("psp_validation.create_xml_log", null));
    }

    @Test
    public void revertIsNoChange() {
        DirsDraft draft = new DirsDraft(config);
        draft.setCreateTxtLog(true);
        draft.setCreateTxtLog(false);
        draft.setVerbosity(OutputSettings.Verbosity.VALID_OR_INVALID);
        draft.setVerbosity(OutputSettings.Verbosity.ALL);
        assertFalse(draft.isChanged());
    }

    @Test
    public void nullValuesIgnored() {
        DirsDraft draft = new DirsDraft(config);
        File logDir = draft.getLogDir();
        draft.setLogDir(null);
        draft.setVerbosity(null);
        assertEquals(logDir, draft.getLogDir());
        assertEquals(OutputSettings.Verbosity.ALL, draft.getVerbosity());
        assertFalse(draft.isChanged());
    }

    @Test
    public void logDir() throws Exception {
        File dir = tmp.newFolder("protokoly");
        DirsDraft draft = new DirsDraft(config);
        draft.setLogDir(dir);
        assertTrue(draft.isChanged());
        assertNotEquals(dir.getCanonicalFile(), config.getFileOrNull("validation.log_dir").getCanonicalFile());
        draft.save();
        assertEquals(dir.getCanonicalPath(), config.getStringOrDefault("validation.log_dir", null));
    }

    @Test
    public void storedValuesLoaded() {
        config.setBoolean("psp_validation.create_xml_log", true);
        config.setInteger("text_log.verbosity", 2);
        DirsDraft draft = new DirsDraft(config);
        assertTrue(draft.isCreateXmlLog());
        assertEquals(OutputSettings.Verbosity.SECTIONS_AND_RULES_WITH_ERRORS, draft.getVerbosity());
        assertFalse(draft.isChanged());
    }

    // ===== konfigurace validátoru =====

    private static final String KEY_DIR = "validatorConfig.dir";
    private static final String KEY_CHOSEN = "validatorConfig.dir.chosenByUser";

    @Test
    public void validatorConfigInUse() {
        File saved = config.getFileOrNull(KEY_DIR);
        DirsDraft draft = new DirsDraft(config, saved);
        assertEquals(saved, draft.getValidatorConfigDir());
        assertEquals(saved, draft.getRunningValidatorConfigDir());
        assertEquals(DirsDraft.ValidatorConfigState.IN_USE, draft.getValidatorConfigState());
        assertFalse(draft.isValidatorConfigDirChanged());
        assertFalse(draft.isRestartNeededAfterSave());
        assertFalse(draft.isChanged());
        //bez zadaneho pouzivaneho adresare je to ten ulozeny
        assertEquals(saved, new DirsDraft(config).getRunningValidatorConfigDir());
        assertEquals(DirsDraft.ValidatorConfigState.IN_USE, new DirsDraft(config).getValidatorConfigState());
    }

    @Test
    public void newValidatorConfigIsUnsavedUntilSave() throws Exception {
        File running = config.getFileOrNull(KEY_DIR);
        File chosen = tmp.newFolder("Moje konfigurace č. 2");
        DirsDraft draft = new DirsDraft(config, running);
        draft.setValidatorConfigDir(chosen);
        assertEquals(chosen, draft.getValidatorConfigDir());
        assertEquals(running, draft.getRunningValidatorConfigDir());
        assertEquals(DirsDraft.ValidatorConfigState.UNSAVED, draft.getValidatorConfigState());
        assertTrue(draft.isValidatorConfigDirChanged());
        assertTrue(draft.isChanged());
        assertTrue(draft.isRestartNeededAfterSave());
        //do nastaveni se zatim nic nezapsalo
        assertEquals(running, config.getFileOrNull(KEY_DIR));
        assertNull(config.getStringOrDefault(KEY_CHOSEN, null));

        draft.save();
        assertEquals(chosen.getCanonicalPath(), config.getStringOrDefault(KEY_DIR, null));
        assertEquals("true", config.getStringOrDefault(KEY_CHOSEN, null));
        //ostatni hodnoty zaloky se nezapsaly
        assertNull(config.getStringOrDefault("psp_validation.create_xml_log", null));
        assertNull(config.getStringOrDefault("text_log.verbosity", null));
    }

    /** Neukládat: koncept se zapomene, při dalším otevření Nastavení je zase uložený adresář */
    @Test
    public void discardedValidatorConfig() throws Exception {
        File running = config.getFileOrNull(KEY_DIR);
        DirsDraft draft = new DirsDraft(config, running);
        draft.setValidatorConfigDir(tmp.newFolder("zahozená"));
        DirsDraft reopened = new DirsDraft(config, running);
        assertEquals(running, reopened.getValidatorConfigDir());
        assertEquals(DirsDraft.ValidatorConfigState.IN_USE, reopened.getValidatorConfigState());
        assertFalse(reopened.isChanged());
        assertEquals(running, config.getFileOrNull(KEY_DIR));
        assertNull(config.getStringOrDefault(KEY_CHOSEN, null));
    }

    /** Později: uložený adresář se liší od používaného až do restartu */
    @Test
    public void savedValidatorConfigPendingRestart() throws Exception {
        File running = config.getFileOrNull(KEY_DIR);
        File chosen = tmp.newFolder("nová");
        DirsDraft draft = new DirsDraft(config, running);
        draft.setValidatorConfigDir(chosen);
        draft.save();

        DirsDraft reopened = new DirsDraft(config, running);
        assertEquals(chosen.getCanonicalFile(), reopened.getValidatorConfigDir());
        assertEquals(running, reopened.getRunningValidatorConfigDir());
        assertEquals(DirsDraft.ValidatorConfigState.SAVED_PENDING_RESTART, reopened.getValidatorConfigState());
        assertFalse(reopened.isChanged());
        assertFalse("uložený adresář se nemění, restart se znovu nenabízí", reopened.isRestartNeededAfterSave());
    }

    /** návrat k používanému adresáři po uložení jiného: neuložená změna bez restartu; výchozí adresář se neoznačí */
    @Test
    public void backToRunningValidatorConfig() throws Exception {
        File running = config.getFileOrNull(KEY_DIR);
        DirsDraft draft = new DirsDraft(config, running);
        draft.setValidatorConfigDir(tmp.newFolder("jiná"));
        draft.save();
        assertEquals("true", config.getStringOrDefault(KEY_CHOSEN, null));

        DirsDraft back = new DirsDraft(config, running);
        back.setValidatorConfigDir(running);
        assertEquals(DirsDraft.ValidatorConfigState.UNSAVED_BACK_TO_RUNNING, back.getValidatorConfigState());
        assertTrue(back.isChanged());
        assertFalse(back.isRestartNeededAfterSave());
        back.save();
        assertEquals(running.getCanonicalPath(), config.getStringOrDefault(KEY_DIR, null));
        assertNull("výchozí adresář z instalace se neoznačuje jako zvolený", config.getStringOrDefault(KEY_CHOSEN, null));
        assertEquals(DirsDraft.ValidatorConfigState.IN_USE, new DirsDraft(config, running).getValidatorConfigState());
    }

    /** stejný adresář zadaný jinou cestou (s „..“) není změna */
    @Test
    public void sameValidatorConfigByOtherPathIsNoChange() {
        File running = config.getFileOrNull(KEY_DIR);
        DirsDraft draft = new DirsDraft(config, running);
        draft.setValidatorConfigDir(new File(running, "../" + running.getName()));
        assertFalse(draft.isValidatorConfigDirChanged());
        assertFalse(draft.isChanged());
        assertEquals(DirsDraft.ValidatorConfigState.IN_USE, draft.getValidatorConfigState());
        draft.setValidatorConfigDir(null);
        assertFalse(draft.isChanged());
    }
}
