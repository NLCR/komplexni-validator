package nkp.pspValidator.shared.engine;

import java.util.List;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class PatternEvaluation {

    private final List<PatternExpression> expressions;
    private final String errorMessage;


    public PatternEvaluation(List<PatternExpression> expressions, String errorMessage) {
        this.expressions = expressions;
        this.errorMessage = errorMessage;
    }

    public List<PatternExpression> getExpressions() {
        return expressions;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isOk() {
        return expressions != null;
    }

    public boolean matches(String string) {
        for (PatternExpression expression : expressions) {
            if (expression.matches(string)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return Utils.listToString(expressions);
    }
}
