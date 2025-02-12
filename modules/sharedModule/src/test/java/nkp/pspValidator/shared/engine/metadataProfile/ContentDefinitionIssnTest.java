package nkp.pspValidator.shared.engine.metadataProfile;

import nkp.pspValidator.shared.metadataProfile.CheckingResult;
import nkp.pspValidator.shared.metadataProfile.ContentDefinitionIsbn;
import nkp.pspValidator.shared.metadataProfile.ContentDefinitionIssn;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContentDefinitionIssnTest {

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

    @Test
    public void testEmpty() {
        CheckingResult checkingResult = new ContentDefinitionIssn().checkAgainst("");
        assertFalse(checkingResult.matches());
        assertEquals("neplatné ISSN: prázdná hodnota", checkingResult.getErrorMessage());
    }

    @Test
    public void testNull() {
        CheckingResult checkingResult = new ContentDefinitionIssn().checkAgainst(null);
        assertFalse(checkingResult.matches());
        assertEquals("neplatné ISSN: prázdná hodnota", checkingResult.getErrorMessage());
    }

    private void testForValidSampleIssn(String issn) {
        System.out.println(issn);
        ContentDefinitionIssn contentDefinitionIsbn = new ContentDefinitionIssn();
        CheckingResult checkingResult = contentDefinitionIsbn.checkAgainst(issn);
        if (!checkingResult.matches()) {
            System.out.println(checkingResult.getErrorMessage());
        }
        assertEquals("23077301", contentDefinitionIsbn.normalizeIssn(issn));
        assertTrue(checkingResult.matches());
    }

    @Test
    public void testIssnFormat1() {
        testForValidSampleIssn("ISSN 2307-7301");
    }

    @Test
    public void testIssnFormat2() {
        testForValidSampleIssn("ISSN 2307 7301");
    }

    @Test
    public void testIssnFormat3() {
        testForValidSampleIssn("ISSN 23077301");
    }

    @Test
    public void testIssnFormat4() {
        testForValidSampleIssn("ISSN: 2307-7301");
    }

    @Test
    public void testIssnFormat5() {
        testForValidSampleIssn("ISSN: 2307 7301");
    }

    @Test
    public void testIssnFormat6() {
        testForValidSampleIssn("ISSN: 23077301");
    }

    @Test
    public void testIssnFormat7() {
        testForValidSampleIssn("2307-7301");
    }

    @Test
    public void testIssnFormat8() {
        testForValidSampleIssn("2307 7301");
    }

    @Test
    public void testIssnFormat9() {
        testForValidSampleIssn("23077301");
    }

    private void testForInvalidIssn(String issn) {
        System.out.println(issn);
        ContentDefinitionIssn contentDefinitionIsbn = new ContentDefinitionIssn();
        CheckingResult checkingResult = contentDefinitionIsbn.checkAgainst(issn);
        if (!checkingResult.matches()) {
            System.out.println(checkingResult.getErrorMessage());
        }
        assertFalse(checkingResult.matches());
    }

    @Test
    public void testIssnInvalid0() {
        testForInvalidIssn("23077300");
    }

    @Test
    public void testIssnInvalid2() {
        testForInvalidIssn("23077302");
    }

    @Test
    public void testIssnInvalid3() {
        testForInvalidIssn("23077303");
    }

    @Test
    public void testIssnInvalid4() {
        testForInvalidIssn("23077304");
    }

    @Test
    public void testIssnInvalid5() {
        testForInvalidIssn("23077305");
    }

    @Test
    public void testIssnInvalid6() {
        testForInvalidIssn("23077306");
    }

    @Test
    public void testIssnInvalid7() {
        testForInvalidIssn("23077307");
    }

    @Test
    public void testIssnInvalid8() {
        testForInvalidIssn("23077308");
    }

    @Test
    public void testIssnInvalid9() {
        testForInvalidIssn("23077309");
    }

    @Test
    public void testIssnInvalid10() {
        testForInvalidIssn("2307730X");
    }
}
