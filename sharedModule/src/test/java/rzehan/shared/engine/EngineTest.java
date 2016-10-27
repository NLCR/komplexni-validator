package rzehan.shared.engine;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.evaluationFunctions.*;
import rzehan.shared.engine.params.ValueParamConstant;
import rzehan.shared.engine.validationFunctions.ValidationFunction;
import rzehan.shared.engine.validationFunctions.ValidationResult;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Created by martin on 27.10.16.
 */
public class EngineTest {

    private static final String EF_PROVIDED_STRING = "PROVIDED_STRING";
    private static final String EF_PROVIDED_FILE = "PROVIDED_FILE";
    private static final String EF_PROVIDED_INTEGER = "PROVIDED_INTEGER";
    private static final String EF_RETURN_FIRST_FILE_FROM_LIST = "RETURN_FIRST_FILE_FROM_LIST";
    private static final String EF_FIND_FILES_IN_DIR_BY_PATTERN = "FIND_FILES_IN_DIR_BY_PATTERN";

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addFile("PSP_DIR", new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52"));
        pvMgr.addString("PSP_ID", "b50eb6b0-f0a4-11e3-b72e-005056827e52");
        engine = new Engine(pvMgr);
    }

    @Test
    public void testAll() {
        //provided VARS
        engine.registerValueDefinition("PSP_DIR",
                engine.buildValueDefinition(ValueType.FILE,
                        engine.buildEvaluationFunction(EF_PROVIDED_FILE)
                                .withValue("file_id", ValueType.STRING, "PSP_DIR")
                ));
        //this one is provided just temporarily
        engine.registerValueDefinition("PSP_ID",
                engine.buildValueDefinition(ValueType.STRING,
                        engine.buildEvaluationFunction(EF_PROVIDED_STRING)
                                .withValue("string_id", ValueType.STRING, "PSP_ID")
                ));


        //other VARS
        engine.registerPattern("INFO_FILENAME",
                engine.buildPattern(
                        engine.buildExpression(true, "info_${PSP_ID}\\.xml"),
                        engine.buildExpression(true, "info.xml")
                )
        );

        engine.registerValueDefinition("INFO_FILES",
                engine.buildValueDefinition(ValueType.LIST_OF_FILES,
                        engine.buildEvaluationFunction(EF_FIND_FILES_IN_DIR_BY_PATTERN)
                                .withValue("dir", ValueType.FILE, engine.getValueFromVariable("PSP_DIR"))
                                .withPattern("pattern", engine.getPatternFromVariable("INFO_FILENAME"))
                )
        );


        ValidationFunction singleInfoFile = engine.buildValidationFunction("CHECK_FILE_LIST_EXACT_SIZE")
                .withValueReference("list", ValueType.LIST_OF_FILES, "INFO_FILES")
                .withValue("size", ValueType.INTEGER, 1);
        ValidationResult result = singleInfoFile.validate();

        assertTrue(result.getMessage(), result.isValid());


    }


}

