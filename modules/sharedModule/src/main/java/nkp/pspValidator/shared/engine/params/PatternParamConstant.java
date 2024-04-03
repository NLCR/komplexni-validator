package nkp.pspValidator.shared.engine.params;

import nkp.pspValidator.shared.engine.PatternEvaluation;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public class PatternParamConstant extends PatternParam {

    private final PatternEvaluation evaluation;

    public PatternParamConstant(PatternEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    @Override
    public PatternEvaluation getEvaluation() {
        return evaluation;
    }

    @Override
    public String toString() {
        return evaluation.toString();
    }


}
