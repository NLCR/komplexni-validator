package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.validationFunctions.ValidationFunction;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

/**
 * Created by martin on 30.10.16.
 */
public class Rule {

    private final Integer sectionId;
    private final Integer id;
    private final String name;
    private final ValidationFunction function;

    private String description;

    private ValidationResult result;

    public Rule(Integer sectionId, Integer id, String name, ValidationFunction function) {
        this.sectionId = id;
        this.id = id;
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
