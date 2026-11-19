package nkp.pspValidator.gui.results;

import nkp.pspValidator.gui.validation.ProcessingState;
import nkp.pspValidator.gui.validation.RuleWithState;
import nkp.pspValidator.gui.validation.SectionWithState;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Filtr výsledků: úrovně problémů a „jen sekce a pravidla s problémy“.
 */
public class ProblemFilterTest {

    private static SectionWithState section(ProcessingState state, int errors, int warnings, int infos) {
        SectionWithState section = new SectionWithState(new RulesSection(1, "Sekce", "popis"));
        section.setState(state);
        section.setErrors(errors);
        section.setWarnings(warnings);
        section.setInfos(infos);
        return section;
    }

    private static RuleWithState rule(ProcessingState state, int errors, int warnings, int infos) {
        RuleWithState rule = new RuleWithState(new Rule(1, 2, "PRAVIDLO", "popis", null));
        rule.setState(state);
        rule.setErrors(errors);
        rule.setWarnings(warnings);
        rule.setInfos(infos);
        return rule;
    }

    @Test
    public void defaultShowsEverything() {
        ProblemFilter filter = ProblemFilter.all();
        assertTrue(filter.isErrors());
        assertTrue(filter.isWarnings());
        assertTrue(filter.isInfos());
        assertFalse(filter.isOnlyWithProblems());
        for (Level level : Level.values()) {
            assertTrue(filter.accepts(level));
        }
        assertTrue(filter.showSection(section(ProcessingState.FINISHED, 0, 0, 0)));
        assertTrue(filter.showRule(rule(ProcessingState.FINISHED, 0, 0, 0)));
        assertEquals(6, filter.visibleCount(1, 2, 3));
    }

    @Test
    public void eachLevelSeparately() {
        ProblemFilter errorsOnly = new ProblemFilter(true, false, false, false);
        assertTrue(errorsOnly.accepts(Level.ERROR));
        assertFalse(errorsOnly.accepts(Level.WARNING));
        assertFalse(errorsOnly.accepts(Level.INFO));
        assertEquals(1, errorsOnly.visibleCount(1, 2, 4));

        ProblemFilter warningsOnly = new ProblemFilter(false, true, false, false);
        assertFalse(warningsOnly.accepts(Level.ERROR));
        assertTrue(warningsOnly.accepts(Level.WARNING));
        assertFalse(warningsOnly.accepts(Level.INFO));
        assertEquals(2, warningsOnly.visibleCount(1, 2, 4));

        ProblemFilter infosOnly = new ProblemFilter(false, false, true, false);
        assertFalse(infosOnly.accepts(Level.ERROR));
        assertFalse(infosOnly.accepts(Level.WARNING));
        assertTrue(infosOnly.accepts(Level.INFO));
        assertEquals(4, infosOnly.visibleCount(1, 2, 4));

        ProblemFilter nothing = new ProblemFilter(false, false, false, false);
        assertEquals(0, nothing.visibleCount(1, 2, 4));
    }

    @Test
    public void onlyWithProblemsHidesFinishedWithoutVisibleProblems() {
        ProblemFilter errorsAndWarnings = new ProblemFilter(true, true, false, true);
        assertFalse(errorsAndWarnings.showSection(section(ProcessingState.FINISHED, 0, 0, 0)));
        //jen informace, ktere se nezobrazuji
        assertFalse(errorsAndWarnings.showSection(section(ProcessingState.FINISHED, 0, 0, 7)));
        assertTrue(errorsAndWarnings.showSection(section(ProcessingState.FINISHED, 1, 0, 0)));
        assertTrue(errorsAndWarnings.showSection(section(ProcessingState.FINISHED, 0, 1, 0)));
        assertFalse(errorsAndWarnings.showRule(rule(ProcessingState.FINISHED, 0, 0, 7)));
        assertTrue(errorsAndWarnings.showRule(rule(ProcessingState.FINISHED, 0, 1, 0)));

        ProblemFilter withInfos = new ProblemFilter(true, true, true, true);
        assertTrue(withInfos.showSection(section(ProcessingState.FINISHED, 0, 0, 1)));
        assertTrue(withInfos.showRule(rule(ProcessingState.FINISHED, 0, 0, 1)));
    }

    @Test
    public void onlyWithProblemsKeepsWaitingAndRunning() {
        ProblemFilter filter = new ProblemFilter(true, true, true, true);
        assertTrue(filter.showSection(section(ProcessingState.WAITING, 0, 0, 0)));
        assertTrue(filter.showSection(section(ProcessingState.RUNNING, 0, 0, 0)));
        assertTrue(filter.showRule(rule(ProcessingState.WAITING, 0, 0, 0)));
        assertTrue(filter.showRule(rule(ProcessingState.RUNNING, 0, 0, 0)));
        //preskocene a zrusene bez problemu se skryji
        assertFalse(filter.showSection(section(ProcessingState.SKIPPED, 0, 0, 0)));
        assertFalse(filter.showSection(section(ProcessingState.CANCELED, 0, 0, 0)));
        assertTrue(filter.showSection(section(ProcessingState.CANCELED, 2, 0, 0)));
        assertFalse(filter.showRule(rule(ProcessingState.CANCELED, 0, 0, 0)));
    }
}
