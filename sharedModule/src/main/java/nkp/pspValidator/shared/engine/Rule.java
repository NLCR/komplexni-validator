package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.validationFunctions.ValidationFunction;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

/**
 * Created by Martin Řehánek on 30.10.16.
 */
public class Rule {

    private final Integer sectionId;
    private final Integer id;
    private final String name;
    private final String description;
    private final ValidationFunction function;
    private ValidationResult result;

    public Rule(Integer sectionId, Integer id, String name, String description, ValidationFunction function) {
        this.sectionId = sectionId;
        this.id = id;
        this.name = name;
        this.description = description;
        this.function = function;
    }

    public Rule copyWithoutValidationFunction() {
        return new Rule(sectionId.intValue(), id.intValue(), name, description, null);
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

    public Integer getSectionId() {
        return sectionId;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        if (!sectionId.equals(rule.sectionId)) return false;
        return id.equals(rule.id);

    }

    @Override
    public int hashCode() {
        int result = sectionId.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
