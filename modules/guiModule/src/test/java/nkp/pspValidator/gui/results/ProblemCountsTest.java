package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.engine.Level;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Počty problémů podle úrovně.
 */
public class ProblemCountsTest {

    @Test
    public void fromCoreMap() {
        Map<Level, Integer> map = new EnumMap<>(Level.class);
        map.put(Level.ERROR, 3);
        map.put(Level.WARNING, 0);
        map.put(Level.INFO, 12);
        ProblemCounts counts = ProblemCounts.of(map);
        assertEquals(3, counts.getErrors());
        assertEquals(0, counts.getWarnings());
        assertEquals(12, counts.getInfos());
    }

    @Test
    public void missingLevelsAndMissingMapAreZero() {
        Map<Level, Integer> onlyWarnings = new EnumMap<>(Level.class);
        onlyWarnings.put(Level.WARNING, 2);
        assertEquals(new ProblemCounts(0, 2, 0), ProblemCounts.of(onlyWarnings));
        assertEquals(ProblemCounts.ZERO, ProblemCounts.of(null));
        Map<Level, Integer> withNull = new EnumMap<>(Level.class);
        withNull.put(Level.ERROR, null);
        assertEquals(ProblemCounts.ZERO, ProblemCounts.of(withNull));
    }

    @Test
    public void plusAddsEachLevelSeparately() {
        ProblemCounts sum = new ProblemCounts(1, 2, 3).plus(new ProblemCounts(10, 0, 30));
        assertEquals(11, sum.getErrors());
        assertEquals(2, sum.getWarnings());
        assertEquals(33, sum.getInfos());
    }

    @Test
    public void equalityByAllLevels() {
        assertEquals(new ProblemCounts(1, 2, 3), new ProblemCounts(1, 2, 3));
        assertEquals(new ProblemCounts(1, 2, 3).hashCode(), new ProblemCounts(1, 2, 3).hashCode());
        assertNotEquals(new ProblemCounts(1, 2, 3), new ProblemCounts(0, 2, 3));
        assertNotEquals(new ProblemCounts(1, 2, 3), new ProblemCounts(1, 0, 3));
        assertNotEquals(new ProblemCounts(1, 2, 3), new ProblemCounts(1, 2, 0));
    }
}
