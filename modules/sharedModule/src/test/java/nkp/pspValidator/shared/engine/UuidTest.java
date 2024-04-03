package nkp.pspValidator.shared.engine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests regular expressions for UUID
 *
 * @see <a href="https://www.ietf.org/rfc/rfc4122.txt">https://www.ietf.org/rfc/rfc4122.txt</a>
 */
public class UuidTest {

    String regexpUuid = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";

    private boolean isValidUuid(String str) {
        return str.matches(regexpUuid);
    }

    @Test
    public void validSample() {
        assertTrue(isValidUuid("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    public void validSampleNill() {
        assertTrue(isValidUuid("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    public void validSampleUpperCase() {
        assertTrue(isValidUuid("123E4567-E89b-12D3-A456-426614174000"));
    }

    @Test
    public void invalidContainingPrefix() {
        assertFalse(isValidUuid("uuid:123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    public void invalidMissingSeparators() {
        assertFalse(isValidUuid("uuid:123e4567e89b-12d3-a456-426614174000"));
        assertFalse(isValidUuid("uuid:123e4567-e89b12d3-a456-426614174000"));
        assertFalse(isValidUuid("uuid:123e4567-e89b-12d3a456-426614174000"));
        assertFalse(isValidUuid("uuid:123e4567-e89b-12d3-a456426614174000"));
        assertFalse(isValidUuid("uuid:123e4567e89b12d3a456426614174000"));
    }

    @Test
    public void invalidContainingNonhexaChars() {
        assertFalse(isValidUuid("uuid:123g4567-e89b-12d3-a456-426614174000"));
        assertFalse(isValidUuid("uuid:123e4567-x89b-12d3-a456-426614174000"));
        assertFalse(isValidUuid("uuid:123e4567-e89b-12z3-a456-426614174000"));
        assertFalse(isValidUuid("uuid:123e4567-e89b-12d3-.456-426614174000"));
    }
}
