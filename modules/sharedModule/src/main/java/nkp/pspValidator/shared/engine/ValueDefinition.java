package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.evaluationFunctions.EvaluationFunction;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public class ValueDefinition {

    private final ValueType resultType;
    private final EvaluationFunction evaluationFunction;


    public ValueDefinition(ValueType resultType, EvaluationFunction evaluationFunction) {
        this.resultType = resultType;
        this.evaluationFunction = evaluationFunction;
        if (evaluationFunction.getResultType() != resultType) {
            throw new RuntimeException(String.format("nesouhlasí typ výsledku pro definici hodnoty (%s) a vyhodnocovací funkci (%s)", resultType, evaluationFunction.getResultType()));
        }
    }


    public ValueEvaluation evaluate() {
        return evaluationFunction.evaluate();
    }

}
