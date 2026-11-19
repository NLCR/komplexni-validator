package nkp.pspValidator.gui;

import nkp.pspValidator.gui.ConfigurationManager.Locations;
import nkp.pspValidator.gui.skipping.Skipped;
import nkp.pspValidator.gui.skipping.SkippedManagerImpl;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import nkp.pspValidator.shared.engine.RulesSection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Dialog preskakovani sekci (SkippedManagerImpl) bere seznam sekci z FdmfRegistry.getRulesSections, bez stavby
 * validatoru a bez nacteni konfigurace vsech fDMF. Bez JavaFX.
 */
public class SkippedManagerTest {

    private static final File VALIDATOR_CONFIG_DIR = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager configurationManager;
    private ValidationDataManager validationDataManager;
    private FdmfRegistry registry;

    @Before
    public void setUp() throws Exception {
        File jarDir = tmp.newFolder("KomplexniValidator", "app");
        assertTrue(new File(jarDir, "validatorConfig").mkdirs());
        File home = tmp.newFolder("home");
        Locations locations = Locations.resolve(OperatingSystem.WINDOWS, true, jarDir, home, new File(home, "AppData/Roaming").getPath(), null, tmp.newFolder("cwd"));
        configurationManager = new ConfigurationManager(Platform.detectOs(), locations);
        validationDataManager = new ValidationDataManager(configurationManager);
        registry = new FdmfRegistry(new ValidatorConfigurationManager(VALIDATOR_CONFIG_DIR));
        validationDataManager.setFdmfRegistry(registry);
    }

    @Test
    public void sectionsForAllFdmfWithoutLoadingConfigurations() throws Exception {
        Dmf dmf = new Dmf(Dmf.Type.FUND_UNIT, "0.2");
        List<RulesSection> sections = registry.getRulesSections(dmf);
        String skippedName = sections.get(1).getName();
        configurationManager.setStringSet(ConfigurationManager.propSkippedValidationSections(dmf),
                new HashSet<>(Arrays.asList(skippedName, "neexistujici sekce")));

        SkippedManagerImpl manager = new SkippedManagerImpl(configurationManager, validationDataManager);
        int fdmfDirs = new File(VALIDATOR_CONFIG_DIR, "fDMF").listFiles(File::isDirectory).length;
        assertEquals(fdmfDirs, manager.getDmfList().size());
        assertEquals("seznam sekci nesmi nacitat konfigurace fDMF", 0, registry.getLoadedFdmfConfigCount());

        Skipped skipped = manager.getSkippedForDmf(dmf);
        assertEquals(sections.size(), skipped.getAllSections().size());
        //jen existujici sekce z konfigurace GUI
        assertEquals(new HashSet<>(Arrays.asList(skippedName)), skipped.getNamesOfSkippedSections());
        for (RulesSection section : skipped.getAllSections()) {
            assertEquals(section.getName(), !section.getName().equals(skippedName), section.isEnabled());
        }
        //ostatni fDMF bez preskocenych sekci
        assertTrue(manager.getSkippedForDmf(new Dmf(Dmf.Type.MONOGRAPH, "2.0")).getNamesOfSkippedSections().isEmpty());
    }

    @Test
    public void managersDoNotShareSections() throws Exception {
        Dmf dmf = new Dmf(Dmf.Type.MONOGRAPH, "2.0");
        SkippedManagerImpl first = new SkippedManagerImpl(configurationManager, validationDataManager);
        first.getSkippedForDmf(dmf).getAllSections().get(0).setEnabled(false);
        SkippedManagerImpl second = new SkippedManagerImpl(configurationManager, validationDataManager);
        assertTrue(second.getSkippedForDmf(dmf).getAllSections().get(0).isEnabled());
        Set<String> names = first.getSkippedForDmf(dmf).getNamesOfSkippedSections();
        assertEquals(1, names.size());
    }
}
