package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.externalUtils.ExtractionResultType;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

/**
 * Všechny neprázdné hodnoty ze všech XPath (v pořadí výrazů) jako jeden seznam, např. všechny typy médií
 * nebo všechny hlášky EPUBCheck. Prázdný seznam, když nic nenajde.
 */
public class AllNonemptyByXpathDataExctraction extends XmlDataExtraction {

    private final List<String> paths;

    public AllNonemptyByXpathDataExctraction(ExtractionResultType extractionResultType, NamespaceContext namespaceContext, List<String> paths) {
        super(extractionResultType, namespaceContext);
        this.paths = paths;
    }

    @Override
    public List<Object> extract(Object processedOutput) throws ExtractionException {
        String pathForError = null;
        try {
            List<Object> allMatches = new ArrayList<>();
            for (String path : paths) {
                pathForError = path;
                XPathExpression xPath = buildXpath(path);
                Object extractedData = extractData(xPath, processedOutput);
                if (extractedData instanceof List) {
                    for (Object item : (List) extractedData) {
                        if (item != null && !item.toString().isEmpty()) {
                            allMatches.add(item);
                        }
                    }
                } else if (extractedData != null && !extractedData.toString().isEmpty()) {
                    allMatches.add(extractedData);
                }
            }
            return allMatches;
        } catch (ExtractionException e) {
            throw e;
        } catch (XPathExpressionException e) {
            throw new ExtractionException(String.format("chyba v zápisu Xpath '%s': %s", pathForError, e.getMessage()));
        } catch (Throwable e) {
            throw new ExtractionException(e);
        }
    }
}
