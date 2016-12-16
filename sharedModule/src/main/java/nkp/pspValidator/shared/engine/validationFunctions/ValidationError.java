package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

/**
 * Created by martin on 11.11.16.
 */
public class ValidationError {

    private final Level level;
    private final String message;

    public ValidationError(Level level, String message) {
        this.level = level;
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
