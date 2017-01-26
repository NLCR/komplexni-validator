package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.StringUtils;
import nkp.pspValidator.shared.engine.exceptions.InvalidIdException;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class UtilsTest {


    @Test
    public void simpleTypes() {
        assertTrue(Utils.instanceOf("string", "neco"));
        assertFalse(Utils.instanceOf("string", 3));
        assertFalse(Utils.instanceOf("string", new File("/something")));

        assertTrue(Utils.instanceOf("integer", 3));
        assertTrue(Utils.instanceOf("integer", 0));
        assertTrue(Utils.instanceOf("integer", -3));
        assertFalse(Utils.instanceOf("integer", "6"));
        assertFalse(Utils.instanceOf("integer", new File("/something")));

        assertTrue(Utils.instanceOf("file", new File("/something")));
        assertFalse(Utils.instanceOf("file", "/something"));
        assertFalse(Utils.instanceOf("file", 3));
    }

    @Test
    public void unknownSimpleType() {
        try {
            Utils.instanceOf("NeznamyTyp", "neco");
            fail();
        } catch (RuntimeException e) {

        }
    }

    @Test
    public void collections() {
        assertTrue(Utils.instanceOf("list", new ArrayList()));
        assertTrue(Utils.instanceOf("list", new ArrayList<String>()));
        assertFalse(Utils.instanceOf("list", new HashSet()));
    }

    @Test
    public void collectionsGeneric() {
        List<String> strings = new ArrayList<String>();
        strings.add("something");
        List<Integer> integers = new ArrayList<Integer>();
        integers.add(3);
        List<File> files = new ArrayList<File>();
        files.add(new File("/something"));

        //List<String>
        assertTrue(Utils.instanceOf("string_list", new ArrayList<String>()));
        //assertFalse(TestUtils.instanceOf("string_list", new ArrayList<Integer>()));
        //assertFalse(TestUtils.instanceOf("string_list", new ArrayList()));

        assertTrue(Utils.instanceOf("string_list", strings));
        assertFalse(Utils.instanceOf("string_list", integers));
        assertFalse(Utils.instanceOf("string_list", files));

        //List<File>
        assertTrue(Utils.instanceOf("file_list", new ArrayList<File>()));
        //assertFalse(TestUtils.instanceOf("file_list", new ArrayList<Integer>()));
        //assertFalse(TestUtils.instanceOf("file_list", new ArrayList()));

        assertTrue(Utils.instanceOf("file_list", files));
        assertFalse(Utils.instanceOf("file_list", strings));
        assertFalse(Utils.instanceOf("file_list", integers));
    }


    @Test
    public void findLongestCommonSubstrings() {

        assertTrue(Utils.findLongestCommonSubstrings("abab", "baba").contains("aba"));
        assertTrue(Utils.findLongestCommonSubstrings("abab", "baba").contains("bab"));
        assertEquals(2, Utils.findLongestCommonSubstrings("abab", "baba").size());

        assertTrue(Utils.findLongestCommonSubstrings("abcde", "bcd").contains("bcd"));
        assertTrue(Utils.findLongestCommonSubstrings("bcd", "abcde").contains("bcd"));
        assertEquals(1, Utils.findLongestCommonSubstrings("one", "chosenone").size());

        assertEquals(0, Utils.findLongestCommonSubstrings("abc", "xyz").size());
    }

    @Test
    public void getLongestCommonSubstringLength() {
        String uuid = "b50eb6b0-f0a4-11e3-b72e-005056827e52";
        assertEquals(uuid.length(), Utils.getLongestCommonSubstringLength("uuid:" + uuid, uuid));
    }

    @Test
    public void extractIdentifierFromDcStringCcnb() throws InvalidIdException {
        String type = "ccnb";
        String value = "cnb001315579";
        String dcString = String.format("%s:%s", type, value);
        Identifier id = Utils.extractIdentifierFromDcString(dcString);
        assertEquals(type, id.getType());
        assertEquals(value, id.getValue());
        assertEquals(dcString, id.toString());
    }

    @Test
    public void extractIdentifierFromDcStringUuid() throws InvalidIdException {
        String type = "uuid";
        String value = "6d5a1810-0377-11e4-9806-005056825209";
        String dcString = String.format("%s:%s", type, value);
        Identifier id = Utils.extractIdentifierFromDcString(dcString);
        assertEquals(type, id.getType());
        assertEquals(value, id.getValue());
        assertEquals(dcString, id.toString());
    }

    @Test
    public void extractIdentifierFromDcStringBarCode() throws InvalidIdException {
        String type = "barCode";
        String value = "1001149769";
        String dcString = String.format("%s:%s", type, value);
        Identifier id = Utils.extractIdentifierFromDcString(dcString);
        assertEquals(type, id.getType());
        assertEquals(value, id.getValue());
        assertEquals(dcString, id.toString());
    }

    @Test
    public void extractIdentifierFromDcStringUrnNbn() throws InvalidIdException {
        String type = "urnnbn";
        String value = "urn:nbn:cz:nk-0016ke";
        String dcString = String.format("%s:%s", type, value);
        Identifier id = Utils.extractIdentifierFromDcString(dcString);
        assertEquals(type, id.getType());
        assertEquals(value, id.getValue());
        assertEquals(dcString, id.toString());
    }

    @Test
    public void formatMilliseconds() {
        assertEquals("0 ms", StringUtils.formatMilliseconds(0));
        assertEquals("1 ms", StringUtils.formatMilliseconds(1));
        assertEquals("99 ms", StringUtils.formatMilliseconds(99));
        assertEquals("1 s, 0 ms", StringUtils.formatMilliseconds(1000));
        assertEquals("5 s, 1 ms", StringUtils.formatMilliseconds(5001));
        assertEquals("2 s, 999 ms", StringUtils.formatMilliseconds(2999));
        assertEquals("59 s, 0 ms", StringUtils.formatMilliseconds(59000));
        assertEquals("1 m, 0 s, 0 ms", StringUtils.formatMilliseconds(60000));
        assertEquals("1 m, 2 s, 1 ms", StringUtils.formatMilliseconds(2001 + 60 * 1000));
        assertEquals("1 m, 1 s, 1 ms", StringUtils.formatMilliseconds(1 + 1000 + 60 * 1000));
        assertEquals("1 h, 1 m, 1 s, 1 ms", StringUtils.formatMilliseconds(1 + 1000 + 60 * 1000 + 60 * 60 * 1000));
    }

}
