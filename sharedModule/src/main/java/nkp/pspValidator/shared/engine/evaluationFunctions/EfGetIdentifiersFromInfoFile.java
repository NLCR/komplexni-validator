package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfGetIdentifiersFromInfoFile extends EvaluationFunction {

    private static final String PARAM_INFO_FILE = "info_file";

    public EfGetIdentifiersFromInfoFile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

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
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    private ValueEvaluation evaluate(File infoFile) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile, false);
            XPathExpression exp = engine.buildXpath("/info/titleid");
            NodeList nodes = (NodeList) exp.evaluate(infoDoc, XPathConstants.NODESET);
            List<Identifier> identifiers = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String typeLc = element.getAttribute("type").trim();
                String typeUc = element.getAttribute("TYPE").trim();
                String type = null;
                if (typeLc != null && !typeLc.trim().isEmpty()) {
                    type = typeLc.trim();
                } else if (typeUc != null && typeUc.trim().isEmpty()) {
                    type = typeUc.trim();
                }
                String value = element.getTextContent().trim();
                identifiers.add(new Identifier(type, value));
            }
            return okResult(identifiers);
        } catch (XmlFileParsingException e) {
            return errorResult(e);
        } catch (InvalidXPathExpressionException e) {
            return errorResult(e);
        } catch (XPathExpressionException e) {
            return errorResult(e);
        }
    }
}
