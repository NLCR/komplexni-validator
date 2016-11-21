package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by martin on 2.11.16.
 */
public class XmlManager {

    //TODO: na realnem provozu (vsechna pravidla hotova) otestovat, jestli ma vubec cache vyznam
    private final LRUCache<String, Document> docCache;
    private final NamespaceContextImpl namespaceContext = new NamespaceContextImpl();

    public XmlManager(boolean withDocumentCache) {
        if (withDocumentCache) {
            docCache = new LRUCache<>(20);
        } else {
            docCache = null;
        }
    }

    public void setNamespaceUri(String prefix, String uri) {
        namespaceContext.setNamespace(prefix, uri);
    }


    public static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private int cacheSize;

        public LRUCache(int cacheSize) {
            super(16, 0.75f, true);
            this.cacheSize = cacheSize;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() >= cacheSize;
        }
    }

    public Document getXmlDocument(File file) throws XmlParsingException {
        if (docCache != null) {
            String key = file.getAbsolutePath();
            if (docCache.containsKey(key)) {
                return docCache.get(key);
            } else {
                Document doc = loadDocument(file);
                docCache.put(key, doc);
                return doc;
            }
        } else {
            return loadDocument(file);
        }
    }

    private Document loadDocument(File file) throws XmlParsingException {
        try {
            return XmlUtils.buildDocumentFromFile(file, true);
        } catch (SAXException e) {
            throw new XmlParsingException(file, String.format("chyba parsování xml v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new XmlParsingException(file, String.format("chyba čtení v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new XmlParsingException(file, String.format("chyba konfigurace parseru při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }


    public XPathExpression buildXpath(String xpathExpression) throws InvalidXPathExpressionException {
        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(namespaceContext);
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException(xpathExpression, String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }

}
