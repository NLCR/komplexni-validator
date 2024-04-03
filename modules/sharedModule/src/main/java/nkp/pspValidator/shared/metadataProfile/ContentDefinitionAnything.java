package nkp.pspValidator.shared.metadataProfile;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ContentDefinitionAnything implements ContentDefinition {

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
