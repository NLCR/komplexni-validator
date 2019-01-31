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
public class EfGetModsIdentifiersForEachDmdsecId extends EvaluationFunction {

    private static final String PARAM_DMDSEC_IDS = "dmdsec_ids";
    private static final String PARAM_METS_FILE = "mets_file";

    public EfGetModsIdentifiersForEachDmdsecId(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST_LIST)
                .withValueParam(PARAM_DMDSEC_IDS, ValueType.STRING_LIST, 1, 1)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramDmdSecIds = valueParams.getParams(PARAM_DMDSEC_IDS).get(0).getEvaluation();
            List<String> dmdsecIds = (List<String>) paramDmdSecIds.getData();
            if (dmdsecIds == null) {
                return errorResultParamNull(PARAM_DMDSEC_IDS, paramDmdSecIds);
            }

            ValueEvaluation paramMetsFile = valueParams.getParams(PARAM_METS_FILE).get(0).getEvaluation();
            File metsFile = (File) paramMetsFile.getData();
            if (metsFile == null) {
                return errorResultParamNull(PARAM_METS_FILE, paramMetsFile);
            } else if (!metsFile.exists()) {
                return errorResultFileDoesNotExist(metsFile);
            } else if (metsFile.isDirectory()) {
                return errorResultFileIsDir(metsFile);
            } else if (!metsFile.canRead()) {
                return errorResultCannotReadFile(metsFile);
            }

            return evaluate(dmdsecIds, metsFile);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    private ValueEvaluation evaluate(List<String> dmdsecIds, File xmlFile) {
        try {
            List<List<Identifier>> result = new ArrayList<>();
            for (String dmdSecId : dmdsecIds) {
                Document doc = engine.getXmlDocument(xmlFile, true);
                XPathExpression xpathDmdSec = engine.buildXpath(String.format("/mets:mets/mets:dmdSec[@ID=\"%s\"]", dmdSecId));
                NodeList dmdSecNodes = (NodeList) xpathDmdSec.evaluate(doc, XPathConstants.NODESET);
                if (dmdSecNodes.getLength() != 1) {
                    return errorResult("dokument neobsahuje právě jeden element dmdSec s ID=" + dmdSecId);
                }
                Element dmdsecEl = (Element) dmdSecNodes.item(0);
                XPathExpression xpathIdentifier = engine.buildXpath("mets:mdWrap/mets:xmlData/mods:mods/mods:identifier[not(@invalid='yes')]");
                NodeList idEls = (NodeList) xpathIdentifier.evaluate(dmdsecEl, XPathConstants.NODESET);
                List<Identifier> idList = new ArrayList<>();
                for (int i = 0; i < idEls.getLength(); i++) {
                    Element idEl = (Element) idEls.item(i);
                    String type = idEl.getAttribute("type");
                    String value = idEl.getTextContent().trim();
                    idList.add(new Identifier(type, value));
                }
                result.add(idList);
                //System.err.println(dmdSecId + ": " + Utils.listToString(idList));
            }
            //System.err.println(Utils.listToString(result));
            return okResult(result);
        } catch (XPathExpressionException e) {
            return errorResult(String.format("neplatný xpath výraz '%s': %s", xmlFile.getAbsolutePath(), e.getMessage()));
        } catch (XmlFileParsingException e) {
            return errorResult(e);
        } catch (InvalidXPathExpressionException e) {
            return errorResult(e);
        }
    }


}
