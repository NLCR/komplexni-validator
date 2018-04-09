package nkp.pspValidator.gui.exclusions.data;

import nkp.pspValidator.shared.engine.RulesSection;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ExcludedSection {
    private State state;
    private String name;
    private String description;

    public ExcludedSection() {
    }

    public ExcludedSection(ExcludedSection original) {
        state = original.getState();
        name = original.getName();
        description = original.getDescription();
    }

    public ExcludedSection(RulesSection section) {
        state = State.ENABLED;
        name = section.getName();
        description = section.getDescription();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum State {
        ENABLED, DISABLED;
    }

}
