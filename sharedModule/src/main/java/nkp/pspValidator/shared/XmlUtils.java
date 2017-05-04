package nkp.pspValidator.shared;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 31.10.16.
 */
public class XmlUtils {


    public static List<Element> getChildrenElementsByName(Element root, String name) {
        NodeList nodes = root.getChildNodes();
        List<Element> result = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getTagName().equals(name)) {
                    result.add(element);
                }
            }
        }
        return result;
    }


    public static Element getFirstChildElementsByName(Element osEl, String name) {
        List<Element> list = getChildrenElementsByName(osEl, name);
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static String toString(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalArgumentException("no xml", e);
        }
    }

    public static Document buildDocumentFromFile(File file, boolean nsAware) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(nsAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file.getAbsoluteFile());
    }

    public static Document buildDocumentFromString(String string, boolean nsAware) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(nsAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(string));
        return builder.parse(is);
    }


    public static List<Element> getChildrenElements(Element element) {
        NodeList nodes = element.getChildNodes();
        List<Element> result = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) node);
            }
        }
        return result;
    }

    public static String getDirectTextContent(Element element) {
        StringBuilder builder = new StringBuilder();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                Text textNode = (Text) node;
                builder.append(textNode.getWholeText());
            }
        }
        return builder.toString();
    }

    public static String elementToXml(Element element) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }

    public static Document elementToNewDocument(Element element, boolean nsAware) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(nsAware);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDocument = builder.newDocument();
        Node importedNode = newDocument.importNode(element, true);
        newDocument.appendChild(importedNode);
        return newDocument;
    }


}
