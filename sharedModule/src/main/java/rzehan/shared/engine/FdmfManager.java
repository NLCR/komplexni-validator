package rzehan.shared.engine;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rzehan.shared.engine.exceptions.InvalidXPathExpressionException;
import rzehan.shared.engine.exceptions.PspDataException;
import rzehan.shared.engine.exceptions.XmlParsingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by martin on 2.11.16.
 */
public class FdmfManager {


    public static enum DmfType {
        MONOGRAPH, PERIODICAL;
    }


    /**
     * Validátor zkontroluje hlavní mets soubor, konkrétně kořenový element <mets:mets> na hodnotu atributu TYPE. Možné hodnoty jsou „Monograph“ nebo „Periodical“. Platí:
     * Pokud nenalezne atribut TYPE – chyba.
     * Pokud se vyskytne jiná než povolená hodnota atributu – chyba.
     * Pokud se vyskytuje hodnota „Monograph“, zachází validátor s balíčkem jako s monografií.
     * Pokud se vyskytuje hodnota „Periodical“, zachází validátor s balíčkem jako s periodikem.
     */
    public DmfType detectDmfType(File pspRootDir) throws PspDataException, XmlParsingException, InvalidXPathExpressionException {

        try {
            File primaryMetsFile = findPrimaryMetsFile(pspRootDir);
            Document metsDoc = loadDocument(primaryMetsFile);
            XPathExpression xPathExpression = buildXpathIgnoringNamespaces("/mets/@TYPE");
            String docType = (String) xPathExpression.evaluate(metsDoc, XPathConstants.STRING);
            if ("Monograph".equals(docType)) {
                return DmfType.MONOGRAPH;
            } else if ("Periodical".equals(docType)) {
                return DmfType.PERIODICAL;
            } else {
                throw new PspDataException(pspRootDir, String.format("atribut TYPE elementu mods neobsahuje korektní typ (Monograph/Periodical), ale hodnotu '%s'", docType));
            }
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException("", String.format("chyba v zápisu Xpath: %s", e.getMessage()));
        }
    }

    private File findPrimaryMetsFile(File pspRootDir) throws PspDataException {
        Pattern pattern = Pattern.compile("mets.*\\.xml", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

        File[] metsCandidates = pspRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });
        if (metsCandidates.length >= 2) {
            throw new PspDataException(pspRootDir,
                    String.format("nalezeno více možných souborů PRIMARY-METS, není jasné, který použít pro zjištění typu dokumentu (monografie/periodikum)"));
        } else if (metsCandidates.length == 0) {
            throw new PspDataException(pspRootDir,
                    String.format("nenalezen soubor PRIMARY-METS pro zjištění typu dokumentu (monografie/periodikum)"));
        } else {
            return metsCandidates[0];
        }
    }

    private Document loadDocument(File file) throws XmlParsingException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file.getAbsoluteFile());
        } catch (SAXException e) {
            throw new XmlParsingException(file, String.format("chyba parsování xml v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new XmlParsingException(file, String.format("chyba čtení v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new XmlParsingException(file, String.format("chyba konfigurace parseru při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    private XPathExpression buildXpathIgnoringNamespaces(String xpathExpression) throws InvalidXPathExpressionException {
        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException(xpathExpression, String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }


}
