package nkp.pspValidator.shared.engine.params;

import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public abstract class ValueParam {

    private final ValueType type;

    protected ValueParam(ValueType type) {
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public abstract ValueEvaluation getEvaluation();

}
