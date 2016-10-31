package rzehan.shared.engine;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.evaluationFunctions.TestUtils;
import rzehan.shared.engine.exceptions.InvalidVariableTypeException;
import rzehan.shared.engine.exceptions.VariableNotDefinedException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by martin on 20.10.16.
 */
public class PatternTest {

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addString("STR1", "neco");
        pvMgr.addString("STR2", "nic");
        pvMgr.addInteger("NUM1", 6);
        pvMgr.addString("PSP_ID", "b50eb6b0-f0a4-11e3-b72e-005056827e52");
        pvMgr.addString("DOT", ".");
        engine = new Engine(pvMgr);
    }


    @Test
    public void noVariableRegexpConstant() {
        assertTrue(engine.buildPattern(engine.buildExpression(true, "amdsec")).matches("amdsec"));
        assertTrue(engine.buildPattern(engine.buildExpression(true, "AMDSEC")).matches("AMDSEC"));

        //case sensitivity
        assertFalse(engine.buildPattern(engine.buildExpression(true, "amdsec")).matches("AMDSEC"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "AMDSEC")).matches("amdsec"));
        assertTrue(engine.buildPattern(engine.buildExpression(false, "amdsec")).matches("AMDSEC"));
        assertTrue(engine.buildPattern(engine.buildExpression(false, "AMDSEC")).matches("amdsec"));

        //more expressions per pattern
        assertTrue(engine.buildPattern(engine.buildExpression(true, "amdsec"), engine.buildExpression(true, "ocr")).matches("amdsec"));
        assertTrue(engine.buildPattern(engine.buildExpression(true, "amdsec"), engine.buildExpression(true, "ocr")).matches("ocr"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "amdsec"), engine.buildExpression(true, "ocr")).matches("alto"));
    }

    @Test
    public void noVariableRegexp() {
        assertTrue(engine.buildPattern(engine.buildExpression(true, "info_.+\\.xml")).matches("info_SOMETHING.xml"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "info_.+\\.xml")).matches("info_SOMETHINGxml"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "info_.+\\.xml")).matches("info_.xml"));

        assertTrue(engine.buildPattern(engine.buildExpression(true, "info\\.xml")).matches("info.xml"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "info\\.xml")).matches("info,xml"));

        assertTrue(engine.buildPattern(engine.buildExpression(true, "info_.+\\.xml"), engine.buildExpression(true, "info.xml")).matches("info.xml"));
        assertTrue(engine.buildPattern(engine.buildExpression(true, "info_.+\\.xml"), engine.buildExpression(true, "info.xml")).matches("info_SOMETHING.xml"));

        assertTrue(engine.buildPattern(engine.buildExpression(true, "[a-z_\\-\\.]+")).matches("x.y_z-a"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "[a-z_\\-\\.]+")).matches(""));
        assertTrue(engine.buildPattern(engine.buildExpression(false, "[a-z_\\-\\.]+")).matches("X.y_Z-a"));

        assertTrue(engine.buildPattern(engine.buildExpression(false, "[a-z0-9_\\-\\.]+")).matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
    }


    @Test
    public void variableRegexp() {
        TestUtils.defineProvidedStringVar(engine, "STR1");
        TestUtils.defineProvidedStringVar(engine, "STR2");

        assertTrue(engine.buildPattern(engine.buildExpression(true, "prefix_${STR1}_suffix")).matches("prefix_neco_suffix"));
        assertTrue(engine.buildPattern(engine.buildExpression(true, "prefix_${STR1}\\+${STR2}_suffix")).matches("prefix_neco+nic_suffix"));
        try {
            engine.buildPattern(engine.buildExpression(true, "prefix_${STR3}_suffix")).matches("prefix_vsechno_suffix");
            fail();
        } catch (VariableNotDefinedException e) {

        }

        TestUtils.defineProvidedIntegerVar(engine, "NUM1");
        try {
            engine.buildPattern(engine.buildExpression(true, "prefix_${NUM1}_suffix")).matches("prefix_3_suffix");
            fail();
        } catch (InvalidVariableTypeException e) {

        }

        TestUtils.defineProvidedStringVar(engine, "PSP_ID");
        assertTrue(engine.buildPattern(engine.buildExpression(true, "info_${PSP_ID}\\.xml")).matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
        assertTrue(engine.buildPattern(engine.buildExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_0.xml"));
        assertTrue(engine.buildPattern(engine.buildExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_123.xml"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_.xml"));

        //escaping special characters in variables - \\.[]{}()*+-?^$|
        TestUtils.defineProvidedStringVar(engine, "DOT");
        assertTrue(engine.buildPattern(engine.buildExpression(true, "${DOT}co${DOT}uk")).matches(".co.uk"));
        assertFalse(engine.buildPattern(engine.buildExpression(true, "${DOT}co${DOT}uk")).matches("0coXuk"));
    }

    @Test
    public void other() {
        assertTrue(engine.buildPattern(
                engine.buildExpression(true, "info_.+\\.xml"),
                engine.buildExpression(true, "info\\.xml")
        ).matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
    }


}

