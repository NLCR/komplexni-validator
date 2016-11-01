package rzehan.shared.engine.params;

import rzehan.shared.engine.PatternEvaluation;

/**
 * Created by martin on 24.10.16.
 */
public abstract class PatternParam {

    //public abstract boolean matches(String value);

    public abstract PatternEvaluation getEvaluation();

}
