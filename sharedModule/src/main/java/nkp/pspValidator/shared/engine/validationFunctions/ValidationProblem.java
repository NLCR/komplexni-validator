package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

/**
 * Created by Martin Řehánek on 11.11.16.
 */
public class ValidationProblem {

    private final Level level;
    private final String message;

    public ValidationProblem(Level level, String message) {
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
