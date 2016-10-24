package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ProvidedVarsManagerImpl;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by martin on 24.10.16.
 */
public class EfFindFilesInDirByPatternTest {

    private static final String FUNCTION_NAME = "FIND_FILES_IN_DIR_BY_PATTERN";
    private static final String PARAM_DIR = "dir";

    private static File PSP_DIR_FILE = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static String PSP_VAR = "PSP_DIR";

    private static Engine engine;


    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addFile(PSP_VAR, PSP_DIR_FILE);
        engine = new Engine(pvMgr);
        //
        TestUtils.defineProvidedFileVar(engine, PSP_VAR);
    }


    @Test
    public void paramDirFromConstantOk() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_DIR, new ValueParamConstant(ValueType.FILE, PSP_DIR_FILE));
        evFunction.setValueParams(params);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(8, files.size());
    }


    @Test
    public void paramDirFromReferenceOk() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_DIR, new ValueParamReference(engine, ValueType.FILE, PSP_VAR));
        evFunction.setValueParams(params);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(8, files.size());
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
    public void paramDirMissing() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //chybí parametr ...
        }
    }

    @Test
    public void paramDirDuplicate() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_DIR, new ValueParamConstant(ValueType.FILE, PSP_DIR_FILE));
        params.addParam(PARAM_DIR, new ValueParamReference(engine, ValueType.FILE, PSP_VAR));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr ... musí být jen jeden
        }
    }


    @Test
    public void paramDirFromConstantInvalidParamType() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_DIR, new ValueParamConstant(ValueType.STRING, PSP_DIR_FILE));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }

    @Test
    public void paramDirFromReferenceInvalidParamType() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_DIR, new ValueParamReference(engine, ValueType.STRING, PSP_VAR));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr file_id není očekávaného typu STRING
        }
    }

    //TODO: pattern
    //TODO: test exceptions: file is not dir, dir does not exist, dir cannot be read, etc

}
