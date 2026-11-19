package nkp.pspValidator.gui.validation;

import javafx.collections.ListChangeListener;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Konec validace zrušením nebo chybou: nedokončené sekce a pravidla dostanou stav, hotové a přeskočené zůstanou.
 */
public class ValidationStateManagerTest {

    private static ValidationStateManager manager() {
        RulesSection a = new RulesSection(1, "A", null);
        RulesSection b = new RulesSection(2, "B", null);
        RulesSection c = new RulesSection(3, "C", null);
        Map<RulesSection, List<Rule>> rules = new HashMap<>();
        rules.put(a, Arrays.asList(new Rule(1, 1, "A1", null, null), new Rule(1, 2, "A2", null, null)));
        rules.put(b, Arrays.asList(new Rule(2, 1, "B1", null, null), new Rule(2, 2, "B2", null, null), new Rule(2, 3, "B3", null, null)));
        rules.put(c, Arrays.asList(new Rule(3, 1, "C1", null, null)));
        return new ValidationStateManager(Arrays.asList(a, b, c), rules);
    }

    @Test
    public void unfinishedItemsGetStateFinishedAndSkippedStay() {
        ValidationStateManager manager = manager();
        //A hotova, B bezi (B1 hotove, B2 bezi, B3 ceka), C ceka; preskocena sekce v dalsich testech
        manager.updateSectionStatus(1, ProcessingState.FINISHED);
        manager.updateRuleState(1, 1, ProcessingState.FINISHED);
        manager.updateRuleState(1, 2, ProcessingState.FINISHED);
        manager.updateSectionStatus(2, ProcessingState.RUNNING);
        manager.updateRuleState(2, 1, ProcessingState.FINISHED);
        manager.updateRuleState(2, 2, ProcessingState.RUNNING);

        AtomicInteger sectionChanges = new AtomicInteger();
        manager.getSectionsObservable().addListener((ListChangeListener<SectionWithState>) change -> sectionChanges.incrementAndGet());
        AtomicInteger ruleChangesB = new AtomicInteger();
        manager.getRulesObervable(2).addListener((ListChangeListener<RuleWithState>) change -> ruleChangesB.incrementAndGet());

        manager.markUnfinished(ProcessingState.CANCELED);

        assertEquals(ProcessingState.FINISHED, section(manager, 0).getState());
        assertEquals(ProcessingState.CANCELED, section(manager, 1).getState());
        assertEquals(ProcessingState.CANCELED, section(manager, 2).getState());
        assertEquals(ProcessingState.FINISHED, rule(manager, 1, 0).getState());
        assertEquals(ProcessingState.FINISHED, rule(manager, 1, 1).getState());
        assertEquals(ProcessingState.FINISHED, rule(manager, 2, 0).getState());
        assertEquals(ProcessingState.CANCELED, rule(manager, 2, 1).getState());
        assertEquals(ProcessingState.CANCELED, rule(manager, 2, 2).getState());
        assertEquals(ProcessingState.CANCELED, rule(manager, 3, 0).getState());
        //seznamy hlasi zmenu jen u zmenenych polozek (sekce B a C, pravidla B2 a B3), aby se prekreslily radky
        assertEquals(2, sectionChanges.get());
        assertEquals(2, ruleChangesB.get());
    }

    @Test
    public void skippedSectionMarksItsRulesSkipped() {
        ValidationStateManager manager = manager();
        manager.updateSectionStatus(1, ProcessingState.SKIPPED);
        //pravidla preskocene sekce jadro jednotlive nehlasi
        assertEquals(ProcessingState.SKIPPED, rule(manager, 1, 0).getState());
        assertEquals(ProcessingState.SKIPPED, rule(manager, 1, 1).getState());
        //ostatni sekce se nemeni
        assertEquals(ProcessingState.WAITING, rule(manager, 2, 0).getState());
        assertEquals(ProcessingState.WAITING, section(manager, 1).getState());
    }

    @Test
    public void skippedSectionAndItsRulesStayAfterFailure() {
        ValidationStateManager manager = manager();
        manager.updateSectionStatus(1, ProcessingState.SKIPPED);
        manager.markUnfinished(ProcessingState.INTERRUPTED);
        assertEquals(ProcessingState.SKIPPED, section(manager, 0).getState());
        assertEquals(ProcessingState.SKIPPED, rule(manager, 1, 0).getState());
        assertEquals(ProcessingState.SKIPPED, rule(manager, 1, 1).getState());
        assertEquals(ProcessingState.INTERRUPTED, section(manager, 1).getState());
        assertEquals(ProcessingState.INTERRUPTED, rule(manager, 2, 0).getState());
        assertEquals(ProcessingState.INTERRUPTED, section(manager, 2).getState());
        assertEquals(ProcessingState.INTERRUPTED, rule(manager, 3, 0).getState());
    }

    private static SectionWithState section(ValidationStateManager manager, int index) {
        return manager.getSectionsObservable().get(index);
    }

    private static RuleWithState rule(ValidationStateManager manager, int sectionId, int index) {
        return manager.getRulesObervable(sectionId).get(index);
    }
}
