package nkp.pspValidator.gui.validation;

import nkp.pspValidator.shared.engine.RulesSection;

/**
 * Created by Martin Řehánek on 16.12.16.
 */
public class SectionWithState {

    private final Integer id;
    private final String name;
    private final String description;
    private boolean enabled = true;
    private Integer errors = 0;
    private Integer warnings = 0;
    private Integer infos = 0;
    private ProcessingState state = ProcessingState.WAITING;

    public SectionWithState(RulesSection section) {
        this.id = section.getId();
        this.name = section.getName();
        this.description = section.getDescription();
        this.enabled = section.isEnabled();
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


    public boolean isEnabled() {
        return enabled;
    }

    public ProcessingState getState() {
        return state;
    }

    public void setState(ProcessingState state) {
        this.state = state;
    }

    public Integer getErrors() {
        return errors;
    }

    public void setErrors(Integer errors) {
        this.errors = errors;
    }

    public Integer getWarnings() {
        return warnings;
    }

    public void setWarnings(Integer warnings) {
        this.warnings = warnings;
    }

    public Integer getInfos() {
        return infos;
    }

    public void setInfos(Integer infos) {
        this.infos = infos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SectionWithState that = (SectionWithState) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
