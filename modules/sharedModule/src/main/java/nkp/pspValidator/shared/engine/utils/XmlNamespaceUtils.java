package nkp.pspValidator.shared.engine.utils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cteni namespace korenoveho elementu XML souboru a targetNamespace XSD bez nacitani celeho dokumentu (StAX).
 * Pouziva se pro vyber XSD podle verze formatu (napr. ALTO 2.x/3.x/4.x maji ruzne namespace).
 */
public class XmlNamespaceUtils {

    private XmlNamespaceUtils() {
    }

    /**
     * @return namespace korenoveho elementu, prazdny retezec pokud koren namespace nema
     * @throws XMLStreamException pokud soubor neni well-formed XML (nebo nema korenovy element)
     */
    public static String getRootElementNamespace(File xmlFile) throws IOException, XMLStreamException {
        XMLInputFactory factory = newFactory();
        try (InputStream in = new FileInputStream(xmlFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                        String ns = reader.getNamespaceURI();
                        return ns == null ? "" : ns;
                    }
                }
                throw new XMLStreamException("soubor neobsahuje korenovy element");
            } finally {
                reader.close();
            }
        }
    }

    /**
     * @return hodnota atributu targetNamespace korenoveho elementu xs:schema, prazdny retezec pokud chybi
     */
    public static String getXsdTargetNamespace(File xsdFile) throws IOException, XMLStreamException {
        XMLInputFactory factory = newFactory();
        try (InputStream in = new FileInputStream(xsdFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                        String ns = reader.getAttributeValue(null, "targetNamespace");
                        return ns == null ? "" : ns;
                    }
                }
                throw new XMLStreamException("soubor neobsahuje korenovy element");
            } finally {
                reader.close();
            }
        }
    }

    private static XMLInputFactory newFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        //bez externich entit a DTD (bezpecnost, zadne sitove pristupy)
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory;
    }
}
