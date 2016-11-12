package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 27.10.16.
 */
public class ValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(ValidationError error) {
        errors.add(error);
    }

    public void addError(Level level, String messae) {
        errors.add(new ValidationError(level, messae));
    }


    public List<ValidationError> getErrors() {
        return errors;
    }


}
