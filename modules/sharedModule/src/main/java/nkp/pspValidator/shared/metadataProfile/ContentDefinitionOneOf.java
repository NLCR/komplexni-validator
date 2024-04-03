package nkp.pspValidator.shared.metadataProfile;

import java.util.List;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ContentDefinitionOneOf implements ContentDefinition {

    private final List<ContentDefinition> candidates;

    public ContentDefinitionOneOf(List<ContentDefinition> candidates) {
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
            for (ContentDefinition candidate : candidates) {
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
