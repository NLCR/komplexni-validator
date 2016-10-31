package rzehan.shared.engine;

import org.junit.BeforeClass;
import org.junit.Test;
import rzehan.shared.engine.validationFunctions.ValidationFunction;
import rzehan.shared.engine.validationFunctions.ValidationResult;

import java.io.File;

import static junit.framework.TestCase.assertFalse;
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
                                .withValueParam("file_id", ValueType.STRING, "PSP_DIR")
                ));
        //this one is provided just temporarily
        engine.registerValueDefinition("PSP_ID",
                engine.buildValueDefinition(ValueType.STRING,
                        engine.buildEvaluationFunction(EF_PROVIDED_STRING)
                                .withValueParam("string_id", ValueType.STRING, "PSP_ID")
                ));


        //other VARS
        engine.registerValueDefinition("INFO_FILES",
                engine.buildValueDefinition(ValueType.FILE_LIST,
                        engine.buildEvaluationFunction(EF_FIND_FILES_IN_DIR_BY_PATTERN)
                                .withValueParam("dir", ValueType.FILE, engine.getValueFromVariable("PSP_DIR"))
                                .withPatternParamByReference("pattern", "INFO_FILENAME")
                )
        );

        //patterns
        engine.registerPattern("INFO_FILENAME",
                engine.buildPattern(
                        engine.buildExpression(true, "info_${PSP_ID}\\.xml"),
                        engine.buildExpression(true, "info.xml")
                )
        );


        //validation functions directly
        ValidationFunction singleInfoFileVf = engine.buildValidationFunction("CHECK_FILE_LIST_EXACT_SIZE")
                .withValueParamByReference("files", ValueType.FILE_LIST, "INFO_FILES")
                .withValueParam("size", ValueType.INTEGER, 1);
        ValidationResult singleInfoFileResult = singleInfoFileVf.validate();
        assertTrue(singleInfoFileResult.getMessage(), singleInfoFileResult.isValid());

        //through validation rule
        Rule ruleSingleInfo =
                engine.buildRule("SINGLE_INFO", Rule.Level.ERROR,
                        engine.buildValidationFunction("CHECK_FILE_LIST_EXACT_SIZE")
                                .withValueParamByReference("files", ValueType.FILE_LIST, "INFO_FILES")
                                .withValueParam("size", ValueType.INTEGER, 1))
                        .setDescription("musi existovat prave jeden soubor info");
        ValidationResult ruleSingleInfoResult = ruleSingleInfo.getResult();
        assertTrue(ruleSingleInfoResult.getMessage(), ruleSingleInfoResult.isValid());

        //through validation rule
        Rule ruleTwoInfos =
                engine.buildRule("SINGLE_INFO", Rule.Level.ERROR,
                        engine.buildValidationFunction("CHECK_FILE_LIST_EXACT_SIZE")
                                .withValueParamByReference("files", ValueType.FILE_LIST, "INFO_FILES")
                                .withValueParam("size", ValueType.INTEGER, 2))
                        .setDescription("dva soubory info");
        ValidationResult ruleTwoInfosResult = ruleTwoInfos.getResult();
        assertFalse(ruleTwoInfosResult.getMessage(), ruleTwoInfosResult.isValid());
    }


}

