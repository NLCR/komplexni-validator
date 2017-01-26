package nkp.pspValidator.gui.validation;

import nkp.pspValidator.shared.engine.Rule;

/**
 * Created by Martin Řehánek on 16.12.16.
 */
public class RuleWithState {

    private final Integer sectionId;
    private final Integer id;
    private final String name;
    private final String description;
    private Integer errors = 0;
    private Integer warnings = 0;
    private Integer infos = 0;
    private ProcessingState state = ProcessingState.WAITING;

    public RuleWithState(Rule rule) {
        this.sectionId = rule.getSectionId();
        this.id = rule.getId();
        this.name = rule.getName();
        this.description = rule.getDescription();
    }

    public Integer getSectionId() {
        return sectionId;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getErrors() {
        return errors;
    }

    public Integer getWarnings() {
        return warnings;
    }

    public Integer getInfos() {
        return infos;
    }

    public ProcessingState getState() {
        return state;
    }

    public void setErrors(Integer errors) {
        this.errors = errors;
    }

    public void setWarnings(Integer warnings) {
        this.warnings = warnings;
    }

    public void setInfos(Integer infos) {
        this.infos = infos;
    }

    public void setState(ProcessingState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleWithState that = (RuleWithState) o;

        if (!sectionId.equals(that.sectionId)) return false;
        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        int result = sectionId.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
