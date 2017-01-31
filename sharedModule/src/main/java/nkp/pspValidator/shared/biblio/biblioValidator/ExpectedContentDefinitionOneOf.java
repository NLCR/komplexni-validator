package nkp.pspValidator.shared.biblio.biblioValidator;

import java.util.List;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedContentDefinitionOneOf implements ExpectedContentDefinition {

    private final List<ExpectedContentDefinition> candidates;

    public ExpectedContentDefinitionOneOf(List<ExpectedContentDefinition> candidates) {
        this.candidates = candidates;
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
        } else {
            for (ExpectedContentDefinition candidate : candidates) {
                if (candidate.checkAgainst(valueFound).matches()) {
                    return new CheckingResultMatch();
                }
            }
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("hodnota '%s' nesplňuje žádnou ze specifikací %s", valueFound, candidatesToString());
                }
            };
        }
    }

    private String candidatesToString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < candidates.size(); i++) {
            builder.append(candidates.get(i));
            if (i < candidates.size() - 1) {
                builder.append(',');
            }
        }
        builder.append(']');
        return builder.toString();
    }

    public String toString() {
        return candidatesToString();
    }
}
