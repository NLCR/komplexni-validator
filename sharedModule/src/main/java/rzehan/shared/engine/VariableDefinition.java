package rzehan.shared.engine;

import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 24.10.16.
 */
public class VariableDefinition {

    //only for checking if matches result type of evaluation function
    private final ValueType resultType;

    private final EvaluationFunction evaluationFunction;
    private final EvaluationFunction.ValueParams efValueParams;


    public VariableDefinition(ValueType resultType, EvaluationFunction evaluationFunction, EvaluationFunction.ValueParams efValueParams) {
        this.resultType = resultType;
        this.evaluationFunction = evaluationFunction;
        this.efValueParams = efValueParams;
        if (evaluationFunction.getResultType() != resultType) {
            throw new RuntimeException(String.format("nesouhlasí typ výsledku pro definici proměnné (%s) a vyhodnocovací funkci (%s)", resultType, evaluationFunction.getResultType()));
        }
    }

    public Object evaluate() {
        evaluationFunction.setValueParams(efValueParams);
        return evaluationFunction.evaluate();
    }
}
