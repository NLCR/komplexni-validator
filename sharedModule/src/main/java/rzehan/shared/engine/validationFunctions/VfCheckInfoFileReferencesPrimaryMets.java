package rzehan.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.exceptions.InvalidXPathExpressionException;
import rzehan.shared.engine.exceptions.XmlParsingException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by martin on 1.11.16.
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
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramInfoFile = valueParams.getParams(PARAM_INFO_FILE).get(0).getEvaluation();
        File infoFile = (File) paramInfoFile.getData();
        if (infoFile == null) {
            return invalidValueParamNull(PARAM_INFO_FILE, paramInfoFile);
        } else if (infoFile.isDirectory()) {
            return invalidFileIsDir(infoFile);
        } else if (!infoFile.canRead()) {
            return invalidCannotReadDir(infoFile);
        }

        ValueEvaluation paramPrimaryMetsFile = valueParams.getParams(PARAM_PRIMARY_METS_FILE).get(0).getEvaluation();
        File primaryMetsFile = (File) paramPrimaryMetsFile.getData();
        if (primaryMetsFile == null) {
            return invalidValueParamNull(PARAM_PRIMARY_METS_FILE, paramPrimaryMetsFile);
        }

        return validate(infoFile, primaryMetsFile);
    }

    private ValidationResult validate(File infoFile, File primaryMetsFile) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile);
            XPathExpression exp = engine.buildXpath("/info/mainmets");
            String primaryMetsFilenameFound = (String) exp.evaluate(infoDoc, XPathConstants.STRING);
            if (primaryMetsFilenameFound == null || primaryMetsFilenameFound.isEmpty()) {
                return invalid("soubor INFO neobsahuje odkaz na soubor PRIMARY-METS");
            } else if (!primaryMetsFilenameFound.equals(primaryMetsFile.getName())) {
                return invalid(String.format("nalezený název souboru PRIMARY-METS (%s) se neshoduje se skutečným názvem (%s)", primaryMetsFilenameFound, primaryMetsFile.getName()));
            } else {
                return valid();
            }
        } catch (XmlParsingException e) {
            return invalid(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (XPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (Throwable e) {
            return invalid("neočekávaná chyba: " + e.getMessage());
        }
    }

}
