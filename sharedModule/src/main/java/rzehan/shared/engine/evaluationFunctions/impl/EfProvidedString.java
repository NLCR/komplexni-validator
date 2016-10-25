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
        super(engine, new Contract()
                .withReturnType(ValueType.STRING)
                .withValueParam(PARAM_STRING_ID, ValueType.STRING, 1, 1));
    }

    @Override
    public String evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("nebyly zadány parametry");
        }
        contract.checkComplience(valueParams, null);

        String stringId = (String) valueParams.getParams(PARAM_STRING_ID).get(0).getValue();
        String string = engine.getProvidedVarsManager().getProvidedString(stringId);
        if (string == null) {
            throw new RuntimeException("řetězec s id " + stringId + " není poskytován");
        } else {
            return string;
        }
    }

}
