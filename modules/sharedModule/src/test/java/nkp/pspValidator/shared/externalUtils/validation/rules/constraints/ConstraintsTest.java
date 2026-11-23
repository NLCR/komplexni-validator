package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Omezeni (constraints) z profilu binarnich souboru: kazde s hodnotou, ktera vyhovi, a s hodnotou, ktera nevyhovi.
 * Hodnoty odpovidaji tomu, co extrakce vraci z vystupu nastroju (String, Integer, Float, List&lt;String&gt;).
 */
public class ConstraintsTest {

    // isExactly

    @Test
    public void isExactlyMatchesSameString() {
        IsExactlyConstraint c = new IsExactlyConstraint("RPCL");
        assertTrue(c.matches("RPCL"));
        assertFalse(c.matches("LRCP"));
        assertFalse(c.matches("rpcl"));
        assertFalse(c.matches("RPCL "));
    }

    @Test
    public void isExactlyComparesToStringOfNonStringData() {
        // profily pouzivaji isExactly i pro extrakci typu INTEGER (Velikost dlaždic, 4096)
        IsExactlyConstraint c = new IsExactlyConstraint("4096");
        assertTrue(c.matches(4096));
        assertFalse(c.matches(1024));
        // Float se prevede na "4096.0"
        assertFalse(c.matches(4096.0f));
    }

    @Test
    public void isExactlyNullDoesNotMatch() {
        // null = hodnota nenalezena (napr. chybejici repInfo/version u necitelneho EPUB); driv NullPointerException
        // (nalez 28), dnes nevyhovi a mustMatchAny nahlasi 'hodnota "null" neodpovídá ...'
        assertFalse(new IsExactlyConstraint("x").matches(null));
        assertFalse(new IsExactlyConstraint("null").matches(null));
    }

    @Test
    public void isExactlyToString() {
        assertEquals("musí být přesně 'True'", new IsExactlyConstraint("True").toString());
    }

    // endsWith

    @Test
    public void endsWithMatchesSuffix() {
        EndsWithConstraint c = new EndsWithConstraint("irreversible");
        assertTrue(c.matches("9-7 irreversible"));
        assertTrue(c.matches("irreversible"));
        assertFalse(c.matches("5-3 reversible"));
        assertFalse(c.matches("irreversible "));
    }

    @Test
    public void endsWithUsesToStringOfData() {
        EndsWithConstraint c = new EndsWithConstraint("96");
        assertTrue(c.matches(4096));
        assertFalse(c.matches(1024));
    }

    @Test
    public void endsWithEmptySuffixMatchesAnything() {
        assertTrue(new EndsWithConstraint("").matches(""));
        assertTrue(new EndsWithConstraint("").matches("cokoli"));
    }

    @Test
    public void endsWithNullDoesNotMatch() {
        // stejne jako isExactly (nalez 28)
        assertFalse(new EndsWithConstraint("x").matches(null));
        assertFalse(new EndsWithConstraint("").matches(null));
    }

    @Test
    public void endsWithToString() {
        assertEquals("musí končit na '.jp2'", new EndsWithConstraint(".jp2").toString());
    }

    // startsWith

    @Test
    public void startsWithMatchesPrefixCaseSensitive() {
        // profil veraPDF z VEP: startsWith "PDF/A-1B", veraPDF 1.30 ale vypisuje "PDF/A-1b validation profile" (riziko R1)
        StartsWithConstraint c = new StartsWithConstraint("PDF/A-1B");
        assertTrue(c.matches("PDF/A-1B validation profile"));
        assertTrue(c.matches("PDF/A-1B"));
        assertFalse(c.matches("PDF/A-1b validation profile"));
        assertFalse(c.matches(" PDF/A-1B"));
        assertFalse(c.matches(null));
    }

    @Test
    public void startsWithUsesToStringOfData() {
        assertTrue(new StartsWithConstraint("40").matches(4096));
        assertTrue(new StartsWithConstraint("").matches("cokoli"));
    }

    @Test
    public void startsWithToString() {
        assertEquals("musí začínat na 'PDF/A-2U'", new StartsWithConstraint("PDF/A-2U").toString());
    }

    @Test
    public void onlyNTimesXRemainingYAppliesToWholeList() {
        assertTrue(new NTimesXRemainingYConstraint(1, "a", "b").appliesToWholeList());
        assertFalse(new IsExactlyConstraint("a").appliesToWholeList());
        assertFalse(new EndsWithConstraint("a").appliesToWholeList());
        assertFalse(new StartsWithConstraint("a").appliesToWholeList());
        assertFalse(new IsInIntRangeConstraint(1, 2).appliesToWholeList());
        assertFalse(new IsInFloatRangeConstraint(1f, 2f).appliesToWholeList());
    }

    // isInIntRange

    @Test
    public void isInIntRangeMinOnly() {
        IsInIntRangeConstraint c = new IsInIntRangeConstraint(1, null);
        assertTrue(c.matches(1));
        assertTrue(c.matches(Integer.MAX_VALUE));
        assertFalse(c.matches(0));
        assertFalse(c.matches(-5));
        assertFalse(c.matches(null));
    }

    @Test
    public void isInIntRangeMaxOnly() {
        IsInIntRangeConstraint c = new IsInIntRangeConstraint(null, 10);
        assertTrue(c.matches(10));
        assertTrue(c.matches(Integer.MIN_VALUE));
        assertFalse(c.matches(11));
    }

    @Test
    public void isInIntRangeBothBoundsInclusive() {
        IsInIntRangeConstraint c = new IsInIntRangeConstraint(5, 6);
        assertTrue(c.matches(5));
        assertTrue(c.matches(6));
        assertFalse(c.matches(4));
        assertFalse(c.matches(7));
    }

    @Test
    public void isInIntRangeNoBoundsMatchesAnyInteger() {
        IsInIntRangeConstraint c = new IsInIntRangeConstraint(null, null);
        assertTrue(c.matches(0));
        assertFalse(c.matches(null));
    }

    @Test(expected = ClassCastException.class)
    public void isInIntRangeWithStringThrowsClassCast() {
        // známá vada: omezeni predpoklada Integer (extrakce resultType INTEGER); pri chybne konfiguraci
        // (resultType STRING) skonci ClassCastException misto chyby validace
        new IsInIntRangeConstraint(1, null).matches("5");
    }

    @Test
    public void isInIntRangeToString() {
        assertEquals("musí být v intervalu <1, 2>", new IsInIntRangeConstraint(1, 2).toString());
        assertEquals("musí být alespoň 1", new IsInIntRangeConstraint(1, null).toString());
        assertEquals("musí být nejvýše 2", new IsInIntRangeConstraint(null, 2).toString());
        assertEquals("musí být číslo (integer)", new IsInIntRangeConstraint(null, null).toString());
    }

    // isInFloatRange

    @Test
    public void isInFloatRangeMinOnly() {
        // Rozlišení: min 11732.0 (300 DPI v pixelech na metr)
        IsInFloatRangeConstraint c = new IsInFloatRangeConstraint(11732.0f, null);
        assertTrue(c.matches(11811.0f));
        assertTrue(c.matches(11732.0f));
        assertFalse(c.matches(2834.65f));
        assertFalse(c.matches(null));
    }

    @Test
    public void isInFloatRangeMaxOnlyAndBoth() {
        assertTrue(new IsInFloatRangeConstraint(null, 35.0f).matches(35.0f));
        assertFalse(new IsInFloatRangeConstraint(null, 35.0f).matches(35.01f));
        IsInFloatRangeConstraint both = new IsInFloatRangeConstraint(6.0f, 35.0f);
        assertTrue(both.matches(6.0f));
        assertTrue(both.matches(20.5f));
        assertFalse(both.matches(2.67f));
        assertFalse(both.matches(40.0f));
        assertTrue(new IsInFloatRangeConstraint(null, null).matches(-1.0f));
    }

    @Test(expected = ClassCastException.class)
    public void isInFloatRangeWithIntegerThrowsClassCast() {
        // známá vada: stejne jako isInIntRange, typ hodnoty se nekontroluje
        new IsInFloatRangeConstraint(1.0f, null).matches(5);
    }

    @Test
    public void isInFloatRangeToStringIndependentOfLocale() {
        // nalez N4: drive String.format bez Locale (cesky "6,000000", anglicky "6.000000"); ted vzdy desetinna carka
        // jako dosud v ceskem prostredi, bez oddelovace tisicu, v jakemkoli vychozim locale
        Locale original = Locale.getDefault();
        try {
            for (Locale locale : Arrays.asList(Locale.US, new Locale("cs", "CZ"), Locale.ROOT, Locale.GERMANY, Locale.FRANCE)) {
                Locale.setDefault(locale);
                String context = locale.toString();
                assertEquals(context, "musí být alespoň 6,000000", new IsInFloatRangeConstraint(6.0f, null).toString());
                assertEquals(context, "musí být nejvýše 35,000000", new IsInFloatRangeConstraint(null, 35.0f).toString());
                assertEquals(context, "musí být v intervalu <6,000000, 35,000000>", new IsInFloatRangeConstraint(6.0f, 35.0f).toString());
                assertEquals(context, "musí být alespoň 11732,000000", new IsInFloatRangeConstraint(11732.0f, null).toString());
                assertEquals(context, "musí být alespoň 1234567,000000", new IsInFloatRangeConstraint(1234567.0f, null).toString());
                assertEquals(context, "musí být v intervalu <-1,500000, 0,250000>", new IsInFloatRangeConstraint(-1.5f, 0.25f).toString());
                assertEquals(context, "musí být číslo (float)", new IsInFloatRangeConstraint(null, null).toString());
            }
        } finally {
            Locale.setDefault(original);
        }
    }

    // nTimesXRemainingY

    @Test
    public void nTimesXRemainingYMatchesProfileExample() {
        // Velikost precincts: 2krat 256, zbytek 128 (vystup Jpylyzeru precinctSizeX/Y)
        NTimesXRemainingYConstraint c = new NTimesXRemainingYConstraint(2, "256", "128");
        assertTrue(c.matches(Arrays.asList("256", "256", "128", "128", "128", "128")));
        assertTrue(c.matches(Arrays.asList("128", "256", "128", "256")));
    }

    @Test
    public void nTimesXRemainingYFailsOnTooManyX() {
        NTimesXRemainingYConstraint c = new NTimesXRemainingYConstraint(2, "256", "128");
        assertFalse(c.matches(Arrays.asList("256", "256", "256", "128")));
        // realny vystup Jpylyzeru pro odlis_od_prof_mc/precint2.jp2
        assertFalse(c.matches(Arrays.asList("256", "256", "256", "256", "256", "256")));
    }

    @Test
    public void nTimesXRemainingYFailsOnUnknownValue() {
        NTimesXRemainingYConstraint c = new NTimesXRemainingYConstraint(2, "256", "128");
        // realny vystup Jpylyzeru pro src/test/tools/jp2/uc.jp2 (vychozi precincts)
        assertFalse(c.matches(Arrays.asList("32768", "32768", "32768")));
        assertFalse(c.matches(null));
    }

    @Test
    public void nTimesXRemainingYAllowsFewerThanNTimesX() {
        // známá vada (nebo nejasnost zadani): "n" je horni mez, ne presny pocet. Profil rika "2krát hodnota 256",
        // ale projde i seznam bez jedine 256, i prazdny seznam.
        NTimesXRemainingYConstraint c = new NTimesXRemainingYConstraint(2, "256", "128");
        assertTrue(c.matches(Arrays.asList("128", "128", "128")));
        assertTrue(c.matches(Arrays.asList("256", "128")));
        assertTrue(c.matches(Collections.emptyList()));
    }

    @Test
    public void nTimesXRemainingYHasNoReadableToString() {
        // známá vada: chybi toString(), v hlaseni MustMatchAnyDR se proto objevi nazev tridy a hash
        // ("neodpovídá žádnému z omezení: [nkp...NTimesXRemainingYConstraint@1b2c3d]"), viz BinaryFileProfileTest
        String s = new NTimesXRemainingYConstraint(2, "256", "128").toString();
        assertTrue(s, s.startsWith(NTimesXRemainingYConstraint.class.getName() + "@"));
    }
}
