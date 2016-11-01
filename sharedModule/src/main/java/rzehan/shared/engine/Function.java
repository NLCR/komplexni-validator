package rzehan.shared.engine;

/**
 * Created by martin on 30.10.16.
 */
public interface Function {

    //TODO: tady rovnou pouzivat ValueDefinition
    //a pak metody prejmenovat
    public Function withValueParam(String paramName, ValueType valueType, ValueEvaluation valueEvaluation);

    public Function withValueParamByReference(String paramName, ValueType valueType, String varName);

    //public Function withPatternParam(String paramName, Pattern pattern);

    //TODO: tohle bude problem, nejspis nemuzu patterny vyhodnocovat on-the fly
    public Function withPatternParam(String paramName, PatternEvaluation patternEvaluation);

    public Function withPatternParamByReference(String paramName, String varName);
}
