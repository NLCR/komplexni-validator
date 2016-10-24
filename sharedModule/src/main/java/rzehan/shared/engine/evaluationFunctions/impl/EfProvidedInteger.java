package rzehan.shared.engine.evaluationFunctions.impl;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.evaluationFunctions.ValueParam;

import java.util.List;

/**
 * Created by martin on 20.10.16.
 */
public class EfProvidedInteger extends EvaluationFunction {

    private static final String PARAM_INT_ID = "int_id";


    public EfProvidedInteger(Engine engine) {
        super(engine, ValueType.INTEGER);
    }

    @Override
    public Integer evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("nebyly zadány parametry");
        }
        String intId = getStringIdFromParams();
        Integer value = engine.getProvidedVarsManager().getProvidedInteger(intId);
        if (value == null) {
            throw new RuntimeException("číslo s id " + intId + " není poskytováno");
        } else {
            return value;
        }
    }


    public String getStringIdFromParams() {
        List<ValueParam> varNameValues = valueParams.getParams(PARAM_INT_ID);
        if (varNameValues == null || varNameValues.size() == 0) {
            throw new RuntimeException("chybí parametr " + PARAM_INT_ID);
        } else if (varNameValues.size() > 1) {
            throw new RuntimeException("parametr " + PARAM_INT_ID + " musí být jen jeden");
        }
        ValueParam param = varNameValues.get(0);
        //kontrola typu
        if (param.getType() != ValueType.STRING) {
            throw new RuntimeException(String.format("parametr %s není očekávaného typu %s", PARAM_INT_ID, ValueType.STRING.toString()));
        }
        return (String) param.getValue();
    }
}
