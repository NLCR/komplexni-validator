package nkp.pspValidator.shared.metadataProfile;

public class ContentDefinitionIssn implements ContentDefinition {


    /**
     * Povolené zápisy pro ISSN:
     * <p>
     * ISSN 2307-7301
     * ISSN 2307 7301
     * ISSN 23077301
     * <p>
     * ISSN: 2307-7301
     * ISSN: 2307 7301
     * ISSN: 23077301
     * <p>
     * 2307-7301
     * 2307 7301
     * 23077301
     */

    private static final String REGEXP = "(?i)\\b(?:ISSN:? ?)?[0-9]{4}[- ]?[0-9]{3}[0-9X]\\b";

    @Override
    public CheckingResult checkAgainst(String valueFound) {
        if (valueFound == null || valueFound.isEmpty()) { //value empty
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return "neplatné ISSN: prázdná hodnota";
                }
            };
        }
        if (!valueFound.matches(REGEXP)) {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("neplatné ISSN: '%s' neodpovídá regulárním výrazu '%s'", valueFound, REGEXP);
                }
            };
        }
        if (!checksumOk(valueFound)) {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("neplatné ISSN: '%s' nesedí kontrolní součet", valueFound);
                }
            };
        }
        return new CheckingResultMatch();
    }

    private boolean checksumOk(String valueFound) {
        String issn = normalizeIssn(valueFound);
        if (issn == null) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (8 - i) * Character.getNumericValue(issn.charAt(i));
        }
        char lastChar = issn.charAt(7);
        int lastValue = lastChar == 'X' ? 10 : Character.getNumericValue(lastChar);
        return (sum + lastValue) % 11 == 0;
    }

    /**
     * Converts all accepted versions to basic ISSN format, 23077301
     *
     * @param issn input ISSN in any of the accepted formats
     * @return normalized ISSN or null if input is null or empty
     */
    public String normalizeIssn(String issn) {
        if (issn == null || issn.isEmpty()) {
            return null;
        }
        if (issn.startsWith("ISSN:")) {
            issn = issn.substring("ISSN:".length());
        } else if (issn.startsWith("ISSN")) {
            issn = issn.substring("ISSN".length());
        }
        issn = issn.replaceAll("[^0-9X]", "");
        return issn;
    }

    public String toString() {
        return String.format("ISSN");
    }

}
