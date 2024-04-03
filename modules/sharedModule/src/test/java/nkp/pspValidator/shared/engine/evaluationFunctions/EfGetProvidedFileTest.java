package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfGetProvidedFileTest {

    private static final String FUNCTION_NAME = "getProvidedFile";
    private static final String PARAM_NAME = "file_id";

    private static Engine engine;
    private static String PSP_DIR_FILEID = "PSP_DIR";
    private static File PSP_DIR_FILE = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");


    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
        engine.setProvidedFile(PSP_DIR_FILEID, PSP_DIR_FILE);
    }


    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(PSP_DIR_FILEID));
        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(PSP_DIR_FILE, evaluation.getData());
    }

    @Test
    public void missingParam() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME);
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }

    @Test
    public void duplicateParam() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(PSP_DIR_FILEID))
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation("XYZ_DIR"));
        //TODO: to by melo byt v ramci kontroly kotraktu, tj. zadna vyjimka
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
                .withValueParam(PARAM_NAME, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILEID));
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }
}
