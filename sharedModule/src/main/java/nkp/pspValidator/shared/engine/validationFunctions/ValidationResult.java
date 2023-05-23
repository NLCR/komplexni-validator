package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class ValidationResult {

    private final List<ValidationProblem> errors = new ArrayList<>();

    public boolean hasProblems() {
        return !errors.isEmpty();
    }

    public void addError(ValidationProblem error) {
        errors.add(error);
    }

    public void addError(Level level, File file, String message) {
        errors.add(new ValidationProblem(level, file, message));
    }

    public void addError(Level level, File file, String message, Object... params) {
        errors.add(new ValidationProblem(level, file, String.format(message, params)));
    }

    public List<ValidationProblem> getProblems() {
        return errors;
    }

    public static ValidationResult ok() {
        return new ValidationResult();
    }

}
