package nkp.pspValidator.shared.externalUtils;

/**
 * Výsledek spuštění nástroje nad souborem pro profil binárního souboru: výstup vybraný parserem (stream a regexp
 * z externalUtils.xml) a posouzení, zda nástroj soubor opravdu zkontroloval.
 * <ul>
 * <li>{@link #getFailure()}: nástroj selhal (pád, nespuštění, neočekávaný návratový kód), výstup se nevyhodnocuje;</li>
 * <li>{@link #getMissingEvidence()}: nástroj skončil běžně, ale ve výstupu chybí znak, že soubor zpracoval; profil
 * se vyhodnotí a hláška se použije, jen když profil sám žádný problém nenašel (u poškozeného souboru nástroj
 * znak zpracování nevypíše, ale vypíše chybu, kterou profil ohlásí).</li>
 * </ul>
 */
public class ExecutionOutcome {

    private final String output;
    private final String failure;
    private final String missingEvidence;

    private ExecutionOutcome(String output, String failure, String missingEvidence) {
        this.output = output;
        this.failure = failure;
        this.missingEvidence = missingEvidence;
    }

    /** Nástroj soubor zpracoval (nebo konfigurace kontrolu nenastavuje), výstup pro profil. */
    public static ExecutionOutcome completed(String output) {
        return new ExecutionOutcome(output, null, null);
    }

    /** Nástroj selhal, failure je text hlášky bez názvu nástroje („skončil s kódem 134 (…), výstup: …“). */
    public static ExecutionOutcome failed(String failure) {
        return new ExecutionOutcome(null, failure, null);
    }

    /** Nástroj skončil běžně, ale chybí znak zpracování souboru; missingEvidence je text hlášky bez názvu nástroje. */
    public static ExecutionOutcome withoutEvidence(String output, String missingEvidence) {
        return new ExecutionOutcome(output, null, missingEvidence);
    }

    /** Výstup pro profil (po výběru proudu a regexp parseru), null když nástroj selhal. */
    public String getOutput() {
        return output;
    }

    public String getFailure() {
        return failure;
    }

    public String getMissingEvidence() {
        return missingEvidence;
    }

    @Override
    public String toString() {
        return "ExecutionOutcome{" +
                "failure=" + failure +
                ", missingEvidence=" + missingEvidence +
                '}';
    }
}
