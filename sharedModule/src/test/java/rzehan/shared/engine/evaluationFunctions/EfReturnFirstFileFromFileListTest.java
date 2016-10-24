package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ProvidedVarsManagerImpl;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by martin on 21.10.16.
 */
public class EfReturnFirstFileFromFileListTest {


    private static final String FUNCTION_NAME = "RETURN_FIRST_FILE_FROM_LIST";
    private static final String PARAM_FILE_LIST = "file_list";


    private static List<File> LIST = new ArrayList<File>() {
        {
            add(new File("/first"));
            add(new File("/second"));
            add(new File("/third"));
        }
    };

    private static String LIST_VAR = "LIST";

    private static Engine engine;


    @BeforeClass
    public static void setup() {
        engine = new Engine(new ProvidedVarsManagerImpl());
        defineListVar();
    }

    private static void defineListVar() {
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        File dirFile = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        //first file is somehow "txt"
        params.addParam("dir", new EvaluationFunction.ValueParamConstant(ValueType.FILE, dirFile));
        engine.defineVariable(LIST_VAR, ValueType.LIST_OF_FILES, "FIND_FILES_IN_DIR_BY_PATTERN", params);
    }


    @Test
    public void listFromConstantOk() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamConstant(ValueType.LIST_OF_FILES, LIST));
        evFunction.setValueParams(params);
        assertEquals(LIST.get(0), evFunction.evaluate());
    }

    @Test
    public void listFromReferenceOk() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamReference(engine, ValueType.LIST_OF_FILES, LIST_VAR));
        evFunction.setValueParams(params);
        assertEquals("txt", ((File) evFunction.evaluate()).getName());
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
        params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamConstant(ValueType.LIST_OF_FILES, LIST));
        //todo: druhy odkazem na promennou
        params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamConstant(ValueType.LIST_OF_FILES, LIST));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr ... musí být jen jeden
        }
    }


    @Test
    public void paramListFromConstantInvalidParamType() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamConstant(ValueType.FILE, LIST));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }

    @Test
    public void paramListFromReferenceInvalidParamType() {
        EvaluationFunction evFunction = engine.getEvaluationFunction(FUNCTION_NAME);
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        //params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamConstant(ValueType.FILE, LIST));
        params.addParam(PARAM_FILE_LIST, new EvaluationFunction.ValueParamReference(engine, ValueType.FILE, LIST_VAR));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }

}
