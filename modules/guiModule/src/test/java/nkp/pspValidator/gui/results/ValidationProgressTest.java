package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Průběh validace z událostí jádra: podíl hotových pravidel, texty panelu průběhu a průběžné počty.
 * <p>
 * Vzorová validace: sekce A (id 10, pravidla A1 a A2), B (id 20, pravidlo B1), C (id 30, pravidla C1 a C2),
 * celkem 5 pravidel; celkové pořadí A1=1, A2=2, B1=3, C1=4, C2=5.
 */
public class ValidationProgressTest {

    private static final double DELTA = 1e-9;

    private static ValidationProgress sample() {
        return ValidationProgress.builder()
                .section(10, "A").rule(1, "A1").rule(2, "A2")
                .section(20, "B").rule(5, "B1")
                .section(30, "C").rule(7, "C1").rule(8, "C2")
                .build();
    }

    @Test
    public void beforeFirstSectionIsInitializing() {
        ValidationProgress progress = sample();
        assertFalse(progress.isStarted());
        assertEquals(-1, progress.getFraction(), DELTA);
        assertEquals("Inicializuji…", progress.getTitle());
        assertNull(progress.getCountsText());
        assertEquals("Rozpoznávám typ a verzi balíku a připravuji validátor.", progress.getDetail());
        assertFalse(progress.isRuleRunning());
        assertEquals(ProblemCounts.ZERO, progress.getFound());
        assertEquals(5, progress.getTotalRules());
        assertEquals(3, progress.getTotalSections());
        assertEquals(0, progress.getCompletedRules());
    }

    @Test
    public void sectionStartedBeforeItsFirstRule() {
        ValidationProgress progress = sample();
        progress.sectionStarted(10);
        assertTrue(progress.isStarted());
        assertEquals(0, progress.getFraction(), DELTA);
        assertEquals("Sekce 1 z 3: A", progress.getTitle());
        assertEquals("pravidel v sekci: 2, celkem 5", progress.getCountsText());
        assertNull(progress.getDetail());
        assertFalse(progress.isRuleRunning());
    }

    @Test
    public void runningRuleTexts() {
        ValidationProgress progress = sample();
        progress.sectionStarted(10);
        progress.ruleStarted(10, 1);
        assertEquals("pravidlo 1 z 2, celkem 1 z 5", progress.getCountsText());
        assertEquals("A1", progress.getDetail());
        assertTrue(progress.isRuleRunning());
        //pravidlo jeste nedobehlo
        assertEquals(0, progress.getFraction(), DELTA);
    }

    @Test
    public void finishedRulesAdvanceFractionAndCounts() {
        ValidationProgress progress = sample();
        progress.sectionStarted(10);
        progress.ruleStarted(10, 1);
        progress.ruleFinished(10, 1, new ProblemCounts(2, 0, 1));
        assertEquals(0.2, progress.getFraction(), DELTA);
        assertFalse(progress.isRuleRunning());
        //mezi pravidly zustava naposledy spustene pravidlo
        assertEquals("A1", progress.getDetail());
        assertEquals("pravidlo 1 z 2, celkem 1 z 5", progress.getCountsText());
        assertEquals(new ProblemCounts(2, 0, 1), progress.getFound());

        progress.ruleStarted(10, 2);
        progress.ruleFinished(10, 2, new ProblemCounts(0, 3, 4));
        assertEquals(0.4, progress.getFraction(), DELTA);
        assertEquals(new ProblemCounts(2, 3, 5), progress.getFound());
        assertEquals(2, progress.getCompletedRules());
    }

    @Test
    public void secondSectionTextsUseGlobalOrder() {
        ValidationProgress progress = sample();
        finishSectionA(progress);
        progress.sectionStarted(30);
        progress.ruleStarted(30, 8);
        assertEquals("Sekce 3 z 3: C", progress.getTitle());
        assertEquals("pravidlo 2 z 2, celkem 5 z 5", progress.getCountsText());
        assertEquals("C2", progress.getDetail());
    }

    @Test
    public void skippedSectionCountsAsDone() {
        ValidationProgress progress = sample();
        finishSectionA(progress);
        assertEquals(0.4, progress.getFraction(), DELTA);
        progress.sectionSkipped(20);
        //B1 (3. pravidlo) se pocita jako hotove
        assertEquals(0.6, progress.getFraction(), DELTA);
        assertEquals(3, progress.getCompletedRules());
        //preskocena sekce se nestava aktualni
        assertEquals("Sekce 1 z 3: A", progress.getTitle());
        assertEquals(new ProblemCounts(0, 0, 0), progress.getFound());
    }

    @Test
    public void skippedFirstSectionBeforeAnyStart() {
        ValidationProgress progress = sample();
        progress.sectionSkipped(10);
        //dokud nezacne zadna sekce, ukazatel je neurcity
        assertEquals(-1, progress.getFraction(), DELTA);
        assertEquals(2, progress.getCompletedRules());
        progress.sectionStarted(20);
        assertEquals(0.4, progress.getFraction(), DELTA);
        assertEquals("Sekce 2 z 3: B", progress.getTitle());
    }

    @Test
    public void lastRuleFinishedIsWhole() {
        ValidationProgress progress = sample();
        finishSectionA(progress);
        progress.sectionStarted(20);
        progress.ruleStarted(20, 5);
        progress.ruleFinished(20, 5, ProblemCounts.ZERO);
        progress.sectionStarted(30);
        progress.ruleStarted(30, 7);
        progress.ruleFinished(30, 7, ProblemCounts.ZERO);
        assertEquals(0.8, progress.getFraction(), DELTA);
        progress.ruleStarted(30, 8);
        progress.ruleFinished(30, 8, new ProblemCounts(1, 0, 0));
        assertEquals(1.0, progress.getFraction(), DELTA);
        assertEquals(5, progress.getCompletedRules());
        assertEquals(new ProblemCounts(1, 0, 0), progress.getFound());
    }

    @Test
    public void fractionNeverGoesBack() {
        ValidationProgress progress = sample();
        progress.sectionStarted(30);
        progress.ruleFinished(30, 8, ProblemCounts.ZERO);
        assertEquals(1.0, progress.getFraction(), DELTA);
        //starsi udalost nesmi ukazatel vratit
        progress.ruleFinished(10, 1, ProblemCounts.ZERO);
        assertEquals(1.0, progress.getFraction(), DELTA);
        progress.sectionSkipped(10);
        assertEquals(1.0, progress.getFraction(), DELTA);
    }

    @Test
    public void unknownRuleAddsProblemsButNotProgress() {
        ValidationProgress progress = sample();
        progress.sectionStarted(10);
        progress.ruleFinished(10, 99, new ProblemCounts(1, 1, 1));
        progress.ruleFinished(99, 1, new ProblemCounts(1, 0, 0));
        assertEquals(0, progress.getFraction(), DELTA);
        assertEquals(new ProblemCounts(2, 1, 1), progress.getFound());
        //neznama sekce se nezapocita
        progress.sectionSkipped(99);
        assertEquals(0, progress.getCompletedRules());
    }

    @Test
    public void nullRuleCountsAreIgnored() {
        ValidationProgress progress = sample();
        progress.sectionStarted(10);
        progress.ruleStarted(10, 1);
        progress.ruleFinished(10, 1, null);
        assertEquals(ProblemCounts.ZERO, progress.getFound());
        assertEquals(0.2, progress.getFraction(), DELTA);
    }

    @Test
    public void finishOfOtherRuleKeepsRunningFlag() {
        ValidationProgress progress = sample();
        progress.sectionStarted(10);
        progress.ruleStarted(10, 2);
        progress.ruleFinished(10, 1, ProblemCounts.ZERO);
        assertTrue(progress.isRuleRunning());
        //stejne id pravidla v jine sekci neni bezici pravidlo
        progress.ruleFinished(20, 2, ProblemCounts.ZERO);
        assertTrue(progress.isRuleRunning());
        progress.ruleFinished(10, 2, ProblemCounts.ZERO);
        assertFalse(progress.isRuleRunning());
    }

    @Test
    public void noRulesAtAll() {
        ValidationProgress empty = ValidationProgress.builder().section(1, "Prázdná").build();
        assertEquals(0, empty.getTotalRules());
        assertEquals(-1, empty.getFraction(), DELTA);
        empty.sectionStarted(1);
        assertEquals(1.0, empty.getFraction(), DELTA);
        assertEquals("Sekce 1 z 1: Prázdná", empty.getTitle());
        assertEquals("pravidel v sekci: 0, celkem 0", empty.getCountsText());
        assertNull(empty.getDetail());
    }

    @Test(expected = IllegalStateException.class)
    public void ruleWithoutSectionIsRejected() {
        ValidationProgress.builder().rule(1, "X");
    }

    @Test
    public void fromCoreSectionsPairedById() {
        RulesSection a = new RulesSection(10, "A", "popis A");
        RulesSection b = new RulesSection(20, "B", null);
        RulesSection c = new RulesSection(30, "C", null);
        //jadro posila v mape kopie sekci (jine instance), parovat se musi podle id
        Map<RulesSection, List<Rule>> rules = new HashMap<>();
        rules.put(a.copy(), Arrays.asList(rule(10, 1, "A1"), rule(10, 2, "A2")));
        rules.put(c.copy(), Arrays.asList(rule(30, 7, "C1"), rule(30, 8, "C2")));
        //sekce B v mape chybi: nema pravidla
        ValidationProgress progress = ValidationProgress.of(Arrays.asList(a, b, c), rules);
        assertEquals(4, progress.getTotalRules());
        assertEquals(3, progress.getTotalSections());
        progress.sectionStarted(30);
        progress.ruleStarted(30, 7);
        assertEquals("Sekce 3 z 3: C", progress.getTitle());
        assertEquals("pravidlo 1 z 2, celkem 3 z 4", progress.getCountsText());
        assertEquals("C1", progress.getDetail());
        progress.sectionStarted(20);
        assertEquals("pravidel v sekci: 0, celkem 4", progress.getCountsText());
    }

    @Test
    public void fromCoreWithoutRulesMap() {
        ValidationProgress progress = ValidationProgress.of(new ArrayList<>(Arrays.asList(new RulesSection(1, "A", null))), null);
        assertEquals(0, progress.getTotalRules());
        assertEquals(1, progress.getTotalSections());
    }

    private static Rule rule(int sectionId, int id, String name) {
        return new Rule(sectionId, id, name, null, null);
    }

    private static void finishSectionA(ValidationProgress progress) {
        progress.sectionStarted(10);
        progress.ruleStarted(10, 1);
        progress.ruleFinished(10, 1, ProblemCounts.ZERO);
        progress.ruleStarted(10, 2);
        progress.ruleFinished(10, 2, ProblemCounts.ZERO);
    }
}
