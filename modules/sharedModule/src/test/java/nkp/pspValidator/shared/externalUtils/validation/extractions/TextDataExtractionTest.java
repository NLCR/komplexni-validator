package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.externalUtils.ExtractionResultType;
import nkp.pspValidator.shared.externalUtils.validation.DataExtraction;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.output;
import static org.junit.Assert.*;

/**
 * Textove extrakce (allNonempty, firstNonempty podle regularnich vyrazu) nad ulozenymi vystupy Kakadu a ImageMagick
 * (src/test/resources/externalUtils). Regularni vyrazy jsou prevzate z profilu fDMF (kakadu.xml, imageMagick.xml).
 */
public class TextDataExtractionTest {

    private static final String KAKADU_ERROR = "(?ms)Kakadu Core Error:((?!Kakadu Core Error).)*";
    private static final String IM_ERROR_LINE = "(?m)^Error.*$";
    private static final String IM_IDENTIFY = "(?ms)identify:((?!identify).)*";

    private static AllNonemptyByRegexpDataExtraction all(String... regexps) {
        return new AllNonemptyByRegexpDataExtraction(ExtractionResultType.STRING_LIST, Arrays.asList(regexps));
    }

    private static FirstNonemptyByRegexpDataExtraction first(String... regexps) {
        return new FirstNonemptyByRegexpDataExtraction(ExtractionResultType.STRING, Arrays.asList(regexps));
    }

    // Kakadu

    @Test
    public void kakaduNoErrorOnEmptyStderr() throws Exception {
        assertEquals(Collections.emptyList(), all(KAKADU_ERROR).extract(output("kakadu", "tools_mc.txt")));
        assertNull(first(KAKADU_ERROR).extract(output("kakadu", "tools_mc.txt")));
    }

    @Test
    public void kakaduSingleErrorIsTrimmed() throws Exception {
        List<String> errors = all(KAKADU_ERROR).extract(output("kakadu", "detekce_poskozeni_w0352gUC.txt"));
        assertEquals(Collections.singletonList("Kakadu Core Error:\nExpected to find EPH marker following packet header.  Found 0x0 instead."), errors);
    }

    @Test
    public void kakaduMultilineError() throws Exception {
        List<String> errors = all(KAKADU_ERROR).extract(output("kakadu", "nevalidni_w7200f.txt"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("Kakadu Core Error:\nOut-of-sequence SOP marker"));
        assertTrue(errors.get(0).endsWith("recover from this error."));
    }

    @Test
    public void kakaduTwoErrorsAreSplit() throws Exception {
        String text = output("kakadu", "synteticky_dve_chyby.txt");
        List<String> errors = all(KAKADU_ERROR).extract(text);
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("EPH marker"));
        assertFalse(errors.get(0).contains("SOP"));
        assertTrue(errors.get(1).contains("Out-of-sequence SOP marker"));
        // firstNonempty vrati prvni shodu
        assertEquals(errors.get(0), first(KAKADU_ERROR).extract(text));
    }

    // ImageMagick

    @Test
    public void imageMagickNoDelegate() throws Exception {
        List<String> errors = all(IM_ERROR_LINE, IM_IDENTIFY).extract(output("imageMagick", "no_delegate_tools_mc.txt"));
        assertEquals(Collections.singletonList("identify: no decode delegate for this image format `src/test/tools/jp2/mc.jp2' @ error/constitute.c/ReadImage/753."), errors);
    }

    @Test
    public void imageMagickEmptyStderr() throws Exception {
        assertEquals(Collections.emptyList(), all(IM_ERROR_LINE, IM_IDENTIFY).extract(output("imageMagick", "synteticky_ok.txt")));
    }

    @Test
    public void imageMagickWarningAndErrorAreSeparateItems() throws Exception {
        // syntetický vystup: varovani i chyba zacinaji "identify:", kazde je samostatna polozka
        List<String> errors = all(IM_ERROR_LINE, IM_IDENTIFY).extract(output("imageMagick", "synteticky_poskozeny.txt"));
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).startsWith("identify: Malformed tile header"));
        assertTrue(errors.get(1).startsWith("identify: Stream too short"));
    }

    @Test
    public void imageMagickErrorLine() throws Exception {
        List<String> errors = all(IM_ERROR_LINE, IM_IDENTIFY).extract(output("imageMagick", "synteticky_error_radek.txt"));
        assertEquals(Collections.singletonList("Error: Codestream too short"), errors);
    }

    @Test
    public void imageMagickIdentifyRegexStopsAtFileNameContainingIdentify() throws Exception {
        // známá vada: negativni lookahead (?!identify) ukonci shodu u kazdeho vyskytu slova "identify",
        // i uvnitr cesty k souboru; hlaseni se pak usekne
        String text = "identify: Stream too short `/data/identify/a.jp2' @ error/jp2.c/JP2ErrorHandler/211.\n";
        List<String> errors = all(IM_IDENTIFY).extract(text);
        assertEquals(Collections.singletonList("identify: Stream too short `/data/"), errors);
    }

    // obecne chovani

    @Test
    public void matchesOfAllRegexpsAreConcatenatedInRegexpOrder() throws Exception {
        String text = "b1 a1 b2 a2";
        assertEquals(Arrays.asList("a1", "a2", "b1", "b2"), all("a[0-9]", "b[0-9]").extract(text));
        // firstNonempty: prvni shoda prvniho regularniho vyrazu, ktery neco nasel
        assertEquals("a1", first("x[0-9]", "a[0-9]", "b[0-9]").extract(text));
    }

    @Test
    public void whitespaceOnlyMatchesAreDropped() throws Exception {
        assertEquals(Collections.singletonList("x"), all("\\s+|x").extract("a x b"));
        assertNull(first("\\s+").extract("a b"));
    }

    @Test
    public void inputIsTrimmedBeforeMatching() throws Exception {
        // vstup se pred hledanim orizne, (?m)^ tak sedi i na prvni radek s uvodnimi mezerami
        assertEquals(Collections.singletonList("Error x"), all(IM_ERROR_LINE).extract("\n\n  Error x\n"));
        assertEquals(Collections.emptyList(), all(IM_ERROR_LINE).extract("   \n "));
    }

    @Test
    public void processedOutputIsConvertedByToString() throws Exception {
        StringBuilder sb = new StringBuilder("Kakadu Core Error:\nX");
        assertEquals(Collections.singletonList("Kakadu Core Error:\nX"), all(KAKADU_ERROR).extract(sb));
    }

    @Test
    public void nullOutputThrowsExtractionException() {
        try {
            all(KAKADU_ERROR).extract(null);
            fail();
        } catch (DataExtraction.ExtractionException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
        try {
            first(KAKADU_ERROR).extract(null);
            fail();
        } catch (DataExtraction.ExtractionException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void invalidRegexpThrowsExtractionException() {
        try {
            all("(").extract("x");
            fail();
        } catch (DataExtraction.ExtractionException e) {
            assertTrue(e.getCause() instanceof java.util.regex.PatternSyntaxException);
        }
        try {
            first("(").extract("x");
            fail();
        } catch (DataExtraction.ExtractionException e) {
            assertTrue(e.getCause() instanceof java.util.regex.PatternSyntaxException);
        }
    }

    @Test
    public void resultTypeIsIgnored() throws Exception {
        // resultType z profilu se u textovych extrakci uklada, ale nepouziva: vysledek je vzdy String / List<String>
        Object value = new FirstNonemptyByRegexpDataExtraction(ExtractionResultType.INTEGER, Collections.singletonList("[0-9]+")).extract("v 42");
        assertEquals("42", value);
        assertEquals(ExtractionResultType.INTEGER, new FirstNonemptyByRegexpDataExtraction(ExtractionResultType.INTEGER, Collections.<String>emptyList()).extractionResultType);
        assertEquals(ExtractionResultType.FLOAT_LIST, new AllNonemptyByRegexpDataExtraction(ExtractionResultType.FLOAT_LIST, Collections.<String>emptyList()).extractionResultType);
    }
}
