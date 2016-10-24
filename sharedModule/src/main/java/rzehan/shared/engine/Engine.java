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

    private final Map<String, VariableDefinition> varDefinitions = new HashMap<>();
    private final Map<String, Object> varValues = new HashMap<>();


    public Engine(ProvidedVarsManager providedVarsManager) {
        this.providedVarsManager = providedVarsManager;
    }

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public Pattern.Expression newExpression(boolean caseSensitive, String originalRegexp) {
        return new Pattern.Expression(this, caseSensitive, originalRegexp);
    }

    public Pattern newPattern(Pattern.Expression... expressions) {
        return new Pattern(this, expressions);
    }


    public Object evaluateVariable(String variableName) {
        Object value = varValues.get(variableName);
        if (value != null) {
            return value;
        } else {//not evaluated yet
            VariableDefinition definition = varDefinitions.get(variableName);
            if (definition == null) {
                throw new VariableNotDefinedException(variableName);
            } else {
                value = definition.evaluate();
                varValues.put(variableName, value);
                return value;
            }
        }
    }


    //TODO: limit usage, probably just here and in tests
    public EvaluationFunction getEvaluationFunction(String name) {
        if (name.equals("PROVIDED_FILE")) {
            return new EfProvidedFile(this);
        } else if (name.equals("PROVIDED_STRING")) {
            return new EfProvidedString(this);
        } else if (name.equals("PROVIDED_INTEGER")) {
            return new EfProvidedInteger(this);
        } else if (name.equals("RETURN_FIRST_FILE_FROM_LIST")) {
            return new EfReturnFirstFileFromList(this);
        } else if (name.equals("FIND_FILES_IN_DIR_BY_PATTERN")) {
            return new EfFindFilesInDirByPattern(this);
        } else {
            throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }


    public void defineVariable(String name, ValueType type, String efName, EvaluationFunction.ValueParams valueParams) {
        //TODO: check if not defined already
        EvaluationFunction evaluationFunction = getEvaluationFunction(efName);
        VariableDefinition definition = new VariableDefinition(type, evaluationFunction, valueParams);
        varDefinitions.put(name, definition);
    }


}
