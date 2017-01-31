package nkp.pspValidator.shared.biblio.biblioValidator;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedContentDefinitionAnything implements ExpectedContentDefinition {

    @Override
    public CheckingResult checkAgainst(String valueFound) {
        if (valueFound == null || valueFound.isEmpty()) { //value empty
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return "prázdná hodnota";
                }
            };
        } else { //value same
            return new CheckingResultMatch();
        }
    }

    public String toString() {
        return "cokoliv";
    }
}
