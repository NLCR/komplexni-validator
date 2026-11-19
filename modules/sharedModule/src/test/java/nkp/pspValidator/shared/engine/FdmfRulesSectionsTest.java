package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * FdmfRegistry.getRulesSections (seznam sekci pro dialog preskakovani v GUI bez stavby validatoru) musi davat
 * stejne sekce (id, nazev, popis, poradi) jako Engine postaveny pro tutez fDMF, a to pro vsechny fDMF.
 */
public class FdmfRulesSectionsTest {

    @Test
    public void sectionsMatchEngineForAllFdmf() throws Exception {
        ValidatorConfigurationManager configManager = FdmfTestSupport.sharedConfigManager();
        FdmfRegistry registry = FdmfTestSupport.sharedRegistry();
        ExternalUtilManager utilManager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            utilManager.setUtilDisabled(util);
        }
        int checked = 0;
        for (File dir : FdmfTestSupport.fdmfDirs()) {
            Dmf dmf = FdmfTestSupport.dmfFromDirName(dir.getName());
            List<RulesSection> fromRegistry = registry.getRulesSections(dmf);
            FdmfConfiguration fdmfConfig = registry.getFdmfConfig(dmf);
            fdmfConfig.initBinaryFileProfiles(utilManager);
            List<RulesSection> fromEngine = ValidatorFactory.buildValidator(fdmfConfig, null, configManager.getDictionaryManager())
                    .getEngine().getRuleSections();
            assertFalse(dmf + ": zadne sekce", fromEngine.isEmpty());
            assertEquals(dmf + ": pocet sekci", fromEngine.size(), fromRegistry.size());
            for (int i = 0; i < fromEngine.size(); i++) {
                RulesSection expected = fromEngine.get(i);
                RulesSection actual = fromRegistry.get(i);
                assertEquals(dmf + ": id", expected.getId(), actual.getId());
                assertEquals(dmf + ": nazev", expected.getName(), actual.getName());
                assertEquals(dmf + ": popis", expected.getDescription(), actual.getDescription());
                assertTrue(dmf + ": sekce ma byt zapnuta", actual.isEnabled());
            }
            checked++;
        }
        assertEquals(FdmfTestSupport.fdmfDirs().length, checked);
    }

    @Test
    public void sectionsAreReadWithoutLoadingConfigAndReturnedAsCopies() throws Exception {
        FdmfRegistry registry = new FdmfRegistry(FdmfTestSupport.sharedConfigManager());
        Dmf dmf = new Dmf(Dmf.Type.FUND_UNIT, "0.2");
        List<RulesSection> first = registry.getRulesSections(dmf);
        assertFalse(first.isEmpty());
        assertFalse("sekce se maji cist bez nacteni cele konfigurace", registry.isFdmfConfigLoaded(dmf));
        first.get(0).setEnabled(false);
        List<RulesSection> second = registry.getRulesSections(dmf);
        assertNotSame(first.get(0), second.get(0));
        assertTrue("zmena kopie se nesmi projevit v dalsim volani", second.get(0).isEnabled());
    }

    @Test(expected = FdmfRegistry.UnknownFdmfException.class)
    public void unknownFdmfSections() throws Exception {
        FdmfTestSupport.sharedRegistry().getRulesSections(new Dmf(Dmf.Type.MONOGRAPH, "9.9"));
    }
}
