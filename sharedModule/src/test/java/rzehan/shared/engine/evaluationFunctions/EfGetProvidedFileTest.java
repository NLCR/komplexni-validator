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
public class EfGetProvidedFileTest {

    private static final String FUNCTION_NAME = "getProvidedFile";
    private static final String PARAM_NAME = "file_id";

    private static Engine engine;
    private static String PSP_DIR_FILEID = "PSP_DIR";
    private static File PSP_DIR_FILE = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");


    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addFile(PSP_DIR_FILEID, PSP_DIR_FILE);
        engine = new Engine(pvMgr);
    }


    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, PSP_DIR_FILEID);
        assertEquals(PSP_DIR_FILE, evFunction.evaluate());
    }

    @Test
    public void missingParam() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //chybí parametr ...
        }
    }

    @Test
    public void duplicateParam() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, PSP_DIR_FILEID)
                .withValueParam(PARAM_NAME, ValueType.STRING, "XYZ_DIR");
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr ... musí být jen jeden
        }
    }


    @Test
    public void invalidParamType() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.FILE, PSP_DIR_FILEID);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }
}