package nkp.pspValidator.shared.metadataProfile;

import java.util.regex.Pattern;

public class ContentDefinitionIsbn implements ContentDefinition {

    /**
     * Povolené zápisy pro ISBN-13:
     * <p>
     * ISBN 978-83-226-2105-9
     * ISBN 978 83 226 2105 9
     * ISBN 9788322621059
     * ISBN-13: 978-83-226-2105-9
     * ISBN-13: 978 83 226 2105 9
     * ISBN-13: 9788322621059
     * 978-83-226-2105-9
     * 978 83 226 2105 9
     * 9788322621059
     * <p>
     * Povolené zápisy pro ISBN-10:
     * <p>
     * ISBN 83-226-2105-1
     * ISBN 83 226 2105 1
     * ISBN 8322621051
     * ISBN-10: 83-226-2105-1
     * ISBN-10: 83 226 2105 1
     * ISBN-10: 8322621051
     * 83-226-2105-1
     * 83 226 2105 1
     * 8322621051
     */

    private static final String REGEXP = "(?i)\\b(?:ISBN(?:-1[03])?:? )?(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]{1,7}[- ]?[0-9]{1,7}[- ]?[0-9X]\\b";

    public ContentDefinitionIsbn() {
    }

    @Override
    public CheckingResult checkAgainst(String valueFound) {
        if (valueFound == null || valueFound.isEmpty()) { //value empty
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return "neplatné ISBN: prázdná hodnota";
                }
            };
        }
        if (!valueFound.matches(REGEXP)) {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("neplatné ISBN: '%s' neodpovídá regulárním výrazu '%s'", valueFound, REGEXP);
                }
            };
        }
        if (!checksumOk(valueFound)) {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("neplatné ISBN: '%s' nesedí kontrolní součet", valueFound);
                }
            };
        }
        return new CheckingResultMatch();
    }

    private boolean checksumOk(String valueFound) {
        String isbn = normalizeIsbn(valueFound);
        if (isbn == null) {
            return false;
        }
        if (isbn.length() == 10) {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += (10 - i) * Character.getNumericValue(isbn.charAt(i));
            }
            char lastChar = isbn.charAt(9);
            int lastValue = lastChar == 'X' ? 10 : Character.getNumericValue(lastChar);
            return (sum + lastValue) % 11 == 0;
        } else if (isbn.length() == 13) {
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += (i % 2 == 0 ? 1 : 3) * Character.getNumericValue(isbn.charAt(i));
            }
            return (10 - sum % 10) % 10 == Character.getNumericValue(isbn.charAt(12));
        } else { //should not happen because of the regexp filter
            return false;
        }
    }


    /**
     * Converts all accepted versions to basic ISBN format
     * i.e. 9788322621059 for ISBN-13 or 8322621059 for ISBN-10
     *
     * @param isbn input ISBN in any of the accepted formats
     * @return normalized ISBN or null if input is null or empty
     */
    public String normalizeIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            return null;
        }
        if (isbn.startsWith("ISBN-13:")) {
            isbn = isbn.substring("ISBN-13:".length());
        } else if (isbn.startsWith("ISBN-10:")) {
            isbn = isbn.substring("ISBN-10:".length());
        } else if (isbn.startsWith("ISBN")) {
            isbn = isbn.substring("ISBN".length());
        }
        isbn = isbn.replaceAll("[^0-9X]", "");
        return isbn;
    }

    public String toString() {
        return String.format("ISBN");
    }
}
