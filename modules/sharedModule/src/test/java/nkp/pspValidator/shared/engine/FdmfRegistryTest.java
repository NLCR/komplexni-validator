package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Konzistence FdmfRegistry s adresari fDMF a s vychozimi verzemi v DmfDetector. Nahrazuje puvodni
 * ValidatorFactoryRegistryTest s natvrdo psanymi pocty verzi, ktery se musel upravovat pri kazde nove fDMF.
 */
public class FdmfRegistryTest {

    private static FdmfRegistry registry;

    @BeforeClass
    public static void loadRegistry() throws Exception {
        ValidatorConfigurationManager configManager = FdmfTestSupport.configManager();
        registry = new FdmfRegistry(configManager);
    }

    @Test
    public void registrySizeEqualsNumberOfFdmfDirs() {
        int total = 0;
        for (Dmf.Type type : Dmf.Type.values()) {
            total += FdmfTestSupport.versions(registry, type).size();
        }
        assertEquals(FdmfTestSupport.fdmfDirs().length, total);
    }

    @Test
    public void everyFdmfDirIsInRegistryUnderItsTypeAndVersion() {
        for (File dir : FdmfTestSupport.fdmfDirs()) {
            Dmf dmf = FdmfTestSupport.dmfFromDirName(dir.getName());
            assertTrue(dir.getName() + " neni v registru", FdmfTestSupport.versions(registry, dmf.getType()).contains(dmf.getVersion()));
        }
    }

    @Test
    public void everyTypeHasAtLeastOneVersion() {
        for (Dmf.Type type : Dmf.Type.values()) {
            assertFalse("zadna fDMF pro typ " + type, FdmfTestSupport.versions(registry, type).isEmpty());
        }
    }

    @Test
    public void defaultVersionsAreInRegistry() throws Exception {
        assertDefault(Dmf.Type.MONOGRAPH, DmfDetector.DEFAULT_MONOGRAPH_VERSION);
        assertDefault(Dmf.Type.PERIODICAL, DmfDetector.DEFAULT_PERIODICAL_VERSION);
        assertDefault(Dmf.Type.AUDIO_GRAM, DmfDetector.DEFAULT_AUDIO_GRAM_VERSION);
        assertDefault(Dmf.Type.AUDIO_FONO, DmfDetector.DEFAULT_AUDIO_FONO_VERSION);
        assertDefault(Dmf.Type.AUDIO_DISC, DmfDetector.DEFAULT_AUDIO_DISC_VERSION);
        assertDefault(Dmf.Type.AUDIO_NO_CARRIER, DmfDetector.DEFAULT_AUDIO_NO_CARRIER_VERSION);
        assertDefault(Dmf.Type.DATA_DISC, DmfDetector.DEFAULT_DATA_DISC_VERSION);
        assertDefault(Dmf.Type.FUND_UNIT, DmfDetector.DEFAULT_FUND_UNIT_VERSION);
    }

    @Test
    public void defaultConstantsCoverAllTypes() {
        // pokud pribude Dmf.Type, musi pribyt i konstanta DEFAULT_*_VERSION a radek v defaultVersionsAreInRegistry
        Set<Dmf.Type> covered = new HashSet<>();
        for (java.lang.reflect.Field f : DmfDetector.class.getFields()) {
            if (f.getName().startsWith("DEFAULT_") && f.getName().endsWith("_VERSION")) {
                covered.add(Dmf.Type.valueOf(f.getName().substring("DEFAULT_".length(), f.getName().length() - "_VERSION".length())));
            }
        }
        assertEquals(new HashSet<>(java.util.Arrays.asList(Dmf.Type.values())), covered);
    }

    private void assertDefault(Dmf.Type type, String version) throws Exception {
        assertNotNull(registry.getFdmfConfig(new Dmf(type, version)));
    }
}
