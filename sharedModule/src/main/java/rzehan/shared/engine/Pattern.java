package rzehan.shared.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by martin on 20.10.16.
 */
public class Pattern {

    private final String description;
    private final List<Expression> expressions;

    public Pattern(String description, List<Expression> expressions) {
        this.description = description;
        this.expressions = expressions;
    }

    public Pattern(Expression expression) {
        this.description = null;
        List<Expression> list = new ArrayList<>();
        list.add(expression);
        this.expressions = list;
    }

    public Pattern(Expression... expressions) {
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
        /*todo: input vars*/

        private final boolean caseSensitive;
        private final String value;
        private java.util.regex.Pattern compiled;

        public Expression(boolean caseSensitive, String value) {
            this.caseSensitive = caseSensitive;
            this.value = value;
            //todo: az kdyz bodou vsechny zavislosti splnene
            this.compiled = compile();
        }

        private java.util.regex.Pattern compile() {
            //todo: nahrazovani hodnot promennymi
            if (caseSensitive) {
                return java.util.regex.Pattern.compile(value);
            } else {
                return java.util.regex.Pattern.compile(value,
                        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
            }
        }

        public boolean matches(String string) {
            Matcher m = compiled.matcher(string);
            return m.matches();
        }
        //todo: metoda, ktera vraci vzor s jiz nahrazenou promennou kvuli chybovym hlaskam
    }
}
