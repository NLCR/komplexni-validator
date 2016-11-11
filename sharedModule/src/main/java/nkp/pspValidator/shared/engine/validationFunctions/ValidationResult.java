package nkp.pspValidator.shared.engine.validationFunctions;

/**
 * Created by martin on 27.10.16.
 */
public class ValidationResult {

    private final boolean valid;
    private String message;

    public ValidationResult(boolean valid) {
        this.valid = valid;
    }

    public ValidationResult withMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
