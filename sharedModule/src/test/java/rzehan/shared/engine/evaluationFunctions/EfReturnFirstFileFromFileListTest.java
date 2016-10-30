package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.*;

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
        engine.registerValueDefinition(LIST_VAR,
                engine.buildValueDefinition(ValueType.FILE_LIST,
                        engine.buildEvaluationFunction("FIND_FILES_IN_DIR_BY_PATTERN")
                                .withValueParam("dir", ValueType.FILE, new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52"))
                                .withPatternParam("pattern", engine.buildPattern(engine.buildExpression(false, ".+")))
                ));
    }


    @Test
    public void listFromConstantOk() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, LIST);
        assertEquals(LIST.get(0), evFunction.evaluate());
    }

    @Test
    public void listFromReferenceOk() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParamByReference(PARAM_FILE_LIST, ValueType.FILE_LIST, LIST_VAR);
        assertEquals("txt", ((File) evFunction.evaluate()).getName());
    }

    @Test
    public void paramDirMissing() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //chybí parametr ...
        }
    }

    @Test
    public void paramDirDuplicate() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, LIST)
                .withValueParamByReference(PARAM_FILE_LIST, ValueType.FILE_LIST, LIST_VAR);
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr ... musí být jen jeden
        }
    }


    @Test
    public void paramListFromConstantInvalidParamType() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE, LIST);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }

    @Test
    public void paramListFromReferenceInvalidParamType() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParamByReference(PARAM_FILE_LIST, ValueType.FILE, LIST_VAR);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }

}
