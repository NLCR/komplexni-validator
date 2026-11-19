package nkp.pspValidator.gui.results;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.gui.newvalidation.OutputSettings;
import nkp.pspValidator.gui.newvalidation.ValidationRequest;
import nkp.pspValidator.gui.newvalidation.ValidationSource;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * „Validovat znovu“ (issue #87): kdy je akce dostupná a že znovu spustí tentýž požadavek se stejným nastavením
 * (zdroj a cesta, verze, výstupy, podrobnost logu, vypnuté nástroje), u ZIPu znovu přes rozbalení.
 */
public class RerunStateTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;

    /** Záznam volání místo Main a ExternalUtilManager. */
    private static class RecordingLauncher implements ValidationRequest.Launcher {
        final List<String> calls = new ArrayList<>();
        final List<Set<ExternalUtil>> disabled = new ArrayList<>();
        final List<File> files = new ArrayList<>();
        final List<DmfDetector.Params> params = new ArrayList<>();
        final List<String> outputs = new ArrayList<>();

        @Override
        public void setUtilsDisabled(Set<ExternalUtil> utils) {
            calls.add("disable");
            disabled.add(EnumSet.copyOf(utils.isEmpty() ? EnumSet.noneOf(ExternalUtil.class) : utils));
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

        private void record(File file, DmfDetector.Params p, boolean txt, boolean xml, int verbosity) {
            files.add(file);
            params.add(p);
            outputs.add("txt=" + txt + ",xml=" + xml + ",verbosity=" + verbosity);
        }
    }

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("cfg"));
        registry = GuiTestSupport.registry();
    }

    private ValidationRequest dirRequest(File dir, Set<ExternalUtil> disabled) {
        VersionSettings versions = new VersionSettings(config, registry);
        versions.setForcedVersion(Dmf.Type.FUND_UNIT, "0.1");
        OutputSettings outputs = new OutputSettings(config);
        outputs.setCreateXmlLog(true);
        outputs.setCreateTxtLog(true);
        outputs.setVerbosity(OutputSettings.Verbosity.SECTIONS_AND_RULES_WITH_ERRORS);
        return ValidationRequest.create(ValidationSource.DIRECTORY, dir.getPath(), versions, outputs, disabled);
    }

    @Test
    public void notAvailableBeforeAnyValidation() {
        RerunState state = new RerunState();
        assertFalse(state.canRerun());
        assertFalse(state.isRunning());
    }

    @Test(expected = IllegalStateException.class)
    public void requestBeforeAnyValidationFails() {
        new RerunState().requestForRerun();
    }

    @Test
    public void notAvailableWhileRunningAvailableAfterEnd() throws Exception {
        RerunState state = new RerunState();
        ValidationRequest request = dirRequest(tmp.newFolder("balik"), null);
        state.validationStarted(request);
        assertTrue(state.isRunning());
        assertFalse(state.canRerun());
        try {
            state.requestForRerun();
            fail("během validace nesmí jít opakovat");
        } catch (IllegalStateException expected) {
            //ok
        }
        state.validationEnded();
        assertFalse(state.isRunning());
        assertTrue(state.canRerun());
        assertSame(request, state.requestForRerun());
    }

    @Test
    public void validationWithoutRequestCannotBeRepeated() throws Exception {
        RerunState state = new RerunState();
        state.validationStarted(dirRequest(tmp.newFolder("prvni"), null));
        state.validationEnded();
        state.validationStarted(null);
        state.validationEnded();
        assertFalse(state.canRerun());
    }

    @Test
    public void latestRequestWins() throws Exception {
        RerunState state = new RerunState();
        ValidationRequest first = dirRequest(tmp.newFolder("prvni"), null);
        ValidationRequest second = dirRequest(tmp.newFolder("druhy"), EnumSet.of(ExternalUtil.JHOVE));
        state.validationStarted(first);
        state.validationEnded();
        state.validationStarted(second);
        assertFalse(state.canRerun());
        state.validationEnded();
        assertSame(second, state.requestForRerun());
    }

    @Test
    public void rerunOfDirectoryRepeatsAllSettingsIncludingDisabledTools() throws Exception {
        File dir = tmp.newFolder("balik");
        RerunState state = new RerunState();
        state.validationStarted(dirRequest(dir, EnumSet.of(ExternalUtil.JHOVE, ExternalUtil.KAKADU)));
        state.validationEnded();

        //mezitim uzivatel v dialogu zmeni ulozene nastaveni; opakovani ho nesmi prevzit
        new VersionSettings(config, registry).setForcedVersion(Dmf.Type.FUND_UNIT, "0.2");
        new OutputSettings(config).setCreateXmlLog(false);

        RecordingLauncher launcher = new RecordingLauncher();
        state.requestForRerun().launch(launcher);
        state.requestForRerun().launch(launcher);

        assertEquals(Arrays.asList("disable", "dir", "disable", "dir"), launcher.calls);
        for (int i = 0; i < 2; i++) {
            assertEquals(EnumSet.of(ExternalUtil.JHOVE, ExternalUtil.KAKADU), launcher.disabled.get(i));
            assertEquals(dir, launcher.files.get(i));
            assertEquals("0.1", launcher.params.get(i).forcedDmfFduVersion);
            assertEquals("txt=true,xml=true,verbosity=2", launcher.outputs.get(i));
        }
    }

    @Test
    public void changingParamsPassedToLauncherDoesNotChangeRequest() throws Exception {
        ValidationRequest request = dirRequest(tmp.newFolder("balik"), null);
        RecordingLauncher launcher = new RecordingLauncher();
        request.launch(launcher);
        launcher.params.get(0).forcedDmfFduVersion = "9.9";
        request.getParams().forcedDmfMonVersion = "9.9";
        request.launch(launcher);
        assertEquals("0.1", launcher.params.get(1).forcedDmfFduVersion);
        assertNull(launcher.params.get(1).forcedDmfMonVersion);
        assertNotSame(launcher.params.get(0), launcher.params.get(1));
    }

    @Test
    public void rerunOfZipUnzipsAgain() throws Exception {
        File zip = tmp.newFile("balik.zip");
        ValidationRequest request = ValidationRequest.create(ValidationSource.ZIP, zip.getPath(),
                new VersionSettings(config, registry), new OutputSettings(config), EnumSet.of(ExternalUtil.JPYLYZER));
        RerunState state = new RerunState();
        //validace ZIPu zacina az po rozbaleni, stejne jako u adresare se pak hlasi start a konec
        state.validationStarted(request);
        state.validationEnded();

        RecordingLauncher launcher = new RecordingLauncher();
        state.requestForRerun().launch(launcher);
        assertEquals(Arrays.asList("disable", "zip"), launcher.calls);
        assertEquals(zip, launcher.files.get(0));
        assertEquals(EnumSet.of(ExternalUtil.JPYLYZER), launcher.disabled.get(0));
        assertEquals("txt=false,xml=false,verbosity=3", launcher.outputs.get(0));
    }
}
