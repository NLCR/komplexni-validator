package nkp.pspValidator.shared.biblio.biblioValidator;

import java.util.Set;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedContentDefinitionFromDictionary implements ExpectedContentDefinition {
    private final String dictionaryName;
    private final Set<String> dictionaryValues;

    public ExpectedContentDefinitionFromDictionary(String dictionaryName, Set<String> dictionaryValues) {
        this.dictionaryName = dictionaryName;
        this.dictionaryValues = dictionaryValues;
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
        } else if (dictionaryValues.contains(valueFound)) {
            return new CheckingResultMatch();
        } else { //value same
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("hodnota '%s' nenalezena v kontrolovaném slovníku '%s'", valueFound, dictionaryName);
                }
            };
        }
    }

    public String toString() {
        return String.format("slovník '%s'", dictionaryName);
    }

}
