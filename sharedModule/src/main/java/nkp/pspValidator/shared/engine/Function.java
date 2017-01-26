package nkp.pspValidator.shared.engine;

/**
 * Created by Martin Řehánek on 30.10.16.
 */
public interface Function {

    public Function withValueParam(String paramName, ValueType valueType, ValueEvaluation valueEvaluation);

    public Function withValueParamByReference(String paramName, ValueType valueType, String varName);

    //TODO: tohle bude problem, nejspis nemuzu patterny vyhodnocovat on-the fly
    //oproti promennym se musi cekat na dostupnost promenne v tele patternu
    public Function withPatternParam(String paramName, PatternEvaluation patternEvaluation);

    public Function withPatternParamByReference(String paramName, String varName);
}
