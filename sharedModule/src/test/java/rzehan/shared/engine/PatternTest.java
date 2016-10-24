package rzehan.shared.engine;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.evaluationFunctions.TestUtils;
import rzehan.shared.engine.exceptions.InvalidVariableTypeException;
import rzehan.shared.engine.exceptions.VariableNotDefinedException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
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
        assertTrue(engine.newPattern(engine.newExpression(true, "amdsec")).matches("amdsec"));
        assertTrue(engine.newPattern(engine.newExpression(true, "AMDSEC")).matches("AMDSEC"));

        //case sensitivity
        assertFalse(engine.newPattern(engine.newExpression(true, "amdsec")).matches("AMDSEC"));
        assertFalse(engine.newPattern(engine.newExpression(true, "AMDSEC")).matches("amdsec"));
        assertTrue(engine.newPattern(engine.newExpression(false, "amdsec")).matches("AMDSEC"));
        assertTrue(engine.newPattern(engine.newExpression(false, "AMDSEC")).matches("amdsec"));

        //more expressions per pattern
        assertTrue(engine.newPattern(engine.newExpression(true, "amdsec"), engine.newExpression(true, "ocr")).matches("amdsec"));
        assertTrue(engine.newPattern(engine.newExpression(true, "amdsec"), engine.newExpression(true, "ocr")).matches("ocr"));
        assertFalse(engine.newPattern(engine.newExpression(true, "amdsec"), engine.newExpression(true, "ocr")).matches("alto"));
    }

    @Test
    public void noVariableRegexp() {
        assertTrue(engine.newPattern(engine.newExpression(true, "info_.+\\.xml")).matches("info_SOMETHING.xml"));
        assertFalse(engine.newPattern(engine.newExpression(true, "info_.+\\.xml")).matches("info_SOMETHINGxml"));
        assertFalse(engine.newPattern(engine.newExpression(true, "info_.+\\.xml")).matches("info_.xml"));

        assertTrue(engine.newPattern(engine.newExpression(true, "info\\.xml")).matches("info.xml"));
        assertFalse(engine.newPattern(engine.newExpression(true, "info\\.xml")).matches("info,xml"));

        assertTrue(engine.newPattern(engine.newExpression(true, "info_.+\\.xml"), engine.newExpression(true, "info.xml")).matches("info.xml"));
        assertTrue(engine.newPattern(engine.newExpression(true, "info_.+\\.xml"), engine.newExpression(true, "info.xml")).matches("info_SOMETHING.xml"));

        assertTrue(engine.newPattern(engine.newExpression(true, "[a-z_\\-\\.]+")).matches("x.y_z-a"));
        assertFalse(engine.newPattern(engine.newExpression(true, "[a-z_\\-\\.]+")).matches(""));
        assertTrue(engine.newPattern(engine.newExpression(false, "[a-z_\\-\\.]+")).matches("X.y_Z-a"));
    }


    @Test
    public void variableRegexp() {
        TestUtils.defineProvidedStringVar(engine, "STR1");
        TestUtils.defineProvidedStringVar(engine, "STR2");

        assertTrue(engine.newPattern(engine.newExpression(true, "prefix_${STR1}_suffix")).matches("prefix_neco_suffix"));
        assertTrue(engine.newPattern(engine.newExpression(true, "prefix_${STR1}\\+${STR2}_suffix")).matches("prefix_neco+nic_suffix"));
        try {
            engine.newPattern(engine.newExpression(true, "prefix_${STR3}_suffix")).matches("prefix_vsechno_suffix");
            fail();
        } catch (VariableNotDefinedException e) {

        }

        TestUtils.defineProvidedIntegerVar(engine, "NUM1");
        try {
            engine.newPattern(engine.newExpression(true, "prefix_${NUM1}_suffix")).matches("prefix_3_suffix");
            fail();
        } catch (InvalidVariableTypeException e) {

        }

        TestUtils.defineProvidedStringVar(engine, "PSP_ID");
        assertTrue(engine.newPattern(engine.newExpression(true, "info_${PSP_ID}\\.xml")).matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
        assertTrue(engine.newPattern(engine.newExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_0.xml"));
        assertTrue(engine.newPattern(engine.newExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_123.xml"));
        assertFalse(engine.newPattern(engine.newExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_.xml"));

        //escaping special characters in variables - \\.[]{}()*+-?^$|
        TestUtils.defineProvidedStringVar(engine, "DOT");
        assertTrue(engine.newPattern(engine.newExpression(true, "${DOT}co${DOT}uk")).matches(".co.uk"));
        assertFalse(engine.newPattern(engine.newExpression(true, "${DOT}co${DOT}uk")).matches("0coXuk"));
    }


}

