package nkp.pspValidator.shared.engine.params;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public class ValueParamReference extends ValueParam {
    protected final Engine engine;
    private final String varName;

    public ValueParamReference(Engine engine, ValueType type, String varName) {
        super(type);
        this.engine = engine;
        this.varName = varName;
    }

    public ValueEvaluation getEvaluation() {
        return engine.getValueEvaluationByVariable(varName);
    }
}
