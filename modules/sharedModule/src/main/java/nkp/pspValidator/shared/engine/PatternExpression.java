package nkp.pspValidator.shared.engine;

import java.util.regex.Matcher;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class PatternExpression {
    private final boolean caseSensitive;
    private final String regexp;
    private java.util.regex.Pattern compiledPattern;

    public PatternExpression(boolean caseSensitive, String regexp) {
        this.caseSensitive = caseSensitive;
        this.regexp = regexp;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public String getRegexp() {
        return regexp;
    }


    public boolean matches(String string) {
        if (compiledPattern == null) {
            compiledPattern = compile();
        }
        Matcher m = compiledPattern.matcher(string);
        return m.matches();
    }

    private java.util.regex.Pattern compile() {
        if (caseSensitive) {
            return java.util.regex.Pattern.compile(regexp);
        } else {
            return java.util.regex.Pattern.compile(regexp,
                    java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
        }
    }

    public String toString() {
        return regexp;
    }
}
