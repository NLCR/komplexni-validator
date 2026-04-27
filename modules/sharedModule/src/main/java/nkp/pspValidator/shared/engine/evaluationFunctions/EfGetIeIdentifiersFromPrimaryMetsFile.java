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
import java.util.*;

/**
 * @deprecated Replaced with EfSelectIeIdentifiersFromPrimaryMetsFile with more precise IE id selection
 */
@Deprecated
public class EfGetIeIdentifiersFromPrimaryMetsFile extends EvaluationFunction {

    private static final String PARAM_PRIMARY_METS_FILE = "primary_mets_file";

    public EfGetIeIdentifiersFromPrimaryMetsFile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramInfoFile = valueParams.getParams(PARAM_PRIMARY_METS_FILE).get(0).getEvaluation();
            File primaryMetsFile = (File) paramInfoFile.getData();
            if (primaryMetsFile == null) {
                return errorResultParamNull(PARAM_PRIMARY_METS_FILE, paramInfoFile);
            } else if (!primaryMetsFile.exists()) {
                return errorResultFileDoesNotExist(primaryMetsFile);
            } else if (primaryMetsFile.isDirectory()) {
                return errorResultFileIsDir(primaryMetsFile);
            } else if (!primaryMetsFile.canRead()) {
                return errorResultCannotReadFile(primaryMetsFile);
            } else {
                return evaluate(primaryMetsFile);
            }
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    private ValueEvaluation evaluate(File infoFile) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile, true);
            XPathExpression exp = engine.buildXpath("//mods:mods");
            NodeList nodes = (NodeList) exp.evaluate(infoDoc, XPathConstants.NODESET);
            List<Element> modsEls = filterModsElByIntellectualEntities(nodes);
            Set<Identifier> identifiers = new HashSet<>();
            for (Element modsEl : modsEls) {
                identifiers.addAll(extractIdentifiersFromMods(modsEl));
            }
            return okResult(new ArrayList<>(identifiers));
        } catch (XmlFileParsingException | InvalidXPathExpressionException | XPathExpressionException e) {
            return errorResult(e);
        }
    }

    private List<Identifier> extractIdentifiersFromMods(Element modsEl) throws InvalidXPathExpressionException, XPathExpressionException {
        XPathExpression exp = engine.buildXpath("mods:identifier");
        NodeList nodes = (NodeList) exp.evaluate(modsEl, XPathConstants.NODESET);
        List<Identifier> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String type = element.getAttribute("type");
            String value = element.getTextContent().trim();
            if ("uuid".equals(type)) { //we're expecting value "uuid:47647030-3fb3-11e7-ad33-5ef3fc9ae867", not just "47647030-3fb3-11e7-ad33-5ef3fc9ae867"
                value = "uuid:" + value;
            }
            result.add(new Identifier(type, value));
        }
        return result;
    }

    private List<Element> filterModsElByIntellectualEntities(NodeList nodes) {
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String id = element.getAttribute("ID");
            boolean isPossibleIntellectualEntity = id.startsWith("MODS_ISSUE") || id.startsWith("MODS_SUPPL") || id.startsWith("MODS_VOLUME");
            if (isPossibleIntellectualEntity) {
                result.add(element);
            }
        }
        return result;
    }

}
