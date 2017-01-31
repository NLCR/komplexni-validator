package nkp.pspValidator.shared.biblio.biblioValidator;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedAttributeDefinition {
    private String attributeName;
    private boolean mandatory;
    private ExpectedContentDefinition expectedContentDefinition;

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public ExpectedContentDefinition getExpectedContentDefinition() {
        return expectedContentDefinition;
    }

    public void setExpectedContentDefinition(ExpectedContentDefinition expectedContentDefinition) {
        this.expectedContentDefinition = expectedContentDefinition;
    }
}
