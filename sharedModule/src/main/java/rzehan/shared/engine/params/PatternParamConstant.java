package rzehan.shared.engine.params;

import rzehan.shared.engine.Pattern;
import rzehan.shared.engine.exceptions.TemporaryValidatorException;

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
        if (value == null) {
            throw new TemporaryValidatorException("value is null");
        } else if (pattern == null) {
            throw new TemporaryValidatorException("pattern is null");
        }
        return pattern.matches(value);
    }

    @Override
    public String toString() {
        return pattern.toString();
    }
}
