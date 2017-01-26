package nkp.pspValidator.shared.engine;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class ValueEvaluation {
    private final Object data;
    private final String errorMessage;

    public ValueEvaluation(Object data, String errorMessage) {
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public ValueEvaluation(Object data) {
        this.data = data;
        this.errorMessage = null;
    }

    public Object getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
