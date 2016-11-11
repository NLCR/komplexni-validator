package rzehan.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
public class VfCheckPrimaryMetsFilesecContainsAllFilegroups extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckPrimaryMetsFilesecContainsAllFilegroups(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkPrimaryMetsFilesecContainsAllFilegroups";
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
                return invalidFileIsDir(file);
            } else if (!file.canRead()) {
                return invalidCannotReadDir(file);
            }

            return validate(file);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file) {
        try {
            Document doc = engine.getXmlDocument(file);
            checkFileGroupOk(doc, "MC_IMGGRP", "Images");
            checkFileGroupOk(doc, "UC_IMGGRP", "Images");
            checkFileGroupOk(doc, "ALTOGRP", "Layout");
            checkFileGroupOk(doc, "TXTGRP", "Text");
            checkFileGroupOk(doc, "TECHMDGRP", "Technical Metadata");
            checkFilegroupsCount(doc, 5);
            return valid();
        } catch (XmlParsingException e) {
            return invalid(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (XPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (InvalidDataException e) {
            return invalid(e.getMessage());
        }
    }

    private void checkFilegroupsCount(Document doc, int expectedCount) throws InvalidXPathExpressionException, XPathExpressionException, InvalidDataException {
        XPathExpression xpath = engine.buildXpath("count(/mets:mets/mets:fileSec/mets:fileGrp)");
        int count = Integer.valueOf((String) xpath.evaluate(doc, XPathConstants.STRING));
        if (count != expectedCount) {
            throw new InvalidDataException(String.format("neočekávaný počet elementů mets:fileGrp"));
        }
    }

    private void checkFileGroupOk(Document doc, String id, String useExpected) throws InvalidXPathExpressionException, XPathExpressionException, InvalidDataException {
        XPathExpression xpath = engine.buildXpath("/mets:mets/mets:fileSec/mets:fileGrp[@ID='" + id + "']");
        Element fileGrpEl = (Element) xpath.evaluate(doc, XPathConstants.NODE);
        if (fileGrpEl == null) {
            throw new InvalidDataException(String.format("nenalezen element mets:fileGrp s atributem ID=\"%s\"", id));
        } else {
            String useFound = fileGrpEl.getAttribute("USE");
            if (!useExpected.equals(useFound)) {
                throw new InvalidDataException(String.format(
                        "element mets:fileGrp s atributem ID=\"%s\" obsahuje nepovolenou hodnotu atributu USE: '%s' namísto očekávané '%s'", id, useFound, useExpected));
            }
        }
    }

    public static class InvalidDataException extends Exception {
        public InvalidDataException(String message) {
            super(message);
        }
    }

}
