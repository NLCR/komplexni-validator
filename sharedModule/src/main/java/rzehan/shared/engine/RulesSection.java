package rzehan.shared.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 30.10.16.
 */
public class RulesSection {
    private final String name;
    private final List<Rule> rules = new ArrayList<>();

    private String description;
    private boolean enabled = true;

    public RulesSection(String name) {
        this.name = name;
    }


    public RulesSection withDescription(String description) {
        this.description = description;
        return this;
    }

    public RulesSection withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getName() {
        return name;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RulesSection that = (RulesSection) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
