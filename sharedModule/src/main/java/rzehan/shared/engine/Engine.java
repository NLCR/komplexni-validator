package rzehan.shared.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 20.10.16.
 */
public class Engine {


    public Variable getVariable(String name) {
        return null;
    }

    public Pattern.Expression newExpression(boolean caseSensitive, String originalRegexp) {
        return new Pattern.Expression(this, caseSensitive, originalRegexp);
    }

    public Pattern.Expression newExpression(boolean caseSensitive, String originalRegexp, String... variableNames) {
        return new Pattern.Expression(this, caseSensitive, originalRegexp, variableNames);
    }

    public Pattern newPattern(Pattern.Expression... expressions) {
        return new Pattern(this, expressions);
    }


    private final Map<String, Variable> variables = new HashMap<>();

    public void defineVariable(String name, Variable var) {
        variables.put(name, var);
    }

    public String evaluateStringVariable(String variableName) {
        Variable variable = variables.get(variableName);
        if (variable == null) {
            throw new VariableNotDefinedException(variableName);
        }
        return variable.getValue();
    }

}
