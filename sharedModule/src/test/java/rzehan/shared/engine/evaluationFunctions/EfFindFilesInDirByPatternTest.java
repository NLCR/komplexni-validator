package rzehan.shared.engine.evaluationFunctions;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Pattern;
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
    private static final String PARAM_PATTERN = "pattern";

    private static String PSP_VAR = "PSP_DIR";
    private static File PSP_DIR_FILE = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static File PSP_DIR_FILE_UNKNOWN = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/unknown");
    private static File PSP_DIR_FILE_NOT_DIR = new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml");

    private static String PATTERN_CONTAINS_UNDERSCORE_VARIABLE = "STARTS_WITH_A";

    private static Engine engine;
    private static Pattern patternAllFiles;
    private static Pattern patternXmlFiles;


    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addFile(PSP_VAR, PSP_DIR_FILE);
        engine = new Engine(pvMgr);

        TestUtils.defineProvidedFileVar(engine, PSP_VAR);
        patternAllFiles = engine.buildPattern(engine.buildExpression(false, ".+"));
        patternXmlFiles = engine.buildPattern(engine.buildExpression(false, ".+\\.xml"));
        //define pattern for referencing
        Pattern containsUnderscore = engine.buildPattern(engine.buildExpression(true, ".*_.*"));
        engine.registerPattern(PATTERN_CONTAINS_UNDERSCORE_VARIABLE, containsUnderscore);
    }


    @Test
    public void paramDirFromConstantOk() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(8, files.size());
    }


    @Test
    public void paramDirFromReferenceOk() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueReference(PARAM_DIR, ValueType.FILE, PSP_VAR)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(8, files.size());
    }

    @Test
    public void paramDirMissing() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //chybí parametr ...
        }
    }

    @Test
    public void paramPatternMissing() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueReference(PARAM_DIR, ValueType.FILE, PSP_VAR);
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
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE)
                .withValueReference(PARAM_DIR, ValueType.FILE, PSP_VAR);
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
                .withValue(PARAM_DIR, ValueType.STRING, PSP_DIR_FILE)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr ... není očekávaného typu ...
        }
    }

    @Test
    public void paramDirFromReferenceInvalidParamType() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueReference(PARAM_DIR, ValueType.STRING, PSP_VAR)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr file_id není očekávaného typu STRING
        }
    }

    @Test
    public void paramDirFromConstantDirDoesNotExist() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE_UNKNOWN)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //soubor ... neexistuje
        }
    }

    @Test
    public void paramDirFromConstantDirDoesNotDir() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE_NOT_DIR)
                .withPattern(PARAM_PATTERN, patternAllFiles);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //soubor ... není adresář
        }
    }


    //TODO: test exception: dir cannot be read


    @Test
    public void paramDirFromConstantPatternXmlFiles() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE)
                .withPattern(PARAM_PATTERN, patternXmlFiles);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(2, files.size());
    }


    @Test
    public void patternFromReference() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE)
                .withPatternReference(PARAM_PATTERN, PATTERN_CONTAINS_UNDERSCORE_VARIABLE);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(3, files.size());
    }


    //TODO: presunout, tohle je na vyhodnocovani obecne, nejspis do tridy Engine, az to uklidim
    @Test
    public void valueFromReferenceNotDefined() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueReference(PARAM_DIR, ValueType.FILE, "UKNOWN")
                .withPattern(PARAM_PATTERN, patternAllFiles);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //proměnná ... není definována
        }
    }

    //TODO: presunout, tohle je na vyhodnocovani obecne, nejspis do tridy Engine, az to uklidim
    @Test
    public void patternFromReferenceNotDefined() {
        EvaluationFunction evFunction = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValue(PARAM_DIR, ValueType.FILE, PSP_DIR_FILE)
                .withPatternReference(PARAM_PATTERN, "UKNOWN");
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //vzor ... není definován
        }
    }


    //TODO: exceptions when referenced var or pattern are not defined

}
