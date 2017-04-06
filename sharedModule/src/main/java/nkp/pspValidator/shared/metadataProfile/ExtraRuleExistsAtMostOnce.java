package nkp.pspValidator.shared.metadataProfile;

import nkp.pspValidator.shared.engine.XmlManager;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created by Martin Řehánek on 27.1.17.
 */
public class ExtraRuleExistsAtMostOnce implements ExtraRule {

    private final String xpath;
    private final String description;

    public ExtraRuleExistsAtMostOnce(String xpath, String description) {
        this.xpath = xpath;
        this.description = description;
    }

    @Override
    public CheckingResult checkAgainst(XmlManager manager, Element currentElement) {
        try {
            XPathExpression xPathExpression = manager.buildXpath(xpath);
            NodeList elementsByXpath = (NodeList) xPathExpression.evaluate(currentElement, XPathConstants.NODESET);
            int occurences = elementsByXpath.getLength();
            if (occurences > 1) {
                return new CheckingResultFail() {
                    @Override
                    public String getErrorMessage() {
                        String content = nodelistToString(elementsByXpath);
                        if (description == null || description.isEmpty()) {
                            return String.format("povolen nejvýše jeden výskyt %s, nalezeno celkem %d výskytů: %s",
                                    xpath, occurences, content);
                        } else {
                            return String.format("povolen nejvýše jeden výskyt %s, nalezeno celkem %d výskytů: %s. %s",
                                    xpath, occurences, content, description);
                        }
                    }
                };
            } else {
                return new CheckingResultMatch();
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

    private String nodelistToString(NodeList nodeList) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE:
                    builder.append(((Element) node).getNodeName());
                    break;
                case Node.ATTRIBUTE_NODE:
                    builder.append('@').append(node.getNodeValue());
                    break;
                case Node.TEXT_NODE:
                    builder.append('"').append(node.getNodeValue()).append('"');
                    break;
            }
            if (i != nodeList.getLength() - 1) {
                builder.append(',');
            }
        }

        builder.append(']');
        return builder.toString();
    }
}
