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
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("int_id", new ValueParamConstant(ValueType.STRING, varName));
        ValueDefinition valueDefinition = engine.buildValueDefinition(ValueType.INTEGER, "PROVIDED_INTEGER", params, null);
        engine.registerValueDefinition(varName, valueDefinition);
    }

    public static void defineProvidedStringVar(Engine engine, String varName) {
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("string_id", new ValueParamConstant(ValueType.STRING, varName));
        ValueDefinition valueDefinition = engine.buildValueDefinition(ValueType.STRING, "PROVIDED_STRING", params, null);
        engine.registerValueDefinition(varName, valueDefinition);
    }

    public static void defineProvidedFileVar(Engine engine, String varName) {
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("file_id", new ValueParamConstant(ValueType.STRING, varName));
        ValueDefinition valueDefinition = engine.buildValueDefinition(ValueType.FILE, "PROVIDED_FILE", params, null);
        engine.registerValueDefinition(varName, valueDefinition);
    }


}
