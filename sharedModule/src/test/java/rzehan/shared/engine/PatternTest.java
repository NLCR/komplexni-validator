package rzehan.shared.engine;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.evaluationFunctions.TestUtils;

import static org.junit.Assert.*;

/**
 * Created by martin on 20.10.16.
 */
public class PatternTest {

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine();
        engine.setProvidedString("STR1", "neco");
        engine.setProvidedString("STR2", "nic");
        engine.setProvidedInteger("NUM1", 6);
        engine.setProvidedString("PSP_ID", "b50eb6b0-f0a4-11e3-b72e-005056827e52");
        engine.setProvidedString("DOT", ".");
    }


    @Test
    public void noVariableRegexpConstant() {
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec")).evaluate().matches("amdsec"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "AMDSEC")).evaluate().matches("AMDSEC"));

        //case sensitivity
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec")).evaluate().matches("AMDSEC"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "AMDSEC")).evaluate().matches("amdsec"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "amdsec")).evaluate().matches("AMDSEC"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "AMDSEC")).evaluate().matches("amdsec"));

        //more expressions per pattern
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec"), engine.buildRawPatternExpression(true, "ocr")).evaluate().matches("amdsec"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec"), engine.buildRawPatternExpression(true, "ocr")).evaluate().matches("ocr"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec"), engine.buildRawPatternExpression(true, "ocr")).evaluate().matches("alto"));
    }

    @Test
    public void noVariableRegexp() {
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml")).evaluate().matches("info_SOMETHING.xml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml")).evaluate().matches("info_SOMETHINGxml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml")).evaluate().matches("info_.xml"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info\\.xml")).evaluate().matches("info.xml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info\\.xml")).evaluate().matches("info,xml"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml"), engine.buildRawPatternExpression(true, "info.xml")).evaluate().matches("info.xml"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml"), engine.buildRawPatternExpression(true, "info.xml")).evaluate().matches("info_SOMETHING.xml"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "[a-z_\\-\\.]+")).evaluate().matches("x.y_z-a"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "[a-z_\\-\\.]+")).evaluate().matches(""));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "[a-z_\\-\\.]+")).evaluate().matches("X.y_Z-a"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "[a-z0-9_\\-\\.]+")).evaluate().matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
    }


    @Test
    public void variableRegexp() {
        TestUtils.defineProvidedStringVar(engine, "STR1");
        TestUtils.defineProvidedStringVar(engine, "STR2");

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${STR1}_suffix")).evaluate().matches("prefix_neco_suffix"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${STR1}\\+${STR2}_suffix")).evaluate().matches("prefix_neco+nic_suffix"));

        //variable not defined
        PatternEvaluation evaluationVarNotDefined = engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${STR3}_suffix")).evaluate();
        assertFalse(evaluationVarNotDefined.isOk());
        assertNotNull(evaluationVarNotDefined.getErrorMessage());


        //TODO: ted bere vsechno, klidne zavola toString() na List<File>
/*
        //invalid varialble type
        TestUtils.defineProvidedIntegerVar(engine, "NUM1");
        PatternEvaluation evaluationInvalidVarType = engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${NUM1}_suffix")).evaluate();
        assertFalse(evaluationInvalidVarType.isOk());
        assertNotNull(evaluationInvalidVarType.getErrorMessage());
*/

        TestUtils.defineProvidedStringVar(engine, "PSP_ID");
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_${PSP_ID}\\.xml")).evaluate().matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).evaluate().matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_0.xml"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).evaluate().matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_123.xml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).evaluate().matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_.xml"));

        //escaping special characters in variables - \\.[]{}()*+-?^$|
        TestUtils.defineProvidedStringVar(engine, "DOT");
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "${DOT}co${DOT}uk")).evaluate().matches(".co.uk"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "${DOT}co${DOT}uk")).evaluate().matches("0coXuk"));
    }

    @Test
    public void other() {
        assertTrue(engine.buildPatternDefinition(
                engine.buildRawPatternExpression(true, "info_.+\\.xml"),
                engine.buildRawPatternExpression(true, "info\\.xml")
        ).evaluate().matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
    }


}

