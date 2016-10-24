package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
 */
public class TestUtils {

    public static void defineProvidedIntegerVar(Engine engine, String varName) {
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("int_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, varName));
        engine.defineVariable(varName, ValueType.INTEGER, "PROVIDED_INTEGER", params);
    }

    public static void defineProvidedStringVar(Engine engine, String varName) {
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("string_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, varName));
        engine.defineVariable(varName, ValueType.STRING, "PROVIDED_STRING", params);
    }

    public static void defineProvidedFileVar(Engine engine, String varName) {
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("file_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, varName));
        engine.defineVariable(varName, ValueType.FILE, "PROVIDED_FILE", params);
    }


}
