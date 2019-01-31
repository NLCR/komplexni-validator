package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckDcIdentifiersDoNotContainWhiteSpaces extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckDcIdentifiersDoNotContainWhiteSpaces(String name, Engine engine) {
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
            XPathExpression xpath = engine.buildXpath("//dc:identifier");
            NodeList nodes = (NodeList) xpath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String identifier = node.getTextContent();
                if (identifier.matches(".*\\s.*")) {
                    result.addError(invalid(Level.WARNING, "identifikátor '%s' obsahuje bílé znaky", identifier));
                }
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
