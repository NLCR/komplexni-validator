package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;


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

        ValueEvaluation paramXsdFile = valueParams.getParams(PARAM_XSD_FILE).get(0).getValueEvaluation();
        File xsdFile = (File) paramXsdFile.getData();
        if (xsdFile == null) {
        } else if (!xsdFile.exists()) {
            return invalidFileDoesNotExist(xsdFile);
        } else if (xsdFile.isDirectory()) {
            return invalidFileIsDir(xsdFile);
        } else if (!xsdFile.canRead()) {
            return invalidCannotReadFile(xsdFile);
        }

        /*TODO: implement*/
        return invalid("TODO: implement");
    }

}
