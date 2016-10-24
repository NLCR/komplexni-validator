package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
 */
public final class ValueParamConstant extends ValueParam {
    //hodnota, treba i xpath
    private final Object value;

    public ValueParamConstant(ValueType type, Object value) {
        super(type);
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
