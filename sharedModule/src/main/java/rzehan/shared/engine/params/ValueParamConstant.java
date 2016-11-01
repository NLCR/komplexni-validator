package rzehan.shared.engine.params;

import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
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
