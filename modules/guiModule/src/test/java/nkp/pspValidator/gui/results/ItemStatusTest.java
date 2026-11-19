package nkp.pspValidator.gui.results;

import nkp.pspValidator.gui.validation.ProcessingState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Stav řádku sekce nebo pravidla: tečka a text.
 */
public class ItemStatusTest {

    @Test
    public void notFinishedStatesShowText() {
        assertStatus(ItemStatus.WAITING, "status-dot-none", "čeká", ItemStatus.of(ProcessingState.WAITING, 5, 5));
        assertStatus(ItemStatus.RUNNING, "status-dot-running", "probíhá", ItemStatus.of(ProcessingState.RUNNING, 5, 5));
        assertStatus(ItemStatus.SKIPPED, "status-dot-none", "přeskočeno", ItemStatus.of(ProcessingState.SKIPPED, 0, 0));
        assertStatus(ItemStatus.CANCELED, "status-dot-none", "zrušeno", ItemStatus.of(ProcessingState.CANCELED, 1, 0));
        //nedokonceno kvuli chybe validace: text misto poctu i s nalezenou chybou
        assertStatus(ItemStatus.INTERRUPTED, "status-dot-none", "nedokončeno", ItemStatus.of(ProcessingState.INTERRUPTED, 1, 0));
    }

    @Test
    public void finishedByWorstLevel() {
        assertStatus(ItemStatus.ERROR, "status-dot-error", null, ItemStatus.of(ProcessingState.FINISHED, 1, 0));
        assertStatus(ItemStatus.ERROR, "status-dot-error", null, ItemStatus.of(ProcessingState.FINISHED, 1, 3));
        assertStatus(ItemStatus.WARNING, "status-dot-warning", null, ItemStatus.of(ProcessingState.FINISHED, 0, 1));
        //informace na stav nemaji vliv
        assertStatus(ItemStatus.OK, "status-dot-ok", null, ItemStatus.of(ProcessingState.FINISHED, 0, 0));
    }

    private static void assertStatus(ItemStatus expected, String dotClass, String text, ItemStatus actual) {
        assertEquals(expected, actual);
        assertEquals(dotClass, actual.getDotStyleClass());
        if (text == null) {
            assertNull(actual.getText());
        } else {
            assertEquals(text, actual.getText());
        }
    }
}
