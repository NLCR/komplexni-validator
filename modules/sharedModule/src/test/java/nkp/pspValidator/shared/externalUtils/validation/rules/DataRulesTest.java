package nkp.pspValidator.shared.externalUtils.validation.rules;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.externalUtils.validation.Constraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.IsExactlyConstraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.IsInIntRangeConstraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.NTimesXRemainingYConstraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.StartsWithConstraint;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Pravidla profilu binarnich souboru (mustExist, mustNotExist, mustMatchAny) nad hodnotami, jake vraci extrakce.
 */
public class DataRulesTest {

    // mustExist

    @Test
    public void mustExistAcceptsValues() {
        MustExistDR rule = new MustExistDR("Tool version");
        assertEquals("Tool version", rule.getValidationName());
        assertTrue(rule.validate("2.2.0").isEmpty());
        assertTrue(rule.validate(0).isEmpty());
        assertTrue(rule.validate(2.52f).isEmpty());
        assertTrue(rule.validate(Collections.singletonList("256")).isEmpty());
        assertTrue(rule.validate(Collections.singleton("x")).isEmpty());
    }

    @Test
    public void mustExistRejectsMissingValues() {
        MustExistDR rule = new MustExistDR("Height");
        List<String> expected = Collections.singletonList("Height: kontrolovaná hodnota nenalezena");
        assertEquals(expected, rule.validate(null));
        assertEquals(expected, rule.validate(""));
        assertEquals(expected, rule.validate(new ArrayList<>()));
        assertEquals(expected, rule.validate(new HashSet<>()));
    }

    @Test
    public void mustExistAcceptsWhitespaceOnly() {
        // extrakce XPath typu STRING vraci text uzlu bez orezani, napr. "\n   " z prazdneho elementu s odsazenim;
        // mustExist ho bere jako existujici hodnotu
        assertTrue(new MustExistDR("x").validate("  ").isEmpty());
    }

    // mustNotExist

    @Test
    public void mustNotExistAcceptsMissingValues() {
        MustNotExistDR rule = new MustNotExistDR("no_errors");
        assertTrue(rule.validate(null).isEmpty());
        assertTrue(rule.validate("").isEmpty());
    }

    @Test
    public void mustNotExistReportsSingleValue() {
        assertEquals(Collections.singletonList("regions_of_interest: zakázaná hodnota nalezena: yes"),
                new MustNotExistDR("regions_of_interest").validate("yes"));
    }

    @Test
    public void mustNotExistReportsEachListItem() {
        List<String> errors = new MustNotExistDR("no_errors").validate(Arrays.asList("Kakadu Core Error:\nA", "Kakadu Core Error:\nB"));
        assertEquals(Arrays.asList(
                "no_errors: zakázaná hodnota nalezena: Kakadu Core Error:\nA",
                "no_errors: zakázaná hodnota nalezena: Kakadu Core Error:\nB"), errors);
    }

    @Test
    public void mustNotExistEmptyListIsOk() {
        // prazdny seznam ma toString "[]", tedy neprazdny; vetev pro List ho pak projde bez chyby
        assertTrue(new MustNotExistDR("no_errors").validate(new ArrayList<String>()).isEmpty());
    }

    @Test
    public void mustNotExistReportsSetAsSingleValue() {
        // Set neni List, nahlasi se jako celek
        assertEquals(Collections.singletonList("x: zakázaná hodnota nalezena: [a]"),
                new MustNotExistDR("x").validate(Collections.singleton("a")));
    }

    // mustMatchAny

    @Test
    public void mustMatchAnyAcceptsAnyMatchingConstraint() {
        MustMatchAnyDR rule = new MustMatchAnyDR("Levels", Arrays.<Constraint>asList(new IsExactlyConstraint("5"), new IsExactlyConstraint("6")));
        assertTrue(rule.validate("5").isEmpty());
        assertTrue(rule.validate("6").isEmpty());
    }

    @Test
    public void mustMatchAnyReportsValueAndConstraints() {
        MustMatchAnyDR rule = new MustMatchAnyDR("Levels", Arrays.<Constraint>asList(new IsExactlyConstraint("5"), new IsExactlyConstraint("6")));
        assertEquals(Collections.singletonList("Levels: hodnota \"7\" neodpovídá žádnému z omezení: [musí být přesně '5',musí být přesně '6']"),
                rule.validate("7"));
    }

    @Test
    public void mustMatchAnyFormatsListValue() {
        MustMatchAnyDR rule = new MustMatchAnyDR("Velikost precincts (1)", Collections.<Constraint>singletonList(new NTimesXRemainingYConstraint(2, "256", "128")));
        List<String> errors = rule.validate(Arrays.asList("32768", "32768"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).startsWith("Velikost precincts (1): hodnota \"[32768,32768]\" neodpovídá žádnému z omezení: ["));
    }

    @Test
    public void mustMatchAnyNullValue() {
        // null projde do omezeni; isInIntRange vrati false, hlaseni obsahuje "null"
        MustMatchAnyDR rule = new MustMatchAnyDR("Počet dlaždic", Collections.<Constraint>singletonList(new IsInIntRangeConstraint(1, null)));
        assertEquals(Collections.singletonList("Počet dlaždic: hodnota \"null\" neodpovídá žádnému z omezení: [musí být alespoň 1]"),
                rule.validate(null));
    }

    @Test
    public void mustMatchAnyWithoutConstraintsAlwaysFails() {
        MustMatchAnyDR rule = new MustMatchAnyDR("x", Collections.<Constraint>emptyList());
        assertEquals(Collections.singletonList("x: hodnota \"a\" neodpovídá žádnému z omezení: []"), rule.validate("a"));
    }

    @Test
    public void mustMatchAnyValidatesEachListItem() {
        // seznam z extrakce allNonempty (napr. typy medii EPUB): kazda nevyhovujici polozka je samostatna chyba
        MustMatchAnyDR rule = new MustMatchAnyDR("Typy", Arrays.<Constraint>asList(new IsExactlyConstraint("text/css"), new IsExactlyConstraint("image/png")));
        assertTrue(rule.validate(Arrays.asList("text/css", "image/png", "text/css")).isEmpty());
        assertTrue(rule.validate(new ArrayList<String>()).isEmpty());
        assertEquals(Arrays.asList(
                "Typy: hodnota \"font/woff\" neodpovídá žádnému z omezení: [musí být přesně 'text/css',musí být přesně 'image/png']",
                "Typy: hodnota \"audio/mpeg\" neodpovídá žádnému z omezení: [musí být přesně 'text/css',musí být přesně 'image/png']"
        ), rule.validate(Arrays.asList("text/css", "font/woff", "audio/mpeg")));
    }

    @Test
    public void mustMatchAnyPassesWholeListToWholeListConstraint() {
        // nTimesXRemainingY posuzuje seznam jako celek (profily Jpylyzeru), polozky se nerozdeluji
        MustMatchAnyDR rule = new MustMatchAnyDR("Velikost precincts (1)", Collections.<Constraint>singletonList(new NTimesXRemainingYConstraint(2, "256", "128")));
        assertTrue(rule.validate(Arrays.asList("256", "256", "128", "128")).isEmpty());
        assertEquals(1, rule.validate(Arrays.asList("256", "256", "256")).size());
    }

    @Test
    public void rulesWithoutLevelUseLevelFromRules() {
        // bez atributu level v profilu plati validation_problem_level z rules.xml (null)
        assertNull(new MustExistDR("x").getLevel());
        assertNull(new MustNotExistDR("x").getLevel());
        assertNull(new MustMatchAnyDR("x", Collections.<Constraint>emptyList()).getLevel());
        assertNull(new MustExistDR("x", null).getLevel());
    }

    @Test
    public void rulesWithLevel() {
        assertEquals(Level.WARNING, new MustExistDR("x", Level.WARNING).getLevel());
        assertEquals(Level.ERROR, new MustNotExistDR("x", Level.ERROR).getLevel());
        MustMatchAnyDR rule = new MustMatchAnyDR("PDF/A", Level.INFO, Collections.<Constraint>singletonList(new StartsWithConstraint("PDF/A-1B")));
        assertEquals(Level.INFO, rule.getLevel());
        assertEquals(Collections.singletonList("PDF/A: hodnota \"PDF/A-1b validation profile\" neodpovídá žádnému z omezení: [musí začínat na 'PDF/A-1B']"),
                rule.validate("PDF/A-1b validation profile"));
    }

    @Test
    public void abstractDataRuleToString() {
        MustExistDR rule = new MustExistDR("x");
        assertNull(rule.toString(null));
        assertEquals("[1,2]", rule.toString(Arrays.asList(1, 2)));
        assertEquals("abc", rule.toString("abc"));
    }
}
