package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class ValidatorFactoryRegistryTest {

    @Test
    public void patternTest() {
        String pattern = "monograph_[0-9]+(\\.([0-9])+)*";
        assertTrue("monograph_7".matches(pattern));
        assertTrue("monograph_7.18".matches(pattern));
        assertTrue("monograph_7.18.300".matches(pattern));

        assertFalse("monograph_".matches(pattern));
        assertFalse("monograph_.".matches(pattern));
        assertFalse("monograph_.3".matches(pattern));
        assertFalse("monograph_3.".matches(pattern));
        assertFalse("monograph_3.ALPHA".matches(pattern));
    }

    @Test
    public void versions() throws ValidatorConfigurationException {
        File validatorConfigDir = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
        ValidatorConfigurationManager validatorConfigManager = new ValidatorConfigurationManager(validatorConfigDir);
        FdmfRegistry registry = new FdmfRegistry(validatorConfigManager);

/*
        assertEquals(2, registry.getMonographFdmfVersions().size());
        assertTrue(registry.getMonographFdmfVersions().contains("1.0"));
        assertTrue(registry.getMonographFdmfVersions().contains("1.2"));

        assertEquals(2, registry.getPeriodicalFdmfVersions().size());
        assertTrue(registry.getPeriodicalFdmfVersions().contains("1.4"));
        assertTrue(registry.getPeriodicalFdmfVersions().contains("1.6"));
*/
        assertEquals(5, registry.getMonographFdmfVersions().size());
        assertTrue(registry.getMonographFdmfVersions().contains("1.0"));
        assertTrue(registry.getMonographFdmfVersions().contains("1.2"));
        assertTrue(registry.getMonographFdmfVersions().contains("1.3"));
        assertTrue(registry.getMonographFdmfVersions().contains("1.3.1"));
        assertTrue(registry.getMonographFdmfVersions().contains("1.3.2"));

        assertEquals(4, registry.getPeriodicalFdmfVersions().size());
        assertTrue(registry.getPeriodicalFdmfVersions().contains("1.4"));
        assertTrue(registry.getPeriodicalFdmfVersions().contains("1.6"));
        assertTrue(registry.getPeriodicalFdmfVersions().contains("1.7"));

        assertEquals(1, registry.getSoundRecordingFdmfVersions().size());
        assertTrue(registry.getSoundRecordingFdmfVersions().contains("0.3"));
    }

}
