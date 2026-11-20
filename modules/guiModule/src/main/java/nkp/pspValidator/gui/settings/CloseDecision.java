package nkp.pspValidator.gui.settings;

/**
 * Zavření okna Nastavení (tlačítko Zrušit/Zavřít, Esc, křížek): bez neuložených změn se okno zavře hned, jinak se
 * uživatel zeptá „Uložit změny?“ s volbami Uložit, Neukládat a Zpět. Zavření dotazu bez volby (křížek, Esc) je Zpět:
 * nic se neztratí a Nastavení zůstane otevřené. Bez JavaFX.
 */
public final class CloseDecision {

    /** Volba v dotazu „Uložit změny?“. */
    public enum Choice {
        SAVE, DISCARD, BACK
    }

    /** Co se má s oknem udělat. */
    public enum Action {
        /** zavřít (žádné neuložené změny) */
        CLOSE,
        /** zeptat se „Uložit změny?“ */
        ASK,
        /** uložit změny a zavřít */
        SAVE_AND_CLOSE,
        /** zahodit změny a zavřít */
        DISCARD_AND_CLOSE,
        /** nechat Nastavení otevřené (změny zůstávají neuložené) */
        STAY
    }

    private CloseDecision() {
    }

    /** Požadavek na zavření okna: {@link Action#CLOSE} bez neuložených změn, jinak {@link Action#ASK}. */
    public static Action onCloseRequest(boolean unsavedChanges) {
        return unsavedChanges ? Action.ASK : Action.CLOSE;
    }

    /** Volba v dotazu; null = dotaz zavřen bez volby (křížek, Esc), stejně jako Zpět. */
    public static Action afterPrompt(Choice choice) {
        if (choice == null) {
            return Action.STAY;
        }
        switch (choice) {
            case SAVE:
                return Action.SAVE_AND_CLOSE;
            case DISCARD:
                return Action.DISCARD_AND_CLOSE;
            case BACK:
                return Action.STAY;
            default:
                throw new IllegalArgumentException("neznámá volba " + choice);
        }
    }
}
