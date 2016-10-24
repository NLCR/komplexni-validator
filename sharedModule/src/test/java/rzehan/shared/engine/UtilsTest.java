package rzehan.shared.engine;

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
 * Created by martin on 21.10.16.
 */
public class UtilsTest {


    @Test
    public void simpleTypes() {
        assertTrue(Utils.instanceOf("String", "neco"));
        assertFalse(Utils.instanceOf("String", 3));
        assertFalse(Utils.instanceOf("String", new File("/something")));

        assertTrue(Utils.instanceOf("Integer", 3));
        assertTrue(Utils.instanceOf("Integer", 0));
        assertTrue(Utils.instanceOf("Integer", -3));
        assertFalse(Utils.instanceOf("Integer", "6"));
        assertFalse(Utils.instanceOf("Integer", new File("/something")));

        assertTrue(Utils.instanceOf("File", new File("/something")));
        assertFalse(Utils.instanceOf("File", "/something"));
        assertFalse(Utils.instanceOf("File", 3));
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
        assertTrue(Utils.instanceOf("List", new ArrayList()));
        assertTrue(Utils.instanceOf("List", new ArrayList<String>()));
        assertFalse(Utils.instanceOf("List", new HashSet()));
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
        assertTrue(Utils.instanceOf("List<String>", new ArrayList<String>()));
        //assertFalse(TestUtils.instanceOf("List<String>", new ArrayList<Integer>()));
        //assertFalse(TestUtils.instanceOf("List<String>", new ArrayList()));

        assertTrue(Utils.instanceOf("List<String>", strings));
        assertFalse(Utils.instanceOf("List<String>", integers));
        assertFalse(Utils.instanceOf("List<String>", files));

        //List<File>
        assertTrue(Utils.instanceOf("List<File>", new ArrayList<File>()));
        //assertFalse(TestUtils.instanceOf("List<File>", new ArrayList<Integer>()));
        //assertFalse(TestUtils.instanceOf("List<File>", new ArrayList()));

        assertTrue(Utils.instanceOf("List<File>", files));
        assertFalse(Utils.instanceOf("List<File>", strings));
        assertFalse(Utils.instanceOf("List<File>", integers));
    }

}
