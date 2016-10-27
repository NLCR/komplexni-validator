package rzehan.shared.engine.params;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Pattern;

/**
 * Created by martin on 24.10.16.
 */
public class PatternParamReference extends PatternParam {
    protected final Engine engine;
    private final String patternName;

    public PatternParamReference(Engine engine, String patternName) {
        this.engine = engine;
        this.patternName = patternName;
    }

    @Override
    public boolean matches(String value) {
        Pattern pattern = engine.getPatternFromVariable(patternName);
        if (pattern == null) {
            throw new RuntimeException("vzor " + patternName + " není definován");
        }
        return pattern.matches(value);
    }
}
