package nkp.pspValidator.gui.group;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * Hlídání paměti u velké skupiny: zahazování nejdéle nepoužitých podrobností nad rozpočtem, zamčené balíky.
 */
public class DetailRetentionTest {

    @Test
    public void exactlyBudgetKeepsAllOneMoreEvictsOldest() {
        DetailRetention<String, String> retention = new DetailRetention<>(100);
        retention.put("a", "A", 50);
        retention.put("b", "B", 50);
        assertEquals(100, retention.getTotalWeight());
        assertEquals("A", retention.get("a"));
        assertEquals("B", retention.get("b"));
        assertFalse(retention.wasEvicted("a"));

        retention.put("c", "C", 1);
        //"a" je nejdele nepouzity (get("a") probehl pred get("b"))
        assertNull(retention.get("a"));
        assertTrue(retention.wasEvicted("a"));
        assertEquals("B", retention.get("b"));
        assertEquals("C", retention.get("c"));
        assertEquals(51, retention.getTotalWeight());
        assertEquals(2, retention.size());
        assertEquals(Collections.singleton("a"), retention.getEvicted());
    }

    @Test
    public void accessMakesEntryRecent() {
        DetailRetention<String, String> retention = new DetailRetention<>(100);
        retention.put("a", "A", 40);
        retention.put("b", "B", 40);
        retention.get("a");
        retention.put("c", "C", 40);
        assertEquals("A", retention.get("a"));
        assertNull(retention.get("b"));
        assertTrue(retention.wasEvicted("b"));
        assertFalse(retention.wasEvicted("a"));
    }

    @Test
    public void pinnedEntriesAreNeverEvicted() {
        DetailRetention<String, String> retention = new DetailRetention<>(100);
        retention.put("a", "A", 60);
        retention.setPinned(new HashSet<>(Arrays.asList("a", null)));
        retention.put("b", "B", 30);
        retention.put("c", "C", 30);
        //a zamcene, zahodi se b
        assertEquals("A", retention.get("a"));
        assertNull(retention.get("b"));
        assertEquals("C", retention.get("c"));
        //jediny zamceny balík nad rozpoctem zustava
        retention.setPinned(Collections.singleton("c"));
        retention.updateWeight("c", 500);
        assertNull(retention.get("a"));
        assertEquals("C", retention.get("c"));
        assertEquals(500, retention.getTotalWeight());
    }

    @Test
    public void unpinningEvictsOverBudget() {
        DetailRetention<String, String> retention = new DetailRetention<>(10);
        retention.setPinned(new HashSet<>(Arrays.asList("a", "b")));
        retention.put("a", "A", 8);
        retention.put("b", "B", 8);
        assertEquals(2, retention.size());
        retention.setPinned(Collections.singleton("b"));
        assertNull(retention.get("a"));
        assertEquals("B", retention.get("b"));
    }

    @Test
    public void removeIsNotEvictionAndPutAgainClearsFlag() {
        DetailRetention<String, String> retention = new DetailRetention<>(10);
        retention.put("a", "A", 8);
        retention.remove("a");
        assertFalse(retention.wasEvicted("a"));
        assertEquals(0, retention.getTotalWeight());
        retention.put("a", "A", 8);
        retention.put("b", "B", 8);
        assertTrue(retention.wasEvicted("a"));
        retention.put("a", "A2", 1);
        assertFalse(retention.wasEvicted("a"));
        assertEquals("A2", retention.get("a"));
        assertEquals(9, retention.getTotalWeight());
    }

    @Test
    public void replaceAndNegativeWeight() {
        DetailRetention<String, String> retention = new DetailRetention<>(10);
        retention.put("a", "A", 5);
        retention.put("a", "A2", 7);
        assertEquals(7, retention.getTotalWeight());
        assertEquals(1, retention.size());
        retention.put("b", "B", -3);
        assertEquals(7, retention.getTotalWeight());
        retention.updateWeight("neni", 100);
        assertEquals(7, retention.getTotalWeight());
        retention.clear();
        assertEquals(0, retention.getTotalWeight());
        assertEquals(0, retention.size());
        assertNull(retention.get("a"));
    }
}
