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
public class EfGetProvidedIntegerTest {

    private static final String FUNCTION_NAME = "getProvidedInteger";
    private static final String PARAM_NAME = "int_id";

    private static Engine engine;
    private static String YEAR_INTID = "YEAR";
    private static Integer YEAR_VALUE = 2016;


    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
        engine.setProvidedInteger(YEAR_INTID, YEAR_VALUE);
    }

    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(YEAR_INTID));
        assertEquals(YEAR_VALUE, evFunction.evaluate().getData());
    }

    @Test
    public void paramsNotSet() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME);
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
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
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(YEAR_INTID))
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation("YEAR_2"));
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
                .withValueParam(PARAM_NAME, ValueType.FILE, new ValueEvaluation(YEAR_INTID));
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }
}
