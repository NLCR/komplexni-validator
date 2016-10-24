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

    public Engine(ProvidedVarsManager providedVarsManager) {
        this.providedVarsManager = providedVarsManager;
    }

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public Variable getVariable(String name) {
        return null;
    }

    public Pattern.Expression newExpression(boolean caseSensitive, String originalRegexp) {
        return new Pattern.Expression(this, caseSensitive, originalRegexp);
    }

    public Pattern newPattern(Pattern.Expression... expressions) {
        return new Pattern(this, expressions);
    }

    private final Map<String, Variable> variables = new HashMap<>();

    public void defineVariable(String name, Variable var) {
        variables.put(name, var);
    }

    public Object evaluateVariable(String variableName) {
        Variable variable = variables.get(variableName);
        if (variable == null) {
            throw new VariableNotDefinedException(variableName);
        }
        return variable.getValue();
    }

    public EvaluationFunction getEvaluationFunction(String name) {
        if (name.equals("PROVIDED_FILE")) {
            return new EfProvidedFile(this);
        } else if (name.equals("PROVIDED_STRING")) {
            return new EfProvidedString(this);
        } else if (name.equals("RETURN_FIRST_FILE_FROM_LIST")) {
            return new EfReturnFirstFileFromList(this);
        } else if (name.equals("FIND_FILES_IN_DIR_BY_PATTERN")) {
            return new EfFindFilesInDirByPattern(this);
        } else {
            throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }


}
