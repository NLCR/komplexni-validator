package rzehan.shared.engine.params;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.PatternEvaluation;

/**
 * Created by martin on 24.10.16.
 */
public class PatternParamReference extends PatternParam {
    protected final Engine engine;
    private final String varName;

    public PatternParamReference(Engine engine, String varName) {
        this.engine = engine;
        this.varName = varName;
    }

    @Override
    public PatternEvaluation getEvaluation() {
        return engine.getPatternEvaluationByVariable(varName);
    }

    @Override
    public String toString() {
        return engine.getPatternEvaluationByVariable(varName).toString();
    }

}
