package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VfCheckStringDerivedFromUrnnbnOrUuidTest {

    private static final String VF_NAME = "checkStringDerivedFromUrnnbnOrUuid";

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }


    @Test
    public void noUuidNorUrnnbn() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        assertTrue(vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", null, null).hasProblems());
    }

    @Test
    public void derivedFromUuid() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        assertFalse(vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", "b50eb6b0-f0a4-11e3-b72e-005056827e52", null).hasProblems());
    }

    /**
     * Issue #258: DMF odkazuji u UUID na RFC 4122 a velka pismena nezakazuji; UUID velkymi pismeny je platne a nazev
     * balicku malymi pismeny je od nej odvozeny.
     */
    @Test
    public void upperCaseUuidIsValidAndMatchesLowerCaseName() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        assertFalse(vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", "B50EB6B0-F0A4-11E3-B72E-005056827E52", null).hasProblems());
        assertFalse(vf.validate("B50EB6B0-F0A4-11E3-B72E-005056827E52", "B50EB6B0-F0A4-11E3-B72E-005056827E52", null).hasProblems());
    }

    @Test
    public void invalidUuidFormatIsError() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        for (String invalid : new String[]{"b50eb6b0-f0a4-11e3-b72e-005056827e5", "b50eb6b0-f0a4-11e3-b72e-005056827e5g"}) {
            ValidationResult result = vf.validate(invalid, invalid, null);
            assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
            assertEquals("hodnota není ve formátu UUID", result.getProblems().get(0).getSimpleMessage());
        }
    }

    @Test
    public void derivedFromUuidWithPrefix() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        ValidationResult result = vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", "uuid:b50eb6b0-f0a4-11e3-b72e-005056827e52", null);
        List<ValidationProblem> problems = result.getProblems();
        assertEquals(1, problems.size()); //only single problem
        ValidationProblem problem = problems.get(0);
        assertEquals(Level.WARNING, problem.getLevel()); //warning
        assertEquals("identifikátor UUID by měl být bez prefixu 'uuid:'", problem.getSimpleMessage());
    }

    @Test
    public void notDerivedFromUuid() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        assertTrue(vf.validate("uuid:b50eb6b0-f0a4-11e3-b72e-005056827e52", "uuid:b50eb6b0-f0a4-11e3-b72e-005056827e52", null).hasProblems());
        assertTrue(vf.validate("f0a4-11e3-b72e-005056827e52", "uuid:b50eb6b0-f0a4-11e3-b72e-005056827e52", null).hasProblems());
    }

    @Test
    public void uuidInvalid() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        assertTrue(vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", "uuid;b50eb6b0-f0a4-11e3-b72e-005056827e52", null).hasProblems());
        assertTrue(vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", "UUID:b50eb6b0-f0a4-11e3-b72e-005056827e52", null).hasProblems());

        assertTrue(vf.validate("b50eb6b0-f0a4-11e3-005056827e52", "uuid:b50eb6b0-f0a4-11e3-005056827e52", null).hasProblems());
        assertTrue(vf.validate("b50eb6b0-f0a4-11e3-b72e-b72e-005056827e52", "uuid:b50eb6b0-f0a4-11e3-b72e-b72e-005056827e52", null).hasProblems());
        assertTrue(vf.validate("b50eb6b-f0a4-11e3-b72e-005056827e52", "uuid:b50eb6b-f0a4-11e3-b72e-005056827e52", null).hasProblems());
        assertTrue(vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e5", "uuid:b50eb6b0-f0a4-11e3-b72e-005056827e5", null).hasProblems());
        assertTrue(vf.validate("b50eb6b0a-f0a4-11e3-b72e-005056827e52", "uuid:b50eb6b0a-f0a4-11e3-b72e-005056827e52", null).hasProblems());
        assertTrue(vf.validate("b50eb6bg-f0a4-11e3-b72e-005056827e52", "uuid:b50eb6bg-f0a4-11e3-b72e-005056827e52", null).hasProblems());
    }

    @Test
    public void derivedFromUrnNbn() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        for (ValidationProblem p : vf.validate("osa001-001kl9", null, "urn:nbn:cz:osa001-001kl9").getProblems()) {
            System.out.printf(p.getFullMessage());
        }
        assertFalse(vf.validate("osa001-001kl9", null, "urn:nbn:cz:osa001-001kl9").hasProblems());
    }

    @Test
    public void urnNbnInvalid() {
        VfCheckStringDerivedFromUrnnbnOrUuid vf = new VfCheckStringDerivedFromUrnnbnOrUuid(VF_NAME, engine);
        assertTrue(vf.validate("osa001-001kl9", null, "urn:nbn:CZ:osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "URN:NBN:CZ:osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "urn:nbn:osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "urn:nbn:de:osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "cz:osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "urnnbn:cz:osa001-001kl9").hasProblems());
        assertTrue(vf.validate("osa001-001kl9", null, "urn-nbn:cz:osa001-001kl9").hasProblems());

        assertTrue(vf.validate("osa001-001kl", null, "urn:nbn:cz:osa001-001kl").hasProblems());
        assertTrue(vf.validate("osa001-001kl9a", null, "urn:nbn:cz:osa001-001kl9a").hasProblems());
        assertTrue(vf.validate("osa001a-001kl9", null, "urn:nbn:cz:osa0011-001kl9").hasProblems());
        assertTrue(vf.validate("o-001kl9", null, "urn:nbn:cz:o-001kl9").hasProblems());
        assertTrue(vf.validate("osa001:001kl9", null, "urn:nbn:cz:osa001:001kl9").hasProblems());
        assertTrue(vf.validate("osa001_001kl9", null, "urn:nbn:cz:osa001_001kl9").hasProblems());
    }
}
