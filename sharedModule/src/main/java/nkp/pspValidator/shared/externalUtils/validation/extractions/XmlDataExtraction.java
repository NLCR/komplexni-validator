package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.externalUtils.ExtractionResultType;
import nkp.pspValidator.shared.externalUtils.validation.DataExtraction;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public abstract class XmlDataExtraction implements DataExtraction {

    final ExtractionResultType extractionResultType;
    private final NamespaceContext namespaceContext;

    protected XmlDataExtraction(ExtractionResultType extractionResultType, NamespaceContext namespaceContext) {
        this.extractionResultType = extractionResultType;
        this.namespaceContext = namespaceContext;
    }

    XPathExpression buildXpath(String xpathExpression) throws ExtractionException {
        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(namespaceContext);
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new ExtractionException(String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }

    Object extractData(XPathExpression xPath, Object processedOutput) throws XPathExpressionException {
        try {
            switch (extractionResultType) {
                case STRING: {
                    return xPath.evaluate(processedOutput, XPathConstants.STRING);
                }
                case INTEGER: {
                    String dataStr = (String) xPath.evaluate(processedOutput, XPathConstants.STRING);
                    if (dataStr == null) {
                        return null;
                    } else {
                        return Integer.valueOf(dataStr);
                    }
                }
                case FLOAT: {
                    String dataStr = (String) xPath.evaluate(processedOutput, XPathConstants.STRING);
                    if (dataStr == null) {
                        return null;
                    } else {
                        return Float.valueOf(dataStr);
                    }
                }
                case STRING_LIST: {
                    NodeList nodes = (NodeList) xPath.evaluate(processedOutput, XPathConstants.NODESET);
                    if (nodes == null) {
                        return null;
                    } else {
                        List<String> result = new ArrayList<>(nodes.getLength());
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Node node = nodes.item(i);
                            result.add(node.getTextContent());
                        }
                        return result;
                    }
                }
                case INTEGER_LIST: {
                    NodeList nodes = (NodeList) xPath.evaluate(processedOutput, XPathConstants.NODESET);
                    if (nodes == null) {
                        return null;
                    } else {
                        List<Integer> result = new ArrayList<>(nodes.getLength());
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Node node = nodes.item(i);
                            result.add(Integer.valueOf(node.getTextContent()));
                        }
                        return result;
                    }
                }
                case FLOAT_LIST: {
                    NodeList nodes = (NodeList) xPath.evaluate(processedOutput, XPathConstants.NODESET);
                    if (nodes == null) {
                        return null;
                    } else {
                        List<Float> result = new ArrayList<>(nodes.getLength());
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Node node = nodes.item(i);
                            result.add(Float.valueOf(node.getTextContent()));
                        }
                        return result;
                    }
                }
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
