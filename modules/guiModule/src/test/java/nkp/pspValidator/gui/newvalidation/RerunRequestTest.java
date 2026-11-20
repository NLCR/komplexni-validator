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
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * „Validovat znovu“ (P1 z revize GUI): volby dialogu Nová validace (zdroj, vynucené verze, výstupy, podrobnost logu,
 * vypnuté nástroje) zůstávají z původního požadavku, preferované (výchozí) verze se čtou z aktuálního nastavení.
 */
public class RerunRequestTest {

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

    /** Požadavek jako z dialogu: vynucená Jednotka fondu 0.1, výchozí monografie 1.4 a Jednotka fondu 0.2. */
    private ValidationRequest requestFromDialog(File dir) {
        versions.setForcedVersion(Dmf.Type.FUND_UNIT, "0.1");
        versions.setPreferredVersion(Dmf.Type.MONOGRAPH, "1.4");
        versions.setPreferredVersion(Dmf.Type.FUND_UNIT, "0.2");
        OutputSettings outputs = new OutputSettings(config);
        outputs.setCreateTxtLog(true);
        outputs.setCreateXmlLog(false);
        outputs.setVerbosity(OutputSettings.Verbosity.SECTIONS_AND_RULES_WITH_ERRORS);
        return ValidationRequest.create(ValidationSource.DIRECTORY, dir.getPath(), versions, outputs,
                EnumSet.of(ExternalUtil.KAKADU));
    }

    @Test
    public void changedPreferredVersionIsUsedOnRerun() throws Exception {
        ValidationRequest original = requestFromDialog(tmp.newFolder("balik"));
        assertEquals("1.4", original.getParams().preferredDmfMonVersion);

        //uzivatel v Nastaveni zmeni vychozi verzi monografie a vypne vychozi verzi Jednotky fondu
        versions.setPreferredVersion(Dmf.Type.MONOGRAPH, "2.0");
        versions.setPreferredVersion(Dmf.Type.FUND_UNIT, null);

        DmfDetector.Params rerun = original.forRerun(new VersionSettings(config, registry)).getParams();
        assertEquals("2.0", rerun.preferredDmfMonVersion);
        assertNull("vypnutá výchozí verze se nepředává", rerun.preferredDmfFduVersion);
        //puvodni pozadavek se nezmenil
        assertEquals("1.4", original.getParams().preferredDmfMonVersion);
        assertEquals("0.2", original.getParams().preferredDmfFduVersion);
    }

    @Test
    public void forcedVersionFromRequestIsKeptOnRerun() throws Exception {
        ValidationRequest original = requestFromDialog(tmp.newFolder("balik"));

        //vynuceni je volba dialogu pro tuto validaci: ani zmena ulozeneho vynuceni ji pri opakovani nezmeni
        versions.setForcedVersion(Dmf.Type.FUND_UNIT, null);
        versions.setForcedVersion(Dmf.Type.MONOGRAPH, "2.0");

        DmfDetector.Params rerun = original.forRerun(new VersionSettings(config, registry)).getParams();
        assertEquals("0.1", rerun.forcedDmfFduVersion);
        assertNull(rerun.forcedDmfMonVersion);
        assertNull(rerun.forcedDmfPerVersion);
    }

    @Test
    public void otherDialogChoicesAreKeptOnRerun() throws Exception {
        File dir = tmp.newFolder("balik");
        ValidationRequest original = requestFromDialog(dir);

        //zmena vystupu v nastaveni (dialog je pri dalsi validaci predvyplni) opakovani nemeni
        OutputSettings outputs = new OutputSettings(config);
        outputs.setCreateTxtLog(false);
        outputs.setCreateXmlLog(true);
        outputs.setVerbosity(OutputSettings.Verbosity.ALL);

        ValidationRequest rerun = original.forRerun(new VersionSettings(config, registry));
        assertNotSame(original, rerun);
        assertEquals(ValidationSource.DIRECTORY, rerun.getSource());
        assertEquals(dir, rerun.getFile());
        assertTrue(rerun.isCreateTxtLog());
        assertFalse(rerun.isCreateXmlLog());
        assertEquals(2, rerun.getVerbosity());
        assertEquals(EnumSet.of(ExternalUtil.KAKADU), rerun.getDisabledUtils());
    }

    @Test
    public void unchangedSettingsGiveSameParams() throws Exception {
        ValidationRequest original = requestFromDialog(tmp.newFolder("balik"));
        DmfDetector.Params before = original.getParams();
        DmfDetector.Params rerun = original.forRerun(new VersionSettings(config, registry)).getParams();
        assertEquals("1.4", rerun.preferredDmfMonVersion);
        assertEquals("0.2", rerun.preferredDmfFduVersion);
        assertEquals("0.1", rerun.forcedDmfFduVersion);
        //vsech 22 poli stejne
        for (java.lang.reflect.Field field : DmfDetector.Params.class.getFields()) {
            assertEquals(field.getName(), field.get(before), field.get(rerun));
        }
    }

    @Test
    public void allPreferredFieldsComeFromSettingsAndAllForcedFromOriginal() throws Exception {
        //puvodni parametry: kazde pole jina hodnota, aby slo poznat, odkud se vzalo
        DmfDetector.Params original = new DmfDetector.Params();
        int fields = 0;
        for (java.lang.reflect.Field field : DmfDetector.Params.class.getFields()) {
            field.set(original, "puvodni-" + field.getName());
            fields++;
        }
        assertEquals(22, fields);
        //v nastaveni zadna vychozi verze, jen monografie 1.3
        versions.setPreferredVersion(Dmf.Type.MONOGRAPH, "1.3");

        DmfDetector.Params result = DmfParamsBuilder.withCurrentPreferred(original, versions);
        for (java.lang.reflect.Field field : DmfDetector.Params.class.getFields()) {
            Object value = field.get(result);
            if (field.getName().startsWith("forced")) {
                assertEquals(field.getName(), "puvodni-" + field.getName(), value);
            } else if (field.getName().equals("preferredDmfMonVersion")) {
                assertEquals("1.3", value);
            } else {
                assertNull(field.getName(), value);
            }
        }
    }

    @Test
    public void withoutOriginalParamsOnlySettingsAreUsed() {
        versions.setPreferredVersion(Dmf.Type.PERIODICAL, "1.6");
        versions.setForcedVersion(Dmf.Type.MONOGRAPH, "2.0");
        DmfDetector.Params result = DmfParamsBuilder.withCurrentPreferred(null, versions);
        assertEquals("1.6", result.preferredDmfPerVersion);
        assertEquals("2.0", result.forcedDmfMonVersion);
    }

    @Test
    public void rerunLaunchesWithCurrentPreferredVersion() throws Exception {
        File dir = tmp.newFolder("balik");
        ValidationRequest original = requestFromDialog(dir);
        versions.setPreferredVersion(Dmf.Type.FUND_UNIT, "0.1");
        final DmfDetector.Params[] launched = new DmfDetector.Params[1];
        final Set<?>[] disabled = new Set<?>[1];
        original.forRerun(new VersionSettings(config, registry)).launch(new ValidationRequest.Launcher() {
            @Override
            public void setUtilsDisabled(Set<ExternalUtil> utils) {
                disabled[0] = utils;
            }

            @Override
            public void runPspDirValidation(File pspDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
                launched[0] = params;
            }

            @Override
            public void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
                fail("adresář se nerozbaluje");
            }

            @Override
            public void runGroupValidation(File groupDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
                fail("není skupina");
            }
        });
        assertEquals("0.1", launched[0].preferredDmfFduVersion);
        assertEquals("0.1", launched[0].forcedDmfFduVersion);
        assertEquals(EnumSet.of(ExternalUtil.KAKADU), disabled[0]);
    }
}
