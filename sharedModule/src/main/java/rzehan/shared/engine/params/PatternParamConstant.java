package rzehan.shared.engine.params;

import rzehan.shared.engine.Pattern;
import rzehan.shared.engine.PatternEvaluation;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.TemporaryValidatorException;

/**
 * Created by martin on 24.10.16.
 */
public class PatternParamConstant extends PatternParam {

//    private final Pattern pattern;

    private final PatternEvaluation evaluation;

/*    public PatternParamConstant(Pattern pattern) {
        this.pattern = pattern;
    }*/




    /*@Override
    public boolean matches(String value) {
        if (value == null) {
            throw new TemporaryValidatorException("value is null");
        } else if (pattern == null) {
            throw new TemporaryValidatorException("pattern is null");
        }
        return pattern.matches(value);
    }*/

    public PatternParamConstant(PatternEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    @Override
    public String toString() {
        return evaluation.toString();
    }

    @Override
    public PatternEvaluation getEvaluation() {
        return evaluation;
    }

/*
    private final ValueEvaluation evaluation;

    public ValueParamConstant(ValueType type, ValueEvaluation evaluation) {
        super(type);
        this.evaluation = evaluation;
    }

    public ValueEvaluation getEvaluation() {
        return evaluation;
    }*/
}
