package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ProvidedVarsManagerImpl;
import rzehan.shared.engine.ValueType;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by martin on 21.10.16.
 */
public class EfProvidedStringTest {

    private static final String FUNCTION_NAME = "PROVIDED_STRING";

    private static Engine engine;
    private static String PSP_ID_STRINGID = "PSP_ID";
    private static String PSP_ID_VALUE = "b50eb6b0-f0a4-11e3-b72e-005056827e52";


    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addString(PSP_ID_STRINGID, PSP_ID_VALUE);

        engine = new Engine(pvMgr);
    }

    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("string_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, PSP_ID_STRINGID));
        evFunction.setValueParams(params);
        assertEquals(PSP_ID_VALUE, evFunction.evaluate());
    }

    @Test
    public void paramsNotSet() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //nebyly zadány parametry
        }
    }

    @Test
    public void missingParam() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //chybí parametr file_id
        }
    }

    @Test
    public void duplicateParam() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("string_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, PSP_ID_STRINGID));
        params.addParam("string_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, "XYZ_DIR"));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr file_id musí být jen jeden
        }
    }


    @Test
    public void invalidParamType() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("string_id", new EvaluationFunction.ValueParamConstant(ValueType.FILE, PSP_ID_STRINGID));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr string_id není očekávaného typu FILE
        }
    }
}
