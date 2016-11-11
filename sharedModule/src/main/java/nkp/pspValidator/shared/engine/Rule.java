package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.validationFunctions.ValidationFunction;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

/**
 * Created by martin on 30.10.16.
 */
public class Rule {

    private final String name;
    private final ValidationFunction function;

    private String description;

    private ValidationResult result;

    public Rule(String name, ValidationFunction function) {
        this.name = name;
        this.function = function;
    }

    public Rule setDescription(String description) {
        this.description = description;
        return this;
    }

    public ValidationResult getResult() {
        if (result == null) {
            result = function.validate();
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }


}
