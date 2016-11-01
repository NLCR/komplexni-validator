package rzehan.shared.engine.params;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Pattern;
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

   /* @Override
    public boolean matches(String value) {
        Pattern pattern = engine.getPatternFromVariable(varName);
        if (pattern == null) {
            throw new RuntimeException("vzor " + varName + " není definován");
        }
        return pattern.matches(value);
    }*/

    @Override
    public String toString() {
       /* Pattern pattern = engine.getPatternFromVariable(varName);
        return pattern.toString();*/
        return engine.getPatternEvaluationByVariable(varName).toString();
    }

    @Override
    public PatternEvaluation getEvaluation() {
        return engine.getPatternEvaluationByVariable(varName);
    }
}
