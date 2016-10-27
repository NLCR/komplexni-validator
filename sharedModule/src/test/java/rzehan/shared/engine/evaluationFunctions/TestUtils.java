package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueDefinition;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.params.ValueParamConstant;

/**
 * Created by martin on 24.10.16.
 */
public class TestUtils {

    public static void defineProvidedIntegerVar(Engine engine, String varName) {
        engine.registerValueDefinition(varName,
                engine.buildValueDefinition(ValueType.INTEGER,
                        engine.buildEvaluationFunction("PROVIDED_INTEGER")
                                .withValue("int_id", ValueType.STRING, varName)

                )
        );
    }

    public static void defineProvidedStringVar(Engine engine, String varName) {
        engine.registerValueDefinition(varName,
                engine.buildValueDefinition(ValueType.STRING,
                        engine.buildEvaluationFunction("PROVIDED_STRING")
                                .withValue("string_id", ValueType.STRING, varName)

                )
        );
    }

    public static void defineProvidedFileVar(Engine engine, String varName) {
        engine.registerValueDefinition(varName,
                engine.buildValueDefinition(ValueType.FILE,
                        engine.buildEvaluationFunction("PROVIDED_FILE")
                                .withValue("file_id", ValueType.STRING, varName)

                )
        );
    }


}
