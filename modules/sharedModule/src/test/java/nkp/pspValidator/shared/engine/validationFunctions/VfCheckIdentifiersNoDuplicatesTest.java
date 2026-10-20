package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VfCheckIdentifiersNoDuplicatesTest {

    private static final String VF_NAME = "checkIdentifiersNoDuplicates";

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }

    private ValidationResult validate(List<Identifier> ids, List<String> ignoredTypes) {
        VfCheckIdentifiersNoDuplicates vf = new VfCheckIdentifiersNoDuplicates(VF_NAME, engine);
        return vf.validate(Collections.singletonList(ids), ignoredTypes);
    }

    @Test
    public void noDuplicates() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "b"), new Identifier("urnnbn", "a"));
        assertFalse(validate(ids, Collections.emptyList()).hasProblems());
    }

    @Test
    public void duplicateOfFirstValue() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "b"), new Identifier("uuid", "a"));
        assertEquals(1, validate(ids, Collections.emptyList()).getProblems().size());
    }

    /**
     * Duplicita jine nez prvni hodnoty daneho typu (uuid A, B, B); puvodni implementace ji neodhalila.
     */
    @Test
    public void duplicateOfLaterValue() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "b"), new Identifier("uuid", "b"));
        assertEquals(1, validate(ids, Collections.emptyList()).getProblems().size());
    }

    @Test
    public void sameValueDifferentTypeIsNotDuplicate() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("sysno", "a"));
        assertFalse(validate(ids, Collections.emptyList()).hasProblems());
    }

    @Test
    public void ignoredTypeNotChecked() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("uuid", "a"));
        assertFalse(validate(ids, Collections.singletonList("uuid")).hasProblems());
    }

    @Test
    public void tripleDuplicateReportedTwice() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "a"), new Identifier("uuid", "a"));
        assertEquals(2, validate(ids, Collections.emptyList()).getProblems().size());
    }
}
