package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckXmlIsValidByXsd extends ValidationFunction {

    public static final String PARAM_FILE = "file";
    public static final String PARAM_XSD_FILE = "xsd_file";


    public VfCheckXmlIsValidByXsd(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_XSD_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkXmlIsValidByXsd";
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        File file = (File) valueParams.getParams(PARAM_FILE).get(0).getValue();
        File xsdFile = (File) valueParams.getParams(PARAM_XSD_FILE).get(0).getValue();

        if (file == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILE, getName()));
        } else if (!file.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", file.getAbsoluteFile()));
        } else if (file.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", file.getAbsoluteFile()));
        } else if (!file.canRead()) {
            return new ValidationResult(false).withMessage(String.format("nelze číst soubor %s", file.getAbsoluteFile()));
        } else if (xsdFile == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_XSD_FILE, getName()));
        } else if (!xsdFile.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", xsdFile.getAbsoluteFile()));
        } else if (xsdFile.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", xsdFile.getAbsoluteFile()));
        } else if (!xsdFile.canRead()) {
            return new ValidationResult(false).withMessage(String.format("nelze číst soubor %s", xsdFile.getAbsoluteFile()));
        } else {
            /*TODO: implement*/
            return new ValidationResult(false).withMessage("TODO: implement");
        }
    }

}
