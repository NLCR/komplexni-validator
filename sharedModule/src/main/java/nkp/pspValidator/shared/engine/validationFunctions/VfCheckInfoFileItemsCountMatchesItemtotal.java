package nkp.pspValidator.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by martin on 1.11.16.
 */
public class VfCheckInfoFileItemsCountMatchesItemtotal extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";

    public VfCheckInfoFileItemsCountMatchesItemtotal(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkInfoFileItemsCountMatchesItemtotal";
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
                return invalidFileIsDir(infoFile);
            } else if (!infoFile.canRead()) {
                return invalidCannotReadDir(infoFile);
            }

            return validate(infoFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File infoFile) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile);
            XPathExpression itemTotalExp = engine.buildXpath("/info/itemlist/@itemtotal");
            Integer itemTotal = Integer.valueOf((String) itemTotalExp.evaluate(infoDoc, XPathConstants.STRING));
            XPathExpression itemsExp = engine.buildXpath("count(/info/itemlist/item)");
            Integer items = Integer.valueOf((String) itemsExp.evaluate(infoDoc, XPathConstants.STRING));

            if (items != itemTotal) {
                return invalid(String.format("počet elementů item (%s) nesouhlasí s obsahem atributu itemtotal (%s)", items, itemTotal));
            } else {
                return valid();
            }
        } catch (XmlParsingException e) {
            return invalid(e);
        } catch (InvalidXPathExpressionException e) {
            return invalid(e);
        } catch (XPathExpressionException e) {
            return invalid(e);
        }
    }

}
