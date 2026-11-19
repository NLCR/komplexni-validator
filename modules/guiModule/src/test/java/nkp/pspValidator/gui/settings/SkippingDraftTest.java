package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.gui.skipping.Skipped;
import nkp.pspValidator.gui.skipping.SkippedManager;
import nkp.pspValidator.gui.skipping.SkippedManagerImpl;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.RulesSection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.*;

import static org.junit.Assert.*;

/** Záložka Přeskakovaná pravidla: výběr typu a verze, přeskakování sekcí v konceptu a uložení přes SkippedManager. */
public class SkippingDraftTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** Manažer nad pevným seznamem fDMF a sekcí, počítá uložení. */
    private static class FakeManager implements SkippedManager {
        final List<Dmf> dmfs = new ArrayList<>();
        final Map<Dmf, Skipped> skipped = new HashMap<>();
        int saved = 0;

        FakeManager add(Dmf.Type type, String version, String... sectionNames) {
            Dmf dmf = new Dmf(type, version);
            List<RulesSection> sections = new ArrayList<>();
            for (int i = 0; i < sectionNames.length; i++) {
                String name = sectionNames[i];
                boolean skip = name.startsWith("-");
                RulesSection section = new RulesSection(i, skip ? name.substring(1) : name, null);
                section.setEnabled(!skip);
                sections.add(section);
            }
            Skipped s = new Skipped();
            s.setAllSections(sections);
            dmfs.add(dmf);
            skipped.put(dmf, s);
            return this;
        }

        @Override
        public void setSkippedForDmf(Dmf dmf, Skipped s) {
            skipped.put(dmf, s);
        }

        @Override
        public Skipped getSkippedForDmf(Dmf dmf) {
            return skipped.get(dmf);
        }

        @Override
        public List<Dmf> getDmfList() {
            return dmfs;
        }

        @Override
        public void save() {
            saved++;
        }
    }

    private static FakeManager sample() {
        //poradi jako v SkippedManagerImpl: typy podle starych zalozek, verze vzestupne
        return new FakeManager()
                .add(Dmf.Type.MONOGRAPH, "1.3", "A", "B")
                .add(Dmf.Type.MONOGRAPH, "2.3", "A", "B", "C")
                .add(Dmf.Type.MONOGRAPH, "1.3.2", "A")
                .add(Dmf.Type.AUDIO_GRAM, "1.0", "Z", "-Y")
                .add(Dmf.Type.FUND_UNIT, "0.2", "F")
                .add(Dmf.Type.EPERIODICAL, "2.6", "E");
    }

    @Test
    public void typesInGroupOrderAndVersionsNewestFirst() {
        SkippingDraft draft = new SkippingDraft(sample());
        assertEquals(Arrays.asList(Dmf.Type.MONOGRAPH, Dmf.Type.FUND_UNIT, Dmf.Type.EPERIODICAL, Dmf.Type.AUDIO_GRAM), draft.types());
        assertEquals(Arrays.asList("2.3", "1.3.2", "1.3"), draft.versions(Dmf.Type.MONOGRAPH));
        assertEquals(Collections.emptyList(), draft.versions(Dmf.Type.PERIODICAL));
    }

    @Test
    public void initialDmf() {
        SkippingDraft draft = new SkippingDraft(sample());
        //pozadovana existujici fDMF
        assertEquals(new Dmf(Dmf.Type.FUND_UNIT, "0.2"), draft.initialDmf(new Dmf(Dmf.Type.FUND_UNIT, "0.2")));
        //neexistujici nebo zadna: prvni s preskocenou sekci
        assertEquals(new Dmf(Dmf.Type.AUDIO_GRAM, "1.0"), draft.initialDmf(new Dmf(Dmf.Type.FUND_UNIT, "9.9")));
        assertEquals(new Dmf(Dmf.Type.AUDIO_GRAM, "1.0"), draft.initialDmf(null));
        //nic se nepreskakuje: nejnovejsi verze prvniho typu
        FakeManager none = new FakeManager().add(Dmf.Type.PERIODICAL, "2.1", "A").add(Dmf.Type.MONOGRAPH, "2.2", "A").add(Dmf.Type.MONOGRAPH, "2.3", "A");
        assertEquals(new Dmf(Dmf.Type.MONOGRAPH, "2.3"), new SkippingDraft(none).initialDmf(null));
        //zadna fDMF
        assertNull(new SkippingDraft(new FakeManager()).initialDmf(null));
    }

    @Test
    public void skipAndRevert() {
        FakeManager manager = sample();
        SkippingDraft draft = new SkippingDraft(manager);
        Dmf mon = new Dmf(Dmf.Type.MONOGRAPH, "2.3");
        assertEquals(0, draft.skippedCount(mon));
        assertFalse(draft.isChanged());

        RulesSection b = draft.sections(mon).get(1);
        draft.setSkipped(b, true);
        assertTrue(draft.isSkipped(b));
        assertEquals(1, draft.skippedCount(mon));
        assertTrue(draft.isChanged());

        draft.setSkipped(b, false);
        assertEquals(0, draft.skippedCount(mon));
        assertFalse(draft.isChanged());
        assertEquals(0, manager.saved);
    }

    @Test
    public void unskipAll() {
        FakeManager manager = sample();
        SkippingDraft draft = new SkippingDraft(manager);
        Dmf gram = new Dmf(Dmf.Type.AUDIO_GRAM, "1.0");
        assertEquals(1, draft.skippedCount(gram));
        draft.unskipAll(gram);
        assertEquals(0, draft.skippedCount(gram));
        assertTrue(draft.isChanged());
        //jine fDMF se nezmenily
        assertEquals(0, draft.skippedCount(new Dmf(Dmf.Type.MONOGRAPH, "2.3")));
        draft.save();
        assertEquals(1, manager.saved);
    }

    @Test
    public void unknownDmfHasNoSections() {
        SkippingDraft draft = new SkippingDraft(sample());
        Dmf unknown = new Dmf(Dmf.Type.PERIODICAL, "2.2");
        assertTrue(draft.sections(unknown).isEmpty());
        assertEquals(0, draft.skippedCount(unknown));
    }

    @Test
    public void savesToSameKeyAsOldDialog() throws Exception {
        ConfigurationManager config = GuiTestSupport.configurationManager(tmp.getRoot());
        FdmfRegistry registry = GuiTestSupport.registry();
        ValidationDataManager data = new ValidationDataManager(config);
        data.setFdmfRegistry(registry);
        Dmf dmf = new Dmf(Dmf.Type.FUND_UNIT, "0.2");
        String name = registry.getRulesSections(dmf).get(1).getName();

        SkippingDraft draft = new SkippingDraft(new SkippedManagerImpl(config, data));
        RulesSection section = draft.sections(dmf).get(1);
        assertEquals(name, section.getName());
        draft.setSkipped(section, true);
        //pred ulozenim nic
        assertTrue(config.getStringSet("skipped.validation.sections.FUND_UNIT-0.2").isEmpty());
        draft.save();
        assertEquals(Collections.singleton(name), config.getStringSet("skipped.validation.sections.FUND_UNIT-0.2"));

        //znovu otevrena zalozka vidi ulozeny stav
        SkippingDraft reopened = new SkippingDraft(new SkippedManagerImpl(config, data));
        assertEquals(1, reopened.skippedCount(dmf));
        assertEquals(dmf, reopened.initialDmf(null));
    }
}
