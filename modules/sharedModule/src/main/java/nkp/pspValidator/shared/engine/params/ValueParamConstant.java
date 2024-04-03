package nkp.pspValidator.shared.engine.params;

import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public final class ValueParamConstant extends ValueParam {
    private final ValueEvaluation evaluation;

    public ValueParamConstant(ValueType type, ValueEvaluation evaluation) {
        super(type);
        this.evaluation = evaluation;
    }

    public ValueEvaluation getEvaluation() {
        return evaluation;
    }
}
