package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VfCheckIdentifiersOnlyTypesPresentTest {

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }

    private ValidationResult validate(List<Identifier> ids, List<String> allowed, Level level) {
        VfCheckIdentifiersOnlyTypesPresent vf = new VfCheckIdentifiersOnlyTypesPresent("checkIdentifiersOnlyTypesPresent", engine);
        return vf.validate("TEST", allowed, Collections.singletonList(ids), level);
    }

    @Test
    public void allKnown() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("sysno", "b"));
        assertFalse(validate(ids, Arrays.asList("uuid", "sysno"), Level.WARNING).hasProblems());
    }

    @Test
    public void unknownTypeReportedWithGivenLevel() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("inventory", "b"));
        ValidationResult result = validate(ids, Collections.singletonList("uuid"), Level.WARNING);
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.WARNING, result.getProblems().get(0).getLevel());
    }

    @Test
    public void caseSensitive() {
        List<Identifier> ids = Collections.singletonList(new Identifier("UUID", "a"));
        assertEquals(1, validate(ids, Collections.singletonList("uuid"), Level.ERROR).getProblems().size());
    }
}
