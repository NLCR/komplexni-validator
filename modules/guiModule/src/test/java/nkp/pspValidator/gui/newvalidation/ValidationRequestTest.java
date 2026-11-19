package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Požadavek na validaci: z dialogu se volají stejné metody Main jako dřív (runPspDirValidation pro adresář,
 * unzipAndRunPspZipValidation pro ZIP) se stejnými parametry, a před spuštěním se nastaví nástroje vypnuté pro
 * tuto validaci.
 */
public class ValidationRequestTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;

    /** Záznam volání místo Main a ExternalUtilManager. */
    private static class RecordingLauncher implements ValidationRequest.Launcher {
        final List<String> calls = new ArrayList<>();
        Set<ExternalUtil> disabled;
        File file;
        DmfDetector.Params params;
        boolean txt, xml;
        int verbosity = -1;

        @Override
        public void setUtilsDisabled(Set<ExternalUtil> utils) {
            calls.add("disable");
            disabled = utils;
        }

        @Override
        public void runPspDirValidation(File pspDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
            calls.add("dir");
            record(pspDir, params, createTxtLog, createXmlLog, verbosity);
        }

        @Override
        public void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
            calls.add("zip");
            record(pspZip, params, createTxtLog, createXmlLog, verbosity);
        }

        private void record(File file, DmfDetector.Params params, boolean txt, boolean xml, int verbosity) {
            this.file = file;
            this.params = params;
            this.txt = txt;
            this.xml = xml;
            this.verbosity = verbosity;
        }
    }

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("cfg"));
        registry = GuiTestSupport.registry();
    }

    @Test
    public void directoryRunsDirValidationWithSettings() throws Exception {
        File dir = tmp.newFolder("balik");
        VersionSettings versions = new VersionSettings(config, registry);
        versions.setForcedVersion(Dmf.Type.FUND_UNIT, "0.1");
        config.setBoolean(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, true);
        config.setString(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, "1.4");
        OutputSettings outputs = new OutputSettings(config);
        outputs.setCreateTxtLog(true);
        outputs.setVerbosity(OutputSettings.Verbosity.SECTIONS_AND_RULES_WITH_ERRORS);

        ValidationRequest request = ValidationRequest.create(ValidationSource.DIRECTORY, " " + dir.getPath() + " ", versions, outputs,
                EnumSet.of(ExternalUtil.KAKADU));
        RecordingLauncher launcher = new RecordingLauncher();
        request.launch(launcher);

        assertEquals(java.util.Arrays.asList("disable", "dir"), launcher.calls);
        assertEquals(EnumSet.of(ExternalUtil.KAKADU), launcher.disabled);
        assertEquals(dir, launcher.file);
        assertEquals("0.1", launcher.params.forcedDmfFduVersion);
        assertEquals("1.4", launcher.params.preferredDmfMonVersion);
        assertNull(launcher.params.forcedDmfMonVersion);
        assertTrue(launcher.txt);
        assertFalse(launcher.xml);
        assertEquals(2, launcher.verbosity);
    }

    @Test
    public void zipRunsUnzipAndValidationAndClearsDisabledTools() throws Exception {
        File zip = tmp.newFile("balik.zip");
        ValidationRequest request = ValidationRequest.create(ValidationSource.ZIP, zip.getPath(),
                new VersionSettings(config, registry), new OutputSettings(config), null);
        RecordingLauncher launcher = new RecordingLauncher();
        request.launch(launcher);
        assertEquals(java.util.Arrays.asList("disable", "zip"), launcher.calls);
        //prazdna sada: nastroje vypnute pri predchozi validaci se znovu zapnou
        assertTrue(launcher.disabled.isEmpty());
        assertEquals(zip, launcher.file);
        assertFalse(launcher.txt);
        assertFalse(launcher.xml);
        assertEquals(3, launcher.verbosity);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPathIsRejected() {
        ValidationRequest.create(ValidationSource.ZIP, new File(tmp.getRoot(), "neni.zip").getPath(),
                new VersionSettings(config, registry), new OutputSettings(config), null);
    }
}
