package nkp.pspValidator.shared.engine.exceptions;

import nkp.pspValidator.shared.engine.ValueEvaluation;

/**
 * Created by Martin Řehánek on 1.11.16.
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
