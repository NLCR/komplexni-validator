package nkp.pspValidator.shared.engine.metadataProfile;

import nkp.pspValidator.shared.metadataProfile.CheckingResult;
import nkp.pspValidator.shared.metadataProfile.ContentDefinitionIsbn;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContentDefinitionIsbnTest {

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

    @Test
    public void testEmpty() {
        CheckingResult checkingResult = new ContentDefinitionIsbn().checkAgainst("");
        assertFalse(checkingResult.matches());
        assertEquals("prázdná hodnota", checkingResult.getErrorMessage());
    }

    @Test
    public void testNull() {
        CheckingResult checkingResult = new ContentDefinitionIsbn().checkAgainst(null);
        assertFalse(checkingResult.matches());
        assertEquals("prázdná hodnota", checkingResult.getErrorMessage());
    }

    private void testForValidSampleIsbn13(String isbn) {
        System.out.println(isbn);
        ContentDefinitionIsbn contentDefinitionIsbn = new ContentDefinitionIsbn();
        CheckingResult checkingResult = contentDefinitionIsbn.checkAgainst(isbn);
        if (!checkingResult.matches()) {
            System.out.println(checkingResult.getErrorMessage());
        }
        assertEquals("9788322621059", contentDefinitionIsbn.normalizeIsbn(isbn));
        assertTrue(checkingResult.matches());
    }

    private void testForValidSampleIsbn10(String isbn) {
        System.out.println(isbn);
        ContentDefinitionIsbn contentDefinitionIsbn = new ContentDefinitionIsbn();
        CheckingResult checkingResult = contentDefinitionIsbn.checkAgainst(isbn);
        if (!checkingResult.matches()) {
            System.out.println(checkingResult.getErrorMessage());
        }
        assertEquals("8322621051", contentDefinitionIsbn.normalizeIsbn(isbn));
        assertTrue(checkingResult.matches());
    }

    @Test
    public void testIsbn13Format1() {
        testForValidSampleIsbn13("ISBN 978-83-226-2105-9");
    }

    @Test
    public void testIsbn13Format2() {
        testForValidSampleIsbn13("ISBN 978 83 226 2105 9");
    }

    @Test
    public void testIsbn13Format3() {
        testForValidSampleIsbn13("ISBN 9788322621059");
    }

    @Test
    public void testIsbn13Format4() {
        testForValidSampleIsbn13("ISBN-13: 978-83-226-2105-9");
    }

    @Test
    public void testIsbn13Format5() {
        testForValidSampleIsbn13("ISBN-13: 978 83 226 2105 9");
    }

    @Test
    public void testIsbn13Format6() {
        testForValidSampleIsbn13("ISBN-13: 9788322621059");
    }

    @Test
    public void testIsbn13Format7() {
        testForValidSampleIsbn13("978-83-226-2105-9");
    }

    @Test
    public void testIsbn13Format8() {
        testForValidSampleIsbn13("978 83 226 2105 9");
    }

    @Test
    public void testIsbn13Format9() {
        testForValidSampleIsbn13("9788322621059");
    }

    @Test
    public void testIsbn10Format1() {
        testForValidSampleIsbn10("ISBN 83-226-2105-1");
    }

    @Test
    public void testIsbn10Format2() {
        testForValidSampleIsbn10("ISBN 83 226 2105 1");
    }

    @Test
    public void testIsbn10Format3() {
        testForValidSampleIsbn10("ISBN 8322621051");
    }

    @Test
    public void testIsbn10Format4() {
        testForValidSampleIsbn10("ISBN-10: 83-226-2105-1");
    }

    @Test
    public void testIsbn10Format5() {
        testForValidSampleIsbn10("ISBN-10: 83 226 2105 1");
    }

    @Test
    public void testIsbn10Format6() {
        testForValidSampleIsbn10("ISBN-10: 8322621051");
    }

    @Test
    public void testIsbn10Format7() {
        testForValidSampleIsbn10("83-226-2105-1");
    }

    @Test
    public void testIsbn10Format8() {
        testForValidSampleIsbn10("83 226 2105 1");
    }

    @Test
    public void testIsbn10Format9() {
        testForValidSampleIsbn10("8322621051");
    }

    private void testForInvalidIsbn(String isbn) {
        System.out.println(isbn);
        ContentDefinitionIsbn contentDefinitionIsbn = new ContentDefinitionIsbn();
        CheckingResult checkingResult = contentDefinitionIsbn.checkAgainst(isbn);
        if (!checkingResult.matches()) {
            System.out.println(checkingResult.getErrorMessage());
        }
        assertFalse(checkingResult.matches());
    }

    @Test
    public void testIsbn10Invalid0() {
        testForInvalidIsbn("8322621050");
    }

    @Test
    public void testIsbn10Invalid2() {
        testForInvalidIsbn("8322621052");
    }

    @Test
    public void testIsbn10Invalid3() {
        testForInvalidIsbn("8322621053");
    }

    @Test
    public void testIsbn10Invalid4() {
        testForInvalidIsbn("8322621054");
    }

    @Test
    public void testIsbn10Invalid5() {
        testForInvalidIsbn("8322621055");
    }

    @Test
    public void testIsbn10Invalid6() {
        testForInvalidIsbn("8322621056");
    }

    @Test
    public void testIsbn10Invalid7() {
        testForInvalidIsbn("8322621057");
    }

    @Test
    public void testIsbn10Invalid8() {
        testForInvalidIsbn("8322621058");
    }

    @Test
    public void testIsbn10Invalid9() {
        testForInvalidIsbn("8322621059");
    }

    @Test
    public void testIsbn10Invalid10() {
        testForInvalidIsbn("832262105X");
    }


    @Test
    public void testIsbn13Invalid0() {
        testForInvalidIsbn("9788322621050");
    }

    @Test
    public void testIsbn13Invalid1() {
        testForInvalidIsbn("9788322621051");
    }

    @Test
    public void testIsbn13Invalid2() {
        testForInvalidIsbn("9788322621052");
    }

    @Test
    public void testIsbn13Invalid3() {
        testForInvalidIsbn("9788322621053");
    }

    @Test
    public void testIsbn13Invalid4() {
        testForInvalidIsbn("9788322621054");
    }

    @Test
    public void testIsbn13Invalid5() {
        testForInvalidIsbn("9788322621055");
    }

    @Test
    public void testIsbn13Invalid6() {
        testForInvalidIsbn("9788322621056");
    }

    @Test
    public void testIsbn13Invalid7() {
        testForInvalidIsbn("9788322621057");
    }

    @Test
    public void testIsbn13Invalid8() {
        testForInvalidIsbn("9788322621058");
    }

    @Test
    public void testIsbn13Invalid10() {
        testForInvalidIsbn("978832262105X");
    }


}
