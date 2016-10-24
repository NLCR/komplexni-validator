package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
 */
public abstract class ValueParam {

    private final ValueType type;

    protected ValueParam(ValueType type) {
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public abstract Object getValue();

}
