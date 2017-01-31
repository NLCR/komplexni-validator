package nkp.pspValidator.shared.biblio.biblioValidator;

import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedElementDefinition {

    //template
    private final BiblioTemplate template;

    private final String errorMessage;

    //parent element
    private final ExpectedElementDefinition parent;

    //element itself
    private String elementNameNsPrefix;
    private String elementName;
    private String specification; //doplnujici xpath (where)
    private boolean mandatory;
    private boolean repeatable;

    //expected children
    private List<ExpectedAttributeDefinition> expectedAttributes = Collections.emptyList();
    private List<ExpectedElementDefinition> expectedElementDefinitions = Collections.emptyList();
    private ExpectedContentDefinition expectedContentDefinition;
    private List<ExtraRule> extraRules = Collections.emptyList();

    public ExpectedElementDefinition(BiblioTemplate template, ExpectedElementDefinition parent, String errorMessage) {
        this.template = template;
        this.parent = parent;
        this.errorMessage = errorMessage;
    }

    public String getElementNameNsPrefix() {
        return elementNameNsPrefix;
    }

    public void setElementNameNsPrefix(String elementNameNsPrefix) {
        this.elementNameNsPrefix = elementNameNsPrefix;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public List<ExpectedAttributeDefinition> getExpectedAttributes() {
        return expectedAttributes;
    }

    public void setExpectedAttributes(List<ExpectedAttributeDefinition> expectedAttributes) {
        if (expectedAttributes == null) {
            throw new IllegalArgumentException("expectedAttributes mustn't be null");
        }
        this.expectedAttributes = expectedAttributes;
    }

    public List<ExpectedElementDefinition> getExpectedElementDefinitions() {
        return expectedElementDefinitions;
    }

    public void setExpectedElementDefinitions(List<ExpectedElementDefinition> expectedElementDefinitions) {
        if (expectedElementDefinitions == null) {
            throw new IllegalArgumentException("expectedElementDefinitions mustn't be null");
        }
        this.expectedElementDefinitions = expectedElementDefinitions;
    }

    public List<ExtraRule> getExtraRules() {
        return extraRules;
    }

    public void setExtraRules(List<ExtraRule> extraRules) {
        if (extraRules == null) {
            throw new IllegalArgumentException("extraRules mustn't be null");
        }
        this.extraRules = extraRules;
    }

    public ExpectedContentDefinition getExpectedContentDefinition() {
        return expectedContentDefinition;
    }

    public void setExpectedContentDefinition(ExpectedContentDefinition expectedContentDefinition) {
        this.expectedContentDefinition = expectedContentDefinition;
    }

    public String buildAbsoluteXpath() {
        StringBuilder builder = new StringBuilder();
        if (parent != null) {
            builder.append(parent.buildAbsoluteXpath());
        }
        builder.append('/');
        builder.append(buildRelativeXpath());
        return builder.toString();
    }

    public String buildRelativeXpath() {
        StringBuilder builder = new StringBuilder();
        if (elementNameNsPrefix != null) {
            builder.append(elementNameNsPrefix).append(':');
        }
        builder.append(elementName);
        if (specification != null) {
            builder.append('[').append(specification).append(']');
        }
        return builder.toString();
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
