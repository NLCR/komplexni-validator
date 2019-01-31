package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.List;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckSecondaryMetsFilesecContainsAllFilegroups extends ValidationFunction {

    public static final String PARAM_SECONDARY_METS_FILES = "secondary-mets_files";

    public VfCheckSecondaryMetsFilesecContainsAllFilegroups(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_SECONDARY_METS_FILES, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramEvaluation = valueParams.getParams(PARAM_SECONDARY_METS_FILES).get(0).getEvaluation();
            List<File> files = (List<File>) paramEvaluation.getData();
            if (files == null) {
                return invalidValueParamNull(PARAM_SECONDARY_METS_FILES, paramEvaluation);
            }

            return validate(files);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<File> files) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            if (file.isDirectory()) {
                result.addError(invalidFileIsDir(file));
            } else if (!file.canRead()) {
                result.addError(invalidCannotReadDir(file));
            } else {
                checkFileValid(file, result);
            }
        }
        return result;
    }

    private void checkFileValid(File file, ValidationResult result) {
        try {
            Document doc = engine.getXmlDocument(file, true);
            checkFileGroupOk(doc, "MC_IMGGRP", "Images", result);
            checkFileGroupOk(doc, "UC_IMGGRP", "Images", result);
            checkFileGroupOk(doc, "ALTOGRP", "Layout", result);
            checkFileGroupOk(doc, "TXTGRP", "Text", result);
            checkFilegroupsCount(doc, 4, result);
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        }
    }

    private void checkFilegroupsCount(Document doc, int expectedCount, ValidationResult result) throws InvalidXPathExpressionException, XPathExpressionException {
        XPathExpression xpath = engine.buildXpath("count(/mets:mets/mets:fileSec/mets:fileGrp)");
        int count = Integer.valueOf((String) xpath.evaluate(doc, XPathConstants.STRING));
        if (count != expectedCount) {
            result.addError(invalid(Level.ERROR, "neočekávaný počet elementů mets:fileGrp (%d namísto %d)", count, expectedCount));
        }
    }

    private void checkFileGroupOk(Document doc, String id, String useExpected, ValidationResult result) throws InvalidXPathExpressionException, XPathExpressionException {
        XPathExpression xpath = engine.buildXpath("/mets:mets/mets:fileSec/mets:fileGrp[@ID='" + id + "']");
        Element fileGrpEl = (Element) xpath.evaluate(doc, XPathConstants.NODE);
        if (fileGrpEl == null) {
            result.addError(invalid(Level.ERROR, "nenalezen element mets:fileGrp s atributem ID=\"%s\"", id));
        } else {
            String useFound = fileGrpEl.getAttribute("USE");
            if (!useExpected.equals(useFound)) {
                result.addError(invalid(Level.ERROR,
                        "element mets:fileGrp s atributem ID=\"%s\" obsahuje nepovolenou hodnotu atributu USE: '%s' namísto očekávané '%s'",
                        id, useFound, useExpected));
            }
        }
    }

}
