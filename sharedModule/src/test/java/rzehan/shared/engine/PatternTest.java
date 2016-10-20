package rzehan.shared.engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by martin on 20.10.16.
 */
public class PatternTest {


    @Test
    public void noVariableRegexpConstant() {
        assertTrue(new Pattern(new Pattern.Expression(true, "amdsec")).matches("amdsec"));
        assertTrue(new Pattern(new Pattern.Expression(true, "AMDSEC")).matches("AMDSEC"));

        //case sensitivity
        assertFalse(new Pattern(new Pattern.Expression(true, "amdsec")).matches("AMDSEC"));
        assertFalse(new Pattern(new Pattern.Expression(true, "AMDSEC")).matches("amdsec"));
        assertTrue(new Pattern(new Pattern.Expression(false, "amdsec")).matches("AMDSEC"));
        assertTrue(new Pattern(new Pattern.Expression(false, "AMDSEC")).matches("amdsec"));

        //more expressions per pattern
        assertTrue(new Pattern(new Pattern.Expression(true, "amdsec"), new Pattern.Expression(true, "ocr"))
                .matches("amdsec"));
        assertTrue(new Pattern(new Pattern.Expression(true, "amdsec"), new Pattern.Expression(true, "ocr"))
                .matches("ocr"));
        assertFalse(new Pattern(new Pattern.Expression(true, "amdsec"), new Pattern.Expression(true, "ocr"))
                .matches("alto"));
    }

    @Test
    public void noVariableRegexp() {
        assertTrue(new Pattern(new Pattern.Expression(true, "info_.+\\.xml")).matches("info_SOMETHING.xml"));
        assertFalse(new Pattern(new Pattern.Expression(true, "info_.+\\.xml")).matches("info_SOMETHINGxml"));
        assertFalse(new Pattern(new Pattern.Expression(true, "info_.+\\.xml")).matches("info_.xml"));

        assertTrue(new Pattern(new Pattern.Expression(true, "info\\.xml")).matches("info.xml"));
        assertFalse(new Pattern(new Pattern.Expression(true, "info\\.xml")).matches("info,xml"));

        assertTrue(new Pattern(new Pattern.Expression(true, "info_.+\\.xml"), new Pattern.Expression(true, "info.xml")).matches("info.xml"));
        assertTrue(new Pattern(new Pattern.Expression(true, "info_.+\\.xml"), new Pattern.Expression(true, "info.xml")).matches("info_SOMETHING.xml"));

        assertTrue(new Pattern(new Pattern.Expression(true, "[a-z_\\-\\.]+")).matches("x.y_z-a"));
        assertFalse(new Pattern(new Pattern.Expression(true, "[a-z_\\-\\.]+")).matches(""));
        assertTrue(new Pattern(new Pattern.Expression(false, "[a-z_\\-\\.]+")).matches("X.y_Z-a"));
    }


}
