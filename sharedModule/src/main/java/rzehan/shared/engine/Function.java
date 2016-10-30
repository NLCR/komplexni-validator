package rzehan.shared.engine;

import rzehan.shared.engine.params.PatternParamConstant;
import rzehan.shared.engine.params.PatternParamReference;
import rzehan.shared.engine.params.ValueParamConstant;
import rzehan.shared.engine.params.ValueParamReference;
import rzehan.shared.engine.validationFunctions.ValidationFunction;

/**
 * Created by martin on 30.10.16.
 */
public interface Function {

    public Function withValueParam(String paramName, ValueType valueType, Object value);

    public Function withValueParamByReference(String paramName, ValueType valueType, String varName);

    public Function withPatternParam(String paramName, Pattern pattern);

    public Function withPatternParamByReference(String paramName, String varName);
}
