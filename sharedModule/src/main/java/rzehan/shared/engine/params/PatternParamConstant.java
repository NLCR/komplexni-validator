package rzehan.shared.engine.params;

import rzehan.shared.engine.Pattern;

/**
 * Created by martin on 24.10.16.
 */
public class PatternParamConstant extends PatternParam {

    private final Pattern pattern;

    public PatternParamConstant(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String value) {
        return pattern.matches(value);
    }
}
