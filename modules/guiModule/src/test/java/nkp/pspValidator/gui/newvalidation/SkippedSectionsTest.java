package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.RulesSection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Přeskakované sekce v dialogu Nová validace: stejný klíč jako dialog přeskakování, jen sekce, které fDMF má.
 */
public class SkippedSectionsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;
    private SkippedSections skippedSections;
    private final Dmf dmf = new Dmf(Dmf.Type.FUND_UNIT, "0.2");

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("cfg"));
        registry = GuiTestSupport.registry();
        skippedSections = new SkippedSections(config, registry);
    }

    @Test
    public void skippedInSectionOrderOnlyExisting() throws Exception {
        List<RulesSection> sections = registry.getRulesSections(dmf);
        String second = sections.get(1).getName();
        String fourth = sections.get(3).getName();
        config.setStringSet(ConfigurationManager.propSkippedValidationSections(dmf), new HashSet<>(Arrays.asList(fourth, "neexistujici", second)));
        assertEquals(Arrays.asList(second, fourth), skippedSections.skipped(dmf));
        assertEquals("skipped.validation.sections.FUND_UNIT-0.2", ConfigurationManager.propSkippedValidationSections(dmf));
    }

    @Test
    public void unskipOneAndAll() throws Exception {
        List<RulesSection> sections = registry.getRulesSections(dmf);
        String a = sections.get(0).getName();
        String b = sections.get(2).getName();
        config.setStringSet(ConfigurationManager.propSkippedValidationSections(dmf), new HashSet<>(Arrays.asList(a, b)));
        skippedSections.unskip(dmf, a);
        assertEquals(Arrays.asList(b), skippedSections.skipped(dmf));
        assertEquals(new HashSet<>(Arrays.asList(b)), config.getStringSet(ConfigurationManager.propSkippedValidationSections(dmf)));
        skippedSections.unskipAll(dmf);
        assertTrue(skippedSections.skipped(dmf).isEmpty());
    }

    @Test
    public void unknownFdmf() {
        assertNull(skippedSections.skipped(new Dmf(Dmf.Type.FUND_UNIT, "9.9")));
        assertTrue(skippedSections.skipped(dmf).isEmpty());
    }
}
