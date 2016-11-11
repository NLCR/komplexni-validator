package nkp.pspValidator.shared.engine.exceptions;

/**
 * Created by martin on 1.11.16.
 */
public class InvalidXPathExpressionException extends Exception {

    private final String xpathExpression;

    public InvalidXPathExpressionException(String xpathExpression, String message) {
        super(message);
        this.xpathExpression = xpathExpression;
    }

    public String getXpathExpression() {
        return xpathExpression;
    }
}
