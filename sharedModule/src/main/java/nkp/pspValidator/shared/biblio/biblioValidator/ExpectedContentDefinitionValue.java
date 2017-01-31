package nkp.pspValidator.shared.biblio.biblioValidator;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedContentDefinitionValue implements ExpectedContentDefinition {
    private final String valueExpected;

    public ExpectedContentDefinitionValue(String valueExpected) {
        this.valueExpected = valueExpected;
    }

    @Override
    public CheckingResult checkAgainst(String valueFound) {
        if (valueFound == null || valueFound.isEmpty()) { //value empty
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return "prázdná hodnota";
                }
            };
        } else if (!valueFound.equals(valueExpected)) { //value different
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("hodnota '%s' neodpovídá očekávané hodnotě '%s'", valueFound, valueExpected);
                }
            };
        } else { //value same
            return new CheckingResultMatch();
        }
    }

    public String toString() {
        return String.format("\"%s\"", valueExpected);
    }
}
