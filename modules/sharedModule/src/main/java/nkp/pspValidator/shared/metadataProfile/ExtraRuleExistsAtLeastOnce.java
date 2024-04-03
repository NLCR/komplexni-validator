package nkp.pspValidator.shared.metadataProfile;

import nkp.pspValidator.shared.engine.XmlManager;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created by Martin Řehánek on 27.1.17.
 */
public class ExtraRuleExistsAtLeastOnce implements ExtraRule {

    private final String xpath;
    private final String description;

    public ExtraRuleExistsAtLeastOnce(String xpath, String description) {
        this.xpath = xpath;
        this.description = description;
    }

    @Override
    public CheckingResult checkAgainst(XmlManager manager, Element currentElement) {
        try {
            XPathExpression xPathExpression = manager.buildXpath(xpath);
            NodeList elementsByXpath = (NodeList) xPathExpression.evaluate(currentElement, XPathConstants.NODESET);
            if (elementsByXpath.getLength() >= 1) {
                return new CheckingResultMatch();
            } else {
                return new CheckingResultFail() {
                    @Override
                    public String getErrorMessage() {
                        if (description == null || description.isEmpty()) {
                            return String.format("musí být přítomen alespoň jeden výskyt %s", xpath);
                        } else {
                            return String.format("musí být přítomen alespoň jeden výskyt %s. %s", xpath, description);
                        }
                    }
                };
            }

        } catch (InvalidXPathExpressionException e) {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return e.getMessage();
                }
            };
        } catch (XPathExpressionException e) {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return e.getMessage();
                }
            };
        }
    }
}
