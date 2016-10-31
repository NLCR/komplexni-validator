package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 20.10.16.
 */
public class EfGetProvidedInteger extends EvaluationFunction {

    private static final String PARAM_INT_ID = "int_id";


    public EfGetProvidedInteger(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.INTEGER)
                .withValueParam(PARAM_INT_ID, ValueType.STRING, 1, 1));
    }


    @Override
    public String getName() {
        return "getProvidedInteger";
    }

    @Override
    public Integer evaluate() {
        checkContractCompliance();

        String intId = (String) valueParams.getParams(PARAM_INT_ID).get(0).getValue();
        Integer value = engine.getProvidedVarsManager().getProvidedInteger(intId);
        if (value == null) {
            throw new RuntimeException("číslo s id " + intId + " není poskytováno");
        } else {
            return value;
        }
    }


}
