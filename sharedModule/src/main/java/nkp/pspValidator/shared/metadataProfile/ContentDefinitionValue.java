package nkp.pspValidator.shared.metadataProfile;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ContentDefinitionValue implements ContentDefinition {
    private final String valueExpected;

    public ContentDefinitionValue(String valueExpected) {
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
                    return String.format("hodnota '%s' neodpovídá očekávané/doporučené hodnotě '%s'", valueFound, valueExpected);
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
