package nkp.pspValidator.shared.engine.params;

import nkp.pspValidator.shared.engine.PatternEvaluation;

/**
 * Created by Martin Řehánek on 24.10.16.
 */
public abstract class PatternParam {

    //public abstract boolean matches(String value);

    public abstract PatternEvaluation getEvaluation();

}
