package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckInfoFileReferencesPrimaryMets extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";
    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckInfoFileReferencesPrimaryMets(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkInfoFileReferencesPrimaryMets";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramInfoFile = valueParams.getParams(PARAM_INFO_FILE).get(0).getEvaluation();
            File infoFile = (File) paramInfoFile.getData();
            if (infoFile == null) {
                return invalidValueParamNull(PARAM_INFO_FILE, paramInfoFile);
            } else if (infoFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(infoFile));
            } else if (!infoFile.canRead()) {
                return singlErrorResult(invalidCannotReadDir(infoFile));
            }

            ValueEvaluation paramPrimaryMetsFile = valueParams.getParams(PARAM_PRIMARY_METS_FILE).get(0).getEvaluation();
            File primaryMetsFile = (File) paramPrimaryMetsFile.getData();
            if (primaryMetsFile == null) {
                return invalidValueParamNull(PARAM_PRIMARY_METS_FILE, paramPrimaryMetsFile);
            }

            return validate(infoFile, primaryMetsFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File infoFile, File primaryMetsFile) {
        ValidationResult result = new ValidationResult();
        try {
            Document infoDoc = engine.getXmlDocument(infoFile, false);
            XPathExpression exp = engine.buildXpath("/info/mainmets");
            String primaryMetsFilenameFound = (String) exp.evaluate(infoDoc, XPathConstants.STRING);
            if (primaryMetsFilenameFound == null || primaryMetsFilenameFound.isEmpty()) {
                result.addError(invalid(Level.ERROR, "soubor INFO neobsahuje odkaz na soubor PRIMARY-METS"));
            } else if (!primaryMetsFilenameFound.equals(primaryMetsFile.getName())) {
                result.addError(invalid(Level.ERROR,
                        "nalezený název souboru PRIMARY-METS (%s) se neshoduje se skutečným názvem (%s)",
                        primaryMetsFilenameFound, primaryMetsFile.getName()));
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } finally {
            return result;
        }
    }

}
