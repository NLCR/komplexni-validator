package rzehan.shared.engine.validationFunctions;

import org.xml.sax.SAXException;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckXmlIsWellBuilt extends ValidationFunction {

    public static final String PARAM_XML_FILE = "xml_file";


    public VfCheckXmlIsWellBuilt(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_XML_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkXmlIsWellBuilt";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramXmlFile = valueParams.getParams(PARAM_XML_FILE).get(0).getValueEvaluation();
        File xmlFile = (File) paramXmlFile.getData();
        if (xmlFile == null) {
            return invalidParamNull(PARAM_XML_FILE, paramXmlFile);
        } else if (!xmlFile.exists()) {
            return invalidFileDoesNotExist(xmlFile);
        } else if (xmlFile.isDirectory()) {
            return invalidFileIsDir(xmlFile);
        } else if (!xmlFile.canRead()) {
            return invalidCannotReadFile(xmlFile);
        }

        return validate(xmlFile);
    }

    private ValidationResult validate(File file) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.parse(new FileInputStream(file));
            return valid();
        } catch (ParserConfigurationException e) {
            return invalid(String.format("ParserConfigurationException při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (SAXException e) {
            return invalid(String.format("Nebyl nalezen well-built xml dokument v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            return invalid(String.format("I/O chyba při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

}
