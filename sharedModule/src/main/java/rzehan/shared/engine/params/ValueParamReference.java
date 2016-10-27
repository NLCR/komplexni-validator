package rzehan.shared.engine.params;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

/**
 * Created by martin on 24.10.16.
 */
public class ValueParamReference extends ValueParam {
    protected final Engine engine;
    private final String varName;

    public ValueParamReference(Engine engine, ValueType type, String varName) {
        super(type);
        this.engine = engine;
        this.varName = varName;
    }

    @Override
    public Object getValue() {
        Object value = engine.getValueFromVariable(varName);
        if (value == null) {
            throw new RuntimeException("proměnná " + varName + " není definována");
        }
        return value;
    }
}
