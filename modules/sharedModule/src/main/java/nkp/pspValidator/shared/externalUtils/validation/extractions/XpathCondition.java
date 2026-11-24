package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.externalUtils.validation.DataExtraction;
import nkp.pspValidator.shared.externalUtils.validation.ValidationCondition;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;

/**
 * Podmínka validace jako výraz XPath 1.0 vyhodnocený na boolean (např. not(//rule[@clause='6.7.11'])), viz
 * element condition v profilu binárního souboru.
 */
public class XpathCondition implements ValidationCondition {
    private final String expression;
    private final NamespaceContext namespaceContext;

    public XpathCondition(NamespaceContext namespaceContext, String expression) {
        this.namespaceContext = namespaceContext;
        this.expression = expression;
    }

    @Override
    public boolean holds(Object processedOutput) throws DataExtraction.ExtractionException {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(namespaceContext);
            XPathExpression compiled = xpath.compile(expression);
            return (Boolean) compiled.evaluate(processedOutput, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            throw new DataExtraction.ExtractionException(String.format("chyba v zápisu Xpath podmínky '%s': %s", expression, e.getMessage()));
        }
    }

    public String getExpression() {
        return expression;
    }
}
