package rzehan.shared.engine;

import rzehan.shared.engine.evaluationFunctions.*;
import rzehan.shared.engine.exceptions.VariableNotDefinedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 20.10.16.
 */
public class Engine {

    private final ProvidedVarsManager providedVarsManager;

    private final Map<String, ValueDefinition> valueDefinitionsByVarName = new HashMap<>();
    private final Map<String, Object> valuesByVarName = new HashMap<>();
    private final Map<String, Pattern> patternsByVarName = new HashMap<>();


    public Engine(ProvidedVarsManager providedVarsManager) {
        this.providedVarsManager = providedVarsManager;
    }

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public Pattern.Expression buildExpression(boolean caseSensitive, String originalRegexp) {
        return new Pattern.Expression(this, caseSensitive, originalRegexp);
    }

    public Pattern buildPattern(Pattern.Expression... expressions) {
        return new Pattern(this, expressions);
    }

    //TODO: limit usage, probably just here and in tests
    public EvaluationFunction buildEvaluationFunction(String name) {
        if (name.equals("PROVIDED_FILE")) {
            return new EfProvidedFile(this);
        } else if (name.equals("PROVIDED_STRING")) {
            return new EfProvidedString(this);
        } else if (name.equals("PROVIDED_INTEGER")) {
            return new EfProvidedInteger(this);
        } else if (name.equals("RETURN_FIRST_FILE_FROM_LIST")) {
            return new EfReturnFirstFileFromFileList(this);
        } else if (name.equals("FIND_FILES_IN_DIR_BY_PATTERN")) {
            return new EfFindFilesInDirByPattern(this);
        } else {
            throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }

    public ValueDefinition buildValueDefinition(ValueType type, String efName, EvaluationFunction.ValueParams valueParams, EvaluationFunction.PatternParams patternParams) {
        EvaluationFunction evaluationFunction = buildEvaluationFunction(efName);
        ValueDefinition definition = new ValueDefinition(type, evaluationFunction, valueParams, patternParams);
        return definition;
    }

    public void registerPattern(String patternVariableName, Pattern pattern) {
        //TODO: check if not defined already
        patternsByVarName.put(patternVariableName, pattern);
    }

    public void registerValueDefinition(String valueVariableName, ValueDefinition definition) {
        //TODO: check if not defined already
        valueDefinitionsByVarName.put(valueVariableName, definition);
    }


    public Pattern getPatternFromVariable(String patternVariableName) {
        return patternsByVarName.get(patternVariableName);
    }

    public Object getValueFromVariable(String valueVariableName) {
        Object value = valuesByVarName.get(valueVariableName);
        if (value != null) {
            return value;
        } else {//not evaluated yet
            ValueDefinition definition = valueDefinitionsByVarName.get(valueVariableName);
            if (definition == null) {
                throw new VariableNotDefinedException(valueVariableName);
            } else {
                value = definition.evaluate();
                valuesByVarName.put(valueVariableName, value);
                return value;
            }
        }
    }


}
