package rzehan.shared.engine;

import rzehan.shared.engine.validationFunctions.ValidationFunction;
import rzehan.shared.engine.validationFunctions.ValidationResult;

/**
 * Created by martin on 30.10.16.
 */
public class Rule {

    private final String name;
    private final Level level;
    private final ValidationFunction function;

    private String description;

    private ValidationResult result;

    public Rule(String name, Level level, ValidationFunction function) {
        this.name = name;
        this.level = level;
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

    public Level getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public static enum Level {
        INFO, WARNING, ERROR;
    }
}
