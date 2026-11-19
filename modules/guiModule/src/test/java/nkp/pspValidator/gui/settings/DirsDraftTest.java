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
}
