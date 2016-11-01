package rzehan.shared.engine.validationFunctions;

import org.xml.sax.SAXException;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckXmlIsValidByXsd extends ValidationFunction {

    public static final String PARAM_XML_FILE = "xml_file";
    public static final String PARAM_XSD_FILE = "xsd_file";


    public VfCheckXmlIsValidByXsd(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_XML_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_XSD_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkXmlIsValidByXsd";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramXmlFile = valueParams.getParams(PARAM_XML_FILE).get(0).getEvaluation();
        File xmlFile = (File) paramXmlFile.getData();
        if (xmlFile == null) {
            return invalidValueParamNull(PARAM_XML_FILE, paramXmlFile);
        } else if (!xmlFile.exists()) {
            return invalidFileDoesNotExist(xmlFile);
        } else if (xmlFile.isDirectory()) {
            return invalidFileIsDir(xmlFile);
        } else if (!xmlFile.canRead()) {
            return invalidCannotReadFile(xmlFile);
        }

        ValueEvaluation paramXsdFile = valueParams.getParams(PARAM_XSD_FILE).get(0).getEvaluation();
        File xsdFile = (File) paramXsdFile.getData();
        if (xsdFile == null) {
        } else if (!xsdFile.exists()) {
            return invalidFileDoesNotExist(xsdFile);
        } else if (xsdFile.isDirectory()) {
            return invalidFileIsDir(xsdFile);
        } else if (!xsdFile.canRead()) {
            return invalidCannotReadFile(xsdFile);
        }

        return validate(xmlFile, xsdFile);
    }

    private ValidationResult validate(File xmlFileF, File xsdFile) {
        try {
            Source xmlFile = new StreamSource(xmlFileF);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.validate(xmlFile);
            return valid();
        } catch (SAXException e) {
            return invalid(String.format("obsah souboru %s není validní podle Xml schema ze souboru %s: %s",
                    xmlFileF.getAbsolutePath(), xsdFile.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            return invalid(String.format("I/O chyba při čtení souboru %s: %s",
                    xmlFileF.getAbsolutePath(), e.getMessage()));
        }
    }

}
