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

                @Override
                public String getSimpleErrorMessage() {
                    return "prázdná hodnota";
                }

                @Override
                public String getActualValue() {
                    return null;
                }

                @Override
                public String getValueSpecification() {
                    return valueExpected;
                }
            };
        } else if (!valueFound.equals(valueExpected)) { //value different
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("hodnota '%s' neodpovídá očekávané/doporučené hodnotě '%s'", valueFound, valueExpected);
                }

                @Override
                public String getSimpleErrorMessage() {
                    return "hodnota neodpovídá očekávané/doporučené hodnotě";
                }

                @Override
                public String getActualValue() {
                    return valueFound;
                }

                @Override
                public String getValueSpecification() {
                    return valueExpected;
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
