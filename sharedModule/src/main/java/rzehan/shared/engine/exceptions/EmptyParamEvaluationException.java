package rzehan.shared.engine.exceptions;

import rzehan.shared.engine.ValueEvaluation;

/**
 * Created by martin on 1.11.16.
 */
public class EmptyParamEvaluationException extends Exception {

    private final String paramName;
    private final ValueEvaluation evaluation;

    public EmptyParamEvaluationException(String paramName, ValueEvaluation evaluation) {
        this.paramName = paramName;
        this.evaluation = evaluation;
    }

    public String getParamName() {
        return paramName;
    }

    public ValueEvaluation getEvaluation() {
        return evaluation;
    }
}
