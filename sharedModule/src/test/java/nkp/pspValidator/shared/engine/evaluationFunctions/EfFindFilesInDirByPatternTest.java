package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.PatternDefinition;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public class EfFindFilesInDirByPatternTest {

    private static final String FUNCTION_NAME = "findFilesInDirByPattern";
    private static final String PARAM_DIR = "dir";
    private static final String PARAM_PATTERN = "pattern";

    private static String PSP_VAR = "PSP_DIR";
    private static File PSP_DIR_FILE = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static File PSP_DIR_FILE_UNKNOWN = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/unknown");
    private static File PSP_DIR_FILE_NOT_DIR = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml");

    private static String PATTERN_CONTAINS_UNDERSCORE_VARIABLE = "STARTS_WITH_A";

    private static Engine engine;
    private static PatternDefinition patternAllFiles;
    private static PatternDefinition patternXmlFiles;


    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
        engine.setProvidedFile(PSP_VAR, PSP_DIR_FILE);

        TestUtils.defineProvidedFileVar(engine, PSP_VAR);
        patternAllFiles = engine.buildPatternDefinition()
                .withRawExpression(engine.buildRawPatternExpression(false, ".+"));
        patternXmlFiles = engine.buildPatternDefinition()
                .withRawExpression(engine.buildRawPatternExpression(false, ".+\\.xml"));

        //define and register pattern for referencing
        PatternDefinition containsUnderscore =
                engine.buildPatternDefinition()
                        .withRawExpression(engine.buildRawPatternExpression(true, ".*_.*"));
        engine.registerPatternDefinition(PATTERN_CONTAINS_UNDERSCORE_VARIABLE, containsUnderscore);
    }


    @Test
    public void paramDirFromConstantOk() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE))
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNotNull(evaluation.getData());
        List<File> files = (List<File>) evaluation.getData();
        assertEquals(8, files.size());
    }


    @Test
    public void paramDirFromReferenceOk() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParamByReference(PARAM_DIR, ValueType.FILE, PSP_VAR)
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());

        ValueEvaluation evaluation = evFunction.evaluate();
        assertNotNull(evaluation.getData());
        List<File> files = (List<File>) evaluation.getData();
        assertEquals(8, files.size());
    }

    @Test
    public void paramDirMissing() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());
        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(null, evaluation.getData());
    }

    @Test
    public void paramPatternMissing() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParamByReference(PARAM_DIR, ValueType.FILE, PSP_VAR);

        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(null, evaluation.getData());
    }

    @Test
    public void paramDirDuplicate() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE))
                .withValueParamByReference(PARAM_DIR, ValueType.FILE, PSP_VAR);
        //TODO: to by melo byt v ramci kontroly kotraktu, tj. zadna vyjimka
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr ... musí být jen jeden
        }
    }


    @Test
    public void paramDirFromConstantInvalidParamType() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.STRING, new ValueEvaluation(PSP_DIR_FILE))
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());
        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(null, evaluation.getData());
    }

    @Test
    public void paramDirFromReferenceInvalidParamType() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParamByReference(PARAM_DIR, ValueType.STRING, PSP_VAR)
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());
        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(null, evaluation.getData());
    }

    @Test
    public void paramDirFromConstantDirDoesNotExist() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE_UNKNOWN))
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());
        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(null, evaluation.getData());
    }

    @Test
    public void paramDirFromConstantDirDoesNotDir() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE_NOT_DIR))
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());

        ValueEvaluation evaluation = evFunction.evaluate();
        assertEquals(null, evaluation.getData());
    }


    //TODO: test exception: dir cannot be read


    @Test
    public void paramDirFromConstantPatternXmlFiles() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE))
                .withPatternParam(PARAM_PATTERN, patternXmlFiles.evaluate());

        List<File> files = (List<File>) evFunction.evaluate().getData();
        assertEquals(2, files.size());
    }


    @Test
    public void patternFromReference() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE))
                .withPatternParamByReference(PARAM_PATTERN, PATTERN_CONTAINS_UNDERSCORE_VARIABLE);

        ValueEvaluation evaluation = evFunction.evaluate();
        assertNotNull(evaluation.getData());
        List<File> files = (List<File>) evaluation.getData();
        assertEquals(3, files.size());
    }


    //TODO: presunout, tohle je na vyhodnocovani obecne, nejspis do tridy Engine, az to uklidim
    @Test
    public void valueFromReferenceNotDefined() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParamByReference(PARAM_DIR, ValueType.FILE, "UKNOWN")
                .withPatternParam(PARAM_PATTERN, patternAllFiles.evaluate());
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }

    //TODO: presunout, tohle je na vyhodnocovani obecne, nejspis do tridy Engine, az to uklidim
    @Test
    public void patternFromReferenceNotDefined() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_DIR, ValueType.FILE, new ValueEvaluation(PSP_DIR_FILE))
                .withPatternParamByReference(PARAM_PATTERN, "UKNOWN");
        ValueEvaluation evaluation = evFunction.evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }


    //TODO: exceptions when referenced var or pattern are not defined

}
