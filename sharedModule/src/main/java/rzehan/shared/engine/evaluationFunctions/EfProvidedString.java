package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 20.10.16.
 */
public class EfProvidedString extends EvaluationFunction {

    private static final String PARAM_STRING_ID = "string_id";


    public EfProvidedString(Engine engine) {
        super(engine, ValueType.STRING);
    }

    @Override
    public String evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("Nebyly vlozeny parametry");
        }
        String stringId = getStringIdFromParams();
        if (stringId == "PSP_ID") {
            return "b50eb6b0-f0a4-11e3-b72e-005056827e52";
        } else {
            throw new RuntimeException("řetězec s id " + stringId + " není poskytován");
        }
    }


    public String getStringIdFromParams() {
        List<ValueParam> varNameValues = valueParams.getParams(PARAM_STRING_ID);
        if (varNameValues == null || varNameValues.size() == 0) {
            throw new RuntimeException("chybí parametr " + PARAM_STRING_ID);
        } else if (varNameValues.size() > 1) {
            throw new RuntimeException("parametr " + PARAM_STRING_ID + " musí být jen jeden");
        }
        ValueParam param = varNameValues.get(0);
        //todo: kontrola typu
        return (String) param.getValue();
    }
}
