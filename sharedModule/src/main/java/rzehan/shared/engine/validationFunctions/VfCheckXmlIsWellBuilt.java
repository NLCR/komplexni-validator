package rzehan.shared.engine.validationFunctions;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ValidatorException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckXmlIsWellBuilt extends ValidationFunction {

    public static final String PARAM_FILE = "xml_file";


    public VfCheckXmlIsWellBuilt(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkXmlIsWellBuilt";
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        File xmlFile = (File) valueParams.getParams(PARAM_FILE).get(0).getValue();

        if (xmlFile == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILE, getName()));
        } else if (!xmlFile.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", xmlFile.getAbsoluteFile()));
        } else if (xmlFile.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", xmlFile.getAbsoluteFile()));
        } else if (!xmlFile.canRead()) {
            return new ValidationResult(false).withMessage(String.format("nelze číst soubor %s", xmlFile.getAbsoluteFile()));
        } else {
            return validate(xmlFile);
        }
    }

    private ValidationResult validate(File file) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.parse(new FileInputStream(file));
            return new ValidationResult(true);
        } catch (ParserConfigurationException e) {
            return new ValidationResult(false).withMessage(
                    String.format("ParserConfigurationException při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (SAXException e) {
            return new ValidationResult(false).withMessage(
                    String.format("Nebyl nalezen well-built xml dokument v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            return new ValidationResult(false).withMessage(
                    String.format("I/O chyba při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

}
