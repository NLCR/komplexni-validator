package nkp.pspValidator.shared.engine;

/**
 * Created by martin on 30.10.16.
 */
public class RulesSection {
    private final Integer id;
    private final String name;
    //private final List<Rule> rules = new ArrayList<>();

    private String description;
    private boolean enabled = true;

    public RulesSection(Integer id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
        this.name = name;
    }


    public RulesSection setDescription(String description) {
        this.description = description;
        return this;
    }

    public RulesSection setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getName() {
        return name;
    }

    /*public List<Rule> getRules() {
        return rules;
    }*/

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

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
