package rzehan.shared.engine.params;

import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
 */
public final class ValueParamConstant extends ValueParam {
    private final ValueEvaluation valueEvaluation;

    public ValueParamConstant(ValueType type, ValueEvaluation valueEvaluation) {
        super(type);
        this.valueEvaluation = valueEvaluation;
    }

    public ValueEvaluation getValueEvaluation() {
        return valueEvaluation;
    }
}
