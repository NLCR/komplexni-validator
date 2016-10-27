package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

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
        checkContractCompliance();

        String stringId = (String) valueParams.getParams(PARAM_STRING_ID).get(0).getValue();
        String string = engine.getProvidedVarsManager().getProvidedString(stringId);
        if (string == null) {
            throw new RuntimeException("řetězec s id " + stringId + " není poskytován");
        } else {
            return string;
        }
    }

    @Override
    public String getName() {
        return "PROVIDED_STRING";
    }

}
