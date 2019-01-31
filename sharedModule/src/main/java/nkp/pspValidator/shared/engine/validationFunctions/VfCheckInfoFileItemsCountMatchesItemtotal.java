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
public class VfCheckInfoFileItemsCountMatchesItemtotal extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";

    public VfCheckInfoFileItemsCountMatchesItemtotal(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
        );
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

            return validate(infoFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File infoFile) {
        ValidationResult result = new ValidationResult();
        try {
            Document infoDoc = engine.getXmlDocument(infoFile, false);
            Integer itemTotalLc = Integer.valueOf((String) engine.buildXpath("/info/itemlist/@itemtotal").evaluate(infoDoc, XPathConstants.STRING));
            Integer itemTotalUc = Integer.valueOf((String) engine.buildXpath("/info/itemlist/@ITEMTOTAL").evaluate(infoDoc, XPathConstants.STRING));
            Integer itemTotal = null;
            if (itemTotalLc != null) {
                itemTotal = itemTotalLc;
            } else if (itemTotalUc != null) {
                itemTotal = itemTotalUc;
            }
            XPathExpression itemsExp = engine.buildXpath("count(/info/itemlist/item)");
            Integer items = Integer.valueOf((String) itemsExp.evaluate(infoDoc, XPathConstants.STRING));
            if (items != itemTotal) {
                return singlErrorResult(invalid(Level.ERROR, "počet elementů item (%s) nesouhlasí s obsahem atributu itemtotal (%s)", items, itemTotal));
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
