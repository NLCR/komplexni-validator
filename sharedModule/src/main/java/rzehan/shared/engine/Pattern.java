package rzehan.shared.engine;

import rzehan.shared.engine.exceptions.InvalidVariableTypeException;
import rzehan.shared.engine.exceptions.TemporaryValidatorException;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by martin on 20.10.16.
 */
public class Pattern {

    private final Engine engine;
    private final String description;
    private final List<Expression> expressions;


    public Pattern(Engine engine, List<Expression> expressions) {
        this.engine = engine;
        this.description = null;
        this.expressions = expressions;
    }

    public Pattern(Engine engine, Expression expression) {
        this.engine = engine;
        this.description = null;
        List<Expression> list = new ArrayList<>();
        list.add(expression);
        this.expressions = list;
    }

    public Pattern(Engine engine, Expression... expressions) {
        this.engine = engine;
        this.description = null;
        List<Expression> list = new ArrayList<>();
        this.expressions = Arrays.asList(expressions);
    }

    public boolean matches(String string) {
        for (Expression e : expressions) {
            if (e.matches(string)) {
                return true;
            }
        }
        return false;
    }

    public static class Expression {

        private final Engine engine;
        private final boolean caseSensitive;

        private final Set<String> variableNames;
        private final Map<String, String> variableValues = new HashMap<>();

        private final String regexpWithPlaceholders;
        private String regexpFinal;
        private java.util.regex.Pattern compiledPattern;

        public Expression(Engine engine, boolean caseSensitive, String regexpWithPlaceholders) {
            this.engine = engine;
            this.caseSensitive = caseSensitive;
            this.regexpWithPlaceholders = regexpWithPlaceholders;
            this.variableNames = extractVariableNames();
        }

        private Set<String> extractVariableNames() {
            Set<String> result = new HashSet<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{(.[A-Z0-9_\\-]*?)\\}");
            Matcher matcher = pattern.matcher(regexpWithPlaceholders);
            while (matcher.find()) {
                String placeholder = matcher.group();
                String varName = placeholder.substring(2, placeholder.length() - 1);
                result.add(varName);
            }
            return result;
        }

        private java.util.regex.Pattern compile() {
            replacePlaceholdersWithValues();
            if (caseSensitive) {
                return java.util.regex.Pattern.compile(regexpFinal);
            } else {
                return java.util.regex.Pattern.compile(regexpFinal,
                        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
            }
        }

        private void replacePlaceholdersWithValues() {
            regexpFinal = regexpWithPlaceholders;
            for (String varName : variableValues.keySet()) {
                String placeholder = "${" + varName + "}";
                if (regexpFinal.contains(placeholder)) {
                    String varValue = escapeRegexpSpecialChars(variableValues.get(varName));
                    regexpFinal = regexpFinal.replace(placeholder, varValue);
                }
            }
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

        public boolean matches(String string) {
            if (compiledPattern == null) {
                evaluateAllVariables();
                compiledPattern = compile();
            }
            Matcher m = compiledPattern.matcher(string);
            return m.matches();
        }

        private void evaluateAllVariables() {
            for (String varName : variableNames) {
                ValueEvaluation evaluation = engine.getValueEvaluationByVariable(varName);
                Object varValue = evaluation.getData();
                if (varValue == null) {
                    throw new TemporaryValidatorException("proměnná " + varName + " je null");
                }
                if (!(varValue instanceof String)) {
                    throw new InvalidVariableTypeException(varName, "String");
                }
                variableValues.put(varName, (String) varValue);
            }
        }

        public String toString() {
            return regexpFinal;
        }

    }

    @Override
    public String toString() {
        return toString(expressions);
    }

    private String toString(List<Expression> expressions) {
        if (expressions == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        {
            builder.append("[");
            for (int i = 0; i < expressions.size(); i++) {
                if (i != 0) {
                    builder.append(';');
                }
                builder.append(expressions.get(i).toString());
            }
            builder.append("]");
        }
        return builder.toString();
    }
}
