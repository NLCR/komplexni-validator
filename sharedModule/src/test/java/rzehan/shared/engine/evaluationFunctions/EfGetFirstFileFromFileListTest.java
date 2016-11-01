package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ProvidedVarsManagerImpl;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by martin on 21.10.16.
 */
public class EfGetFirstFileFromFileListTest {


    private static final String FUNCTION_NAME = "getFirstFileFromFileList";
    private static final String PARAM_FILE_LIST = "files";


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
                        engine.buildEvaluationFunction("findFilesInDirByPattern")
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