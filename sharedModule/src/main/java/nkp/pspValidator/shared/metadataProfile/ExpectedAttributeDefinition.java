package nkp.pspValidator.shared.metadataProfile;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedAttributeDefinition {
    private String attributeName;
    private boolean mandatory;
    private ContentDefinition expectedContentDefinition;
    private ContentDefinition recommendedContentDefinition;

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

    public ContentDefinition getExpectedContentDefinition() {
        return expectedContentDefinition;
    }

    public void setExpectedContentDefinition(ContentDefinition expectedContentDefinition) {
        this.expectedContentDefinition = expectedContentDefinition;
    }

    public void setRecommendedContentDefinition(ContentDefinition recommendedContentDefinition) {
        this.recommendedContentDefinition = recommendedContentDefinition;
    }

    public ContentDefinition getRecommendedContentDefinition() {
        return recommendedContentDefinition;
    }
}
