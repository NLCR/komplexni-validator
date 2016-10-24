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
public class EfProvidedIntegerTest {

    private static final String FUNCTION_NAME = "PROVIDED_INTEGER";
    private static final String PARAM_NAME = "int_id";

    private static Engine engine;
    private static String YEAR_INTID = "YEAR";
    private static Integer YEAR_VALUE = 2016;


    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addInteger(YEAR_INTID, YEAR_VALUE);
        engine = new Engine(pvMgr);
    }

    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.STRING, YEAR_INTID));
        evFunction.setValueParams(params);
        assertEquals(YEAR_VALUE, evFunction.evaluate());
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
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.STRING, YEAR_INTID));
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.STRING, "XYZ_DIR"));
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
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.FILE, YEAR_INTID));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr string_id není očekávaného typu FILE
        }
    }
}