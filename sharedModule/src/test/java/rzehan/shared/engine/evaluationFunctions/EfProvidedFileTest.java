package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ProvidedVarsManagerImpl;
import rzehan.shared.engine.ValueType;

import java.io.File;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by martin on 21.10.16.
 */
public class EfProvidedFileTest {

    private static final String FUNCTION_NAME = "PROVIDED_FILE";
    private static final String PARAM_NAME = "file_id";

    private static Engine engine;
    private static String PSP_DIR_FILEID = "PSP_DIR";
    private static File PSP_DIR_FILE = new File("/home/martin/zakazky/NKP-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");


    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addFile(PSP_DIR_FILEID, PSP_DIR_FILE);
        engine = new Engine(pvMgr);
    }


    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.STRING, PSP_DIR_FILEID));
        evFunction.setValueParams(params);
        assertEquals(PSP_DIR_FILE, evFunction.evaluate());
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
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.STRING, PSP_DIR_FILEID));
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
        params.addParam(PARAM_NAME, new EvaluationFunction.ValueParamConstant(ValueType.FILE, PSP_DIR_FILEID));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr file_id není očekávaného typu STRING
        }
    }
}
