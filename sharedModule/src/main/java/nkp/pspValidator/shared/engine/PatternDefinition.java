package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.exceptions.VariableNotDefinedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class PatternDefinition {

    private final Engine engine;
    private final List<PatternExpression> rawExpressions = new ArrayList<>();

    public PatternDefinition(Engine engine) {
        this.engine = engine;
    }

    public PatternDefinition withRawExpression(PatternExpression expression) {
        rawExpressions.add(expression);
        return this;
    }

    public PatternDefinition withRawExpressions(List<PatternExpression> expressions) {
        rawExpressions.addAll(expressions);
        return this;
    }

    public PatternEvaluation evaluate() {
        try {
            List<PatternExpression> finalExpressions = new ArrayList<>(rawExpressions.size());
            for (PatternExpression rawExpression : rawExpressions) {
                finalExpressions.add(fillPlaceholders(rawExpression));
            }
            return new PatternEvaluation(finalExpressions, null);
        } catch (VariableNotDefinedException e) {
            return new PatternEvaluation(null, String.format("neznámá hodnota promměnné %s: %s", e.getVarName(), e.getMessage()));
        }
    }

    private PatternExpression fillPlaceholders(PatternExpression rawExpression) throws VariableNotDefinedException {
        Set<String> variables = extractVariableNames(rawExpression.getRegexp());
        String finalRegexp = rawExpression.getRegexp();
        for (String varName : variables) {
            String placeholder = "${" + varName + "}";
            if (finalRegexp.contains(placeholder)) {
                Object varValue = getEvaluation(varName);
                String varValueEscaped = escapeRegexpSpecialChars(varValue.toString());
                finalRegexp = finalRegexp.replace(placeholder, varValueEscaped);
            }
        }
        return new PatternExpression(rawExpression.isCaseSensitive(), finalRegexp);
    }

    private Object getEvaluation(String varName) throws VariableNotDefinedException {
        ValueEvaluation valueEvaluation = engine.getValueEvaluationByVariable(varName);
        if (valueEvaluation.getData() == null) {
            throw new VariableNotDefinedException(varName, valueEvaluation.getErrorMessage());
        } else {
            return valueEvaluation.getData();
        }
    }

    private Set<String> extractVariableNames(String expressionWithPlaceholders) {
        Set<String> result = new HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{(.[A-Z0-9_\\-]*?)\\}");
        Matcher matcher = pattern.matcher(expressionWithPlaceholders);
        while (matcher.find()) {
            String placeholder = matcher.group();
            String varName = placeholder.substring(2, placeholder.length() - 1);
            result.add(varName);
        }
        return result;
    }

    private String escapeRegexpSpecialChars(String s) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isRegexpSpecialChar(c)) {
                builder.append('\\');
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private boolean isRegexpSpecialChar(char c) {
        String special = "\\.[]{}()*+-?^$|";
        for (int i = 0; i < special.length(); i++) {
            if (c == special.charAt(i)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return Utils.listToString(rawExpressions);
    }


}
