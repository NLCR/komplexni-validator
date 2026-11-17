package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EfIsFileListNonEmptyTest {

    private static final String FUNCTION_NAME = "isFileListNonEmpty";
    private static final String PARAM_FILE_LIST = "files";

    private static final List<File> NON_EMPTY = Collections.singletonList(new File("/first"));
    private static final List<File> EMPTY = new ArrayList<>();

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }

    @Test
    public void nonEmptyListIsTrue() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(NON_EMPTY))
                .evaluate();
        assertEquals(true, evaluation.getData());
        assertNull(evaluation.getErrorMessage());
    }

    @Test
    public void emptyListIsFalse() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(EMPTY))
                .evaluate();
        assertEquals(false, evaluation.getData());
        assertNull(evaluation.getErrorMessage());
    }

    @Test
    public void nullListIsTakenAsEmpty() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(null))
                .evaluate();
        assertEquals(false, evaluation.getData());
        assertNull(evaluation.getErrorMessage());
    }

    @Test
    public void oneNonEmptyListAmongMoreIsEnough() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(EMPTY))
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(null))
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(NON_EMPTY))
                .evaluate();
        assertEquals(true, evaluation.getData());
    }

    @Test
    public void allListsEmptyIsFalse() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(EMPTY))
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, new ValueEvaluation(EMPTY))
                .evaluate();
        assertEquals(false, evaluation.getData());
    }

    @Test
    public void paramMissing() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME).evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }

    @Test
    public void paramInvalidType() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE, new ValueEvaluation(NON_EMPTY))
                .evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }
}
