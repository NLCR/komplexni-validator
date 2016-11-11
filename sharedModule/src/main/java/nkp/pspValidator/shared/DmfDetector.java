package nkp.pspValidator.shared;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import static nkp.pspValidator.shared.Dmf.Type.MONOGRAPH;
import static nkp.pspValidator.shared.Dmf.Type.PERIODICAL;

/**
 * Created by martin on 2.11.16.
 */
public class DmfDetector {


    public static final String DEFAULT_MONOGRAPH_VERSION = "1.0";
    public static final String DEFAULT_PERIODICAL_VERSION = "1.4";


    /**
     * Validátor zkontroluje hlavní mets soubor, konkrétně kořenový element <mets:mets> na hodnotu atributu TYPE. Možné hodnoty jsou „Monograph“ nebo „Periodical“. Platí:
     * Pokud nenalezne atribut TYPE – chyba.
     * Pokud se vyskytne jiná než povolená hodnota atributu – chyba.
     * Pokud se vyskytuje hodnota „Monograph“, zachází validátor s balíčkem jako s monografií.
     * Pokud se vyskytuje hodnota „Periodical“, zachází validátor s balíčkem jako s periodikem.
     */
    public Dmf.Type detectDmfType(File pspRootDir) throws PspDataException, XmlParsingException, InvalidXPathExpressionException {

        try {
            File primaryMetsFile = findPrimaryMetsFile(pspRootDir);
            Document metsDoc = loadDocument(primaryMetsFile);
            XPathExpression xPathExpression = buildXpathIgnoringNamespaces("/mets/@TYPE");
            String docType = ((String) xPathExpression.evaluate(metsDoc, XPathConstants.STRING)).trim();
            if ("Monograph".equals(docType)) {
                return MONOGRAPH;
            } else if ("Periodical".equals(docType)) {
                return PERIODICAL;
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


    /**
     * Validátor zkontroluje soubor info_{identifikator}.xml, konkrétně element <metadataversion>. Platí:
     * Pokud element nenalezne, předpokládá, že je balíček zpracován podle starších DMF, než je 1.5 pro periodika a 1.1 pro monografie (a validuje podle 1.4 pro periodika a 1.0 pro monografie).
     * Pokud element nalezne, podívá se na jeho hodnotu a validuje podle této hodnoty.
     */
    public String detectDmfVersion(Dmf.Type dmfType, File pspRootDir) throws PspDataException, XmlParsingException, InvalidXPathExpressionException {

        try {
            File infoFile = findInfoFile(pspRootDir);
            Document infoDoc = loadDocument(infoFile);
            XPathExpression xPathExpression = buildXpathIgnoringNamespaces("/info/metadataversion");
            String versionFound = ((String) xPathExpression.evaluate(infoDoc, XPathConstants.STRING)).trim();
            if (versionFound == null || versionFound.isEmpty()) {
                switch (dmfType) {
                    case MONOGRAPH:
                        return DEFAULT_MONOGRAPH_VERSION;
                    case PERIODICAL:
                        return DEFAULT_PERIODICAL_VERSION;
                    default:
                        throw new IllegalStateException();
                }
            } else {
                return versionFound;
            }
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException("", String.format("chyba v zápisu Xpath: %s", e.getMessage()));
        }
    }

    private File findInfoFile(File pspRootDir) throws PspDataException {
        Pattern pattern = Pattern.compile("info.*\\.xml", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

        File[] infoCandidates = pspRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });
        if (infoCandidates.length >= 2) {
            throw new PspDataException(pspRootDir,
                    String.format("nalezeno více možných souborů INFO, není jasné, který použít pro zjištění verze standardu DMF"));
        } else if (infoCandidates.length == 0) {
            throw new PspDataException(pspRootDir,
                    String.format("nenalezen soubor INFO pro zjištění verze standardu DMF"));
        } else {
            return infoCandidates[0];
        }
    }

    public Dmf resolveDmf(Dmf dmfPrefered, File pspRoot) throws PspDataException, InvalidXPathExpressionException, XmlParsingException {
        if (dmfPrefered == null) {//both missing (wrapper is null)
            Dmf.Type type = detectDmfType(pspRoot);
            String version = detectDmfVersion(type, pspRoot);
            return new Dmf(type, version);
        } else {
            if (dmfPrefered.getType() != null && dmfPrefered.getVersion() != null) { //both present
                return dmfPrefered;
            } else if (dmfPrefered.getType() == null && dmfPrefered.getVersion() == null) { //both missing
                Dmf.Type type = detectDmfType(pspRoot);
                String version = detectDmfVersion(type, pspRoot);
                return new Dmf(type, version);
            } else if (dmfPrefered.getType() == null) { //only type missing
                //TODO: logger
                System.err.println(String.format("Požadována verze DMF (%s), ale neuveden typ (monografie/periodikum). Požadovanou verzi ignoruji.", dmfPrefered.getVersion()));
                Dmf.Type type = detectDmfType(pspRoot);
                String version = detectDmfVersion(type, pspRoot);
                return new Dmf(type, version);
            } else { //only version missing
                String version = detectDmfVersion(dmfPrefered.getType(), pspRoot);
                return new Dmf(dmfPrefered.getType(), version);
            }
        }
    }

}
