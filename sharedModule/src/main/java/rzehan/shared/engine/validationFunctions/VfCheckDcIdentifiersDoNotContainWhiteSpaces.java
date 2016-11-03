package rzehan.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
public class VfCheckDcIdentifiersDoNotContainWhiteSpaces extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primaryMets_file";

    public VfCheckDcIdentifiersDoNotContainWhiteSpaces(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkDcIdentifiersDoNotContainWhiteSpaces";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

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
    }

    private ValidationResult validate(File file) {
        try {
            Document doc = engine.getXmlDocument(file);
            XPathExpression xpath = engine.buildXpath("//dc:identifier");
            NodeList nodes = (NodeList) xpath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String identifier = node.getTextContent();
                if (identifier.matches(".*\\s.*")) {
                    return invalid(String.format("identifikátor '%s' obsahuje bílé znaky", identifier));
                }
            }
            return valid();
        } catch (XmlParsingException e) {
            return invalid(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (XPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (Throwable e) {
            //TODO: tohle u absolutne kazdeho pravidla, muze byt nejake xml nevalidni, tak at spadne jenom pravidlo, ne vsechno
            //plati pro validacni i vyhodnocovaci pravidla
            return invalid("neočekávaná chyba: " + e.getMessage());
        }
    }

}
