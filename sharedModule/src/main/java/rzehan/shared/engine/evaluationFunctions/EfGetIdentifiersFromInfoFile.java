package rzehan.shared.engine.evaluationFunctions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.exceptions.InvalidXPathExpressionException;
import rzehan.shared.engine.exceptions.XmlParsingException;
import rzehan.shared.engine.types.Identifier;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfGetIdentifiersFromInfoFile extends EvaluationFunction {

    private static final String PARAM_INFO_FILE = "info_file";

    public EfGetIdentifiersFromInfoFile(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "getIdentifiersFromInfoFile";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        }

        ValueEvaluation paramInfoFile = valueParams.getParams(PARAM_INFO_FILE).get(0).getEvaluation();
        File infoFile = (File) paramInfoFile.getData();
        if (infoFile == null) {
            return errorResultParamNull(PARAM_INFO_FILE, paramInfoFile);
        } else if (!infoFile.exists()) {
            return errorResultFileDoesNotExist(infoFile);
        } else if (infoFile.isDirectory()) {
            return errorResultFileIsDir(infoFile);
        } else if (!infoFile.canRead()) {
            return errorResultCannotReadFile(infoFile);
        } else {
            return evaluate(infoFile);
        }
    }

    private ValueEvaluation evaluate(File infoFile) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile);
            XPathExpression exp = engine.buildXpath("/info/titleid");
            NodeList nodes = (NodeList) exp.evaluate(infoDoc, XPathConstants.NODESET);
            List<Identifier> identifiers = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String type = element.getAttribute("type").trim();
                String value = element.getTextContent().trim();
                identifiers.add(new Identifier(type, value));
            }
            return okResult(identifiers);
        } catch (XmlParsingException e) {
            return errorResult(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            return errorResult(e.getMessage());
        } catch (XPathExpressionException e) {
            return errorResult(e.getMessage());
        } catch (Throwable e) {
            return errorResult(e.getMessage());
        }
    }
}
