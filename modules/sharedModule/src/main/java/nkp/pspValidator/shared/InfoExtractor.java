package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InfoExtractor {

    public static InfoData extract(File packageFile) {
        File[] infos = packageFile.listFiles(pathname -> pathname.getName().toLowerCase().startsWith("info") && pathname.getName().toLowerCase().endsWith(".xml"));
        if (infos == null || infos.length == 0) {
            return null;
        }
        return parseInfoFile(infos[0]);
    }

    private static InfoData parseInfoFile(File info) {
        try {
            Document infoDoc = loadDocument(info);
            XPathExpression xPathExpression = buildXpathIgnoringNamespaces("//info");
            Element infoEl = (Element) xPathExpression.evaluate(infoDoc, XPathConstants.NODE);
            NodeList childNodes = infoEl.getChildNodes();
            String created = null;
            String metadataversion = null;
            String packageid = null;
            List<TitleId> titleIds = new ArrayList<>();
            String collection = null;
            String institution = null;
            String creator = null;
            Long size = null;
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element childEl) {
                    String name = childEl.getNodeName();
                    String value = childEl.getTextContent();
                    switch (name) {
                        case "created" -> created = value;
                        case "metadataversion", "metadataVersion" -> metadataversion = value;
                        case "packageid", "packageId" -> packageid = value;
                        case "collection" -> collection = value;
                        case "titleid", "titleId" -> {
                            String type = childEl.getAttribute("type");
                            if (type == null || type.isEmpty()) {
                                System.out.println("WARNING: element 'titleId' nemá atribut 'type', tento element bude ignorován");
                            } else {
                                titleIds.add(new TitleId(type, value));
                            }
                        }
                        case "institution" -> institution = value;
                        case "creator" -> creator = value;
                        case "size" -> {
                            try {
                                size = Long.parseLong(value);
                            } catch (NumberFormatException e) {
                                System.out.println("WARNING: nelze převést hodnotu elementu 'size' na číslo: " + value);
                            }
                        }
                    }
                }
            }
            return new InfoData(
                    created,
                    metadataversion,
                    packageid,
                    titleIds,
                    collection,
                    institution,
                    creator,
                    size
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Document loadDocument(File file) throws XmlFileParsingException {
        try {
            return XmlUtils.buildDocumentFromFile(file, false);
        } catch (SAXException e) {
            throw new XmlFileParsingException(file, String.format("chyba parsování xml v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new XmlFileParsingException(file, String.format("chyba čtení v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new XmlFileParsingException(file, String.format("chyba konfigurace parseru při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    private static XPathExpression buildXpathIgnoringNamespaces(String xpathExpression) throws InvalidXPathExpressionException {
        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException(xpathExpression, String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }

    public static record InfoData(
            String created,
            String metadataversion,
            String packageid,
            List<TitleId> titleIds,
            String collection,
            String institution,
            String creator,
            Long size
    ) {
    }

    public static record TitleId(String type, String value) {
    }
}
