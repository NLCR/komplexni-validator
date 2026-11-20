package nkp.pspValidator.gui.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Zavření okna Nastavení: dotaz jen při neuložených změnách, Zpět i zavření dotazu nechají okno otevřené. */
public class CloseDecisionTest {

    @Test
    public void withoutChangesClosesWithoutAsking() {
        assertEquals(CloseDecision.Action.CLOSE, CloseDecision.onCloseRequest(false));
    }

    @Test
    public void withChangesAsks() {
        assertEquals(CloseDecision.Action.ASK, CloseDecision.onCloseRequest(true));
    }

    @Test
    public void choices() {
        assertEquals(CloseDecision.Action.SAVE_AND_CLOSE, CloseDecision.afterPrompt(CloseDecision.Choice.SAVE));
        assertEquals(CloseDecision.Action.DISCARD_AND_CLOSE, CloseDecision.afterPrompt(CloseDecision.Choice.DISCARD));
        assertEquals(CloseDecision.Action.STAY, CloseDecision.afterPrompt(CloseDecision.Choice.BACK));
    }

    /** Křížek nebo Esc v dotazu (bez volby) je Zpět: nic se neuloží ani nezahodí. */
    @Test
    public void promptClosedWithoutChoiceStays() {
        assertEquals(CloseDecision.Action.STAY, CloseDecision.afterPrompt(null));
    }
}
