package nkp.pspValidator.shared.engine;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

/**
 * Created by Martin Řehánekrehanek on 07.12.16.
 */
public class MacTest {

    @Test
    public void testBufferedReader() throws IOException {
        String multiline = "first\nsecond\nthird";
        BufferedReader reader = new BufferedReader(new StringReader(multiline));

        String firstLine = reader.readLine();
        assertEquals("first", firstLine);
        String secondLine = reader.readLine();
        assertEquals("second", secondLine);
        String thirdLine = reader.readLine();
        assertEquals("third", thirdLine);
        assertEquals(null, reader.readLine());
    }


}
