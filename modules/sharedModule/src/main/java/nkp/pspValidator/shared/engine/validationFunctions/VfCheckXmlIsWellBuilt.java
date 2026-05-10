package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.rmi.ServerError;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckXmlIsWellBuilt extends ValidationFunction {

    public static final String PARAM_XML_FILE = "xml_file";


    public VfCheckXmlIsWellBuilt(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_XML_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramXmlFile = valueParams.getParams(PARAM_XML_FILE).get(0).getEvaluation();
            File xmlFile = (File) paramXmlFile.getData();
            if (xmlFile == null) {
                //docela hack, protože ta chyba probublává a mohla by tam být jiná chyba volání checkXmlIsWellBuilt, např. použítí nedefinované reference na soubor
                ValidationResult result = new ValidationResult();
                result.addError(new ValidationProblem(Level.ERROR, String.format("chyba parsování xml: %s ", paramXmlFile.getErrorMessage()))
                        .withSimpleMessage("chyba parsování xml")
                        .withErrorDetails(paramXmlFile.getErrorMessage())
                );
                return result;
                //return invalidValueParamNull(PARAM_XML_FILE, paramXmlFile);
            } else if (!xmlFile.exists()) {
                return singlErrorResult(invalidFileDoesNotExist(xmlFile));
            } else if (xmlFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(xmlFile));
            } else if (!xmlFile.canRead()) {
                return singlErrorResult(invalidCannotReadFile(xmlFile));
            }

            return validate(xmlFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file) {
        ValidationResult result = new ValidationResult();
        try {
            XmlUtils.buildDocumentFromFile(file, false);
        } catch (ParserConfigurationException e) {
            result.addError(invalid(Level.ERROR, file, "ParserConfigurationException při zpracování souboru: %s", e.getMessage()));
        } catch (SAXException e) {
            result.addError(invalid(Level.ERROR, file, "Nebyl nalezen well-built xml dokument v souboru: %s", e.getMessage()));
        } catch (IOException e) {
            result.addError(invalid(Level.ERROR, file, "I/O chyba při zpracování souboru: %s", e.getMessage()));
        }
        return result;
    }

}
