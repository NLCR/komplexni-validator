package nkp.pspValidator.shared.engine;

/**
 * Created by Martin Řehánek on 30.10.16.
 */
public class RulesSection {
    private final Integer id;
    private final String name;
    private final String description;
    private boolean enabled = true;

    public RulesSection(Integer id, String name, String description) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public RulesSection setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public RulesSection copy() {
        return new RulesSection(id.intValue(), name, description);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RulesSection that = (RulesSection) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
