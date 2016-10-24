package rzehan.shared.engine.evaluationFunctions.impl;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.evaluationFunctions.ValueParam;

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
            throw new IllegalStateException("nebyly zadány parametry");
        }
        String stringId = getStringIdFromParams();
        String string = engine.getProvidedVarsManager().getProvidedString(stringId);
        if (string == null) {
            throw new RuntimeException("řetězec s id " + stringId + " není poskytován");
        } else {
            return string;
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
        //kontrola typu
        if (param.getType() != ValueType.STRING) {
            throw new RuntimeException(String.format("parametr %s není očekávaného typu %s", PARAM_STRING_ID, ValueType.STRING.toString()));
        }
        return (String) param.getValue();
    }
}
