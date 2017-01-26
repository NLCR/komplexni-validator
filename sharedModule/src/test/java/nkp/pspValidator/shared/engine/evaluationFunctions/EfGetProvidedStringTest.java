package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfGetProvidedStringTest {

    private static final String FUNCTION_NAME = "getProvidedString";
    private static final String PARAM_NAME = "string_id";

    private static Engine engine;

    private static String PSP_ID_STRINGID = "PSP_ID";
    private static String PSP_ID_VALUE = "b50eb6b0-f0a4-11e3-b72e-005056827e52";


    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
        engine.setProvidedString(PSP_ID_STRINGID, PSP_ID_VALUE);
    }

    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(PSP_ID_STRINGID));
        assertEquals(PSP_ID_VALUE, evFunction.evaluate().getData());
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
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(PSP_ID_STRINGID))
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation("PSPID"));
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
                .withValueParam(PARAM_NAME, ValueType.FILE, new ValueEvaluation(PSP_ID_STRINGID));
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }
}
