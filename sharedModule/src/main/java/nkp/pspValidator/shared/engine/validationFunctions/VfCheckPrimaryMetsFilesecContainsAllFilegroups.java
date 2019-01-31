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

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckPrimaryMetsFilesecContainsAllFilegroups extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckPrimaryMetsFilesecContainsAllFilegroups(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramEvaluation = valueParams.getParams(PARAM_PRIMARY_METS_FILE).get(0).getEvaluation();
            File file = (File) paramEvaluation.getData();
            if (file == null) {
                return invalidValueParamNull(PARAM_PRIMARY_METS_FILE, paramEvaluation);
            } else if (file.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(file));
            } else if (!file.canRead()) {
                return singlErrorResult(invalidCannotReadDir(file));
            }

            return validate(file);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(file, true);
            checkFileGroupOk(doc, "MC_IMGGRP", "Images", result);
            checkFileGroupOk(doc, "UC_IMGGRP", "Images", result);
            checkFileGroupOk(doc, "ALTOGRP", "Layout", result);
            checkFileGroupOk(doc, "TXTGRP", "Text", result);
            checkFileGroupOk(doc, "TECHMDGRP", "Technical Metadata", result);
            checkFilegroupsCount(doc, 5, result);
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
