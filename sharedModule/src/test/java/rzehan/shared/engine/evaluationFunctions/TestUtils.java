package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
 */
public class TestUtils {

    public static void defineProvidedIntegerVar(Engine engine, String varName) {
        engine.registerValueDefinition(varName,
                engine.buildValueDefinition(ValueType.INTEGER,
                        engine.buildEvaluationFunction("getProvidedInteger")
                                .withValueParam("int_id", ValueType.STRING, new ValueEvaluation(varName))

                )
        );
    }

    public static void defineProvidedStringVar(Engine engine, String varName) {
        engine.registerValueDefinition(varName,
                engine.buildValueDefinition(ValueType.STRING,
                        engine.buildEvaluationFunction("getProvidedString")
                                .withValueParam("string_id", ValueType.STRING, new ValueEvaluation(varName))

                )
        );
    }

    public static void defineProvidedFileVar(Engine engine, String varName) {
        engine.registerValueDefinition(varName,
                engine.buildValueDefinition(ValueType.FILE,
                        engine.buildEvaluationFunction("getProvidedFile")
                                .withValueParam("file_id", ValueType.STRING, new ValueEvaluation(varName))

                )
        );
    }


}
