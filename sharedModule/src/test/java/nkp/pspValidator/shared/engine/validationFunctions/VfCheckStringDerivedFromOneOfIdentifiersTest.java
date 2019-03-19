package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Martin Řehánek on 4.11.16.
 */
public class VfCheckStringDerivedFromOneOfIdentifiersTest {

    private static final String VF_NAME = "checkStringDerivedFromOneOfIdentifiers";

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }


    @Test
    public void fromUuidOk() {
        VfCheckStringDerivedFromOneOfIdentifiers vf = new VfCheckStringDerivedFromOneOfIdentifiers(VF_NAME, engine);
        ValidationResult result = vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", 6,
                ids(
                        id("ccnb", "cnb000572615"),
                        id("urnnbn", "urn:nbn:cz:nk-00172f"),
                        id("uuid", "b50eb6b0-f0a4-11e3-b72e-005056827e52")
                ),
                prefixes("cnb", "urn:nbn:cz:", "uuid:")
        );

        assertFalse(result.hasProblems());
    }

    @Test
    public void fromCcnbOk() {
        VfCheckStringDerivedFromOneOfIdentifiers vf = new VfCheckStringDerivedFromOneOfIdentifiers(VF_NAME, engine);
        ValidationResult result = vf.validate("000572615", 6,
                ids(
                        id("ccnb", "cnb000572615"),
                        id("urnnbn", "urn:nbn:cz:nk-00172f"),
                        id("uuid", "b50eb6b0-f0a4-11e3-b72e-005056827e52")
                ),
                prefixes("cnb", "urn:nbn:cz:", "uuid:")
        );
        assertFalse(result.hasProblems());
    }

    @Test
    public void fromUrnOk() {
        VfCheckStringDerivedFromOneOfIdentifiers vf = new VfCheckStringDerivedFromOneOfIdentifiers(VF_NAME, engine);
        ValidationResult result = vf.validate("00172f", 6,
                ids(
                        id("ccnb", "cnb000572615"),
                        id("urnnbn", "urn:nbn:cz:nk-00172f"),
                        id("uuid", "b50eb6b0-f0a4-11e3-b72e-005056827e52")
                ),
                prefixes("cnb", "urn:nbn:cz:", "uuid:")
        );
        assertFalse(result.hasProblems());
    }

    @Test
    public void fromUrnTooShort() {
        VfCheckStringDerivedFromOneOfIdentifiers vf = new VfCheckStringDerivedFromOneOfIdentifiers(VF_NAME, engine);
        ValidationResult result = vf.validate("00172f", 6,
                ids(
                        id("ccnb", "cnb000572615"),
                        id("urnnbn", "urn:nbn:cz:nk-123"),
                        id("uuid", "b50eb6b0-f0a4-11e3-b72e-005056827e52")
                ),
                prefixes("cnb", "urn:nbn:cz:", "uuid:")
        );
        assertTrue(result.hasProblems());
    }


    @Test
    public void fromUuidErrorUuidMissing() {
        VfCheckStringDerivedFromOneOfIdentifiers vf = new VfCheckStringDerivedFromOneOfIdentifiers(VF_NAME, engine);
        ValidationResult result = vf.validate("b50eb6b0-f0a4-11e3-b72e-005056827e52", 6,
                ids(
                        id("ccnb", "cnb000572615"),
                        id("urnnbn", "urn:nbn:cz:nk-00172f")
                ),
                prefixes("cnb", "urn:nbn:cz:")
        );
        assertTrue(result.hasProblems());
    }

    private List<Identifier> ids(Identifier... ids) {
        return Arrays.asList(ids);
    }

    private List<String> prefixes(String... prefix) {
        return Arrays.asList(prefix);
    }

    private Identifier id(String type, String value) {
        return new Identifier(type, value);
    }
}
