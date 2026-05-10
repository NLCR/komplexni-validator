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
                return singlErrorResult(invalidCannotReadFile(infoFile));
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
            Integer itemTotal = extractItemTotal(infoDoc, result);
            if (itemTotal == null) {
                result.addError(new ValidationProblem(Level.ERROR, "nenalezen atribut itemtotal/ITEMTOTAL v elementu itemlist")
                        .withFile(infoFile)
                );
            }

            Integer itemsSize = extractItemsSize(infoDoc, result);
            if (itemsSize == null) {
                result.addError(new ValidationProblem(Level.ERROR, "nepodařilo se zjistit počet elementů itemlist/item v info souboru")
                        .withFile(infoFile)
                );
            }

            if (itemsSize != null && itemTotal != null) {
                if (!itemsSize.equals(itemTotal)) {
                    //return singlErrorResult(invalid(Level.ERROR, infoFile, "počet elementů item (%s) nesouhlasí s obsahem atributu itemtotal (%s)", items, itemTotal));
                    result.addError(new ValidationProblem(Level.ERROR, String.format("počet elementů item (%s) nesouhlasí s obsahem atributu itemtotal/ITEMTOTAL (%s)", itemsSize, itemTotal))
                            .withSimpleMessage("počet elementů item nesouhlasí s obsahem atributu itemtotal/ITEMTOTAL")
                            .withExpectedAndActualValues(itemsSize.toString(), itemTotal.toString())
                            .withFile(infoFile)
                    );
                }
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        }
        return result;
    }

    private Integer extractItemsSize(Document infoDoc, ValidationResult result) {
        try {
            XPathExpression itemsExp = engine.buildXpath("count(/info/itemlist/item)");
            return Integer.valueOf((String) itemsExp.evaluate(infoDoc, XPathConstants.STRING));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (Throwable e) {
            result.addError(new ValidationProblem(Level.ERROR, e.getMessage()));
        }
        return null;
    }

    private Integer extractItemTotal(Document infoDoc, ValidationResult result) {
        try {
            //itemtotal
            String itemTotalLcStr = (String) engine.buildXpath("/info/itemlist/@itemtotal").evaluate(infoDoc, XPathConstants.STRING);
            if (itemTotalLcStr != null) {
                try {
                    return Integer.valueOf(itemTotalLcStr);
                } catch (NumberFormatException e) {
                    result.addError(new ValidationProblem(Level.ERROR, "neplatný formát čísla v atributu itemtotal: " + itemTotalLcStr)
                            .withSimpleMessage("neplatný formát čísla v atributu itemtotal")
                            .withReferencedValue(itemTotalLcStr)
                    );
                }
            }

            //ITEMTOTAL
            String itemTotaLUcStr = (String) engine.buildXpath("/info/itemlist/@ITEMTOTAL").evaluate(infoDoc, XPathConstants.STRING);
            if (itemTotaLUcStr != null && !itemTotaLUcStr.isEmpty()) {
                try {
                    return Integer.valueOf(itemTotaLUcStr);
                } catch (NumberFormatException e) {
                    result.addError(new ValidationProblem(Level.ERROR, "neplatný formát čísla v atributu ITEMTOTAL: " + itemTotaLUcStr)
                            .withSimpleMessage("neplatný formát čísla v atributu ITEMTOTAL")
                            .withReferencedValue(itemTotaLUcStr)
                    );
                }
            }

        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (Throwable e) {
            result.addError(new ValidationProblem(Level.ERROR, e.getMessage()));
        }
        return null;
    }

}
