package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EfGetProvidedFilesTest {

    private static final String FUNCTION_NAME = "getProvidedFiles";
    private static final String PARAM_NAME = "files_id";
    private static final String FILES_ID = "ALTO_XSD_FILES";
    private static final List<File> FILES = Arrays.asList(new File("xsd/alto_2.0.xsd"), new File("xsd/alto_4.4.xsd"));

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
        engine.setProvidedFileList(FILES_ID, FILES);
    }

    @Test
    public void ok() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation(FILES_ID));
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getErrorMessage());
        assertEquals(FILES, evaluation.getData());
    }

    @Test
    public void unknownId() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_NAME, ValueType.STRING, new ValueEvaluation("XYZ_FILES"));
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
}
