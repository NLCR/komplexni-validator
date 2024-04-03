package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Utils;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidPathException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import org.w3c.dom.Document;
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
public class EfGetFileListByXpath extends EvaluationFunction {

    private static final String PARAM_PSP_ROOT_DIR = "psp_dir";
    private static final String PARAM_XML_FILE = "xml_file";
    private static final String PARAM_XPATH = "xpath";
    private static final String PARAM_NS_AWARE = "nsAware";

    public EfGetFileListByXpath(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.FILE_LIST)
                .withValueParam(PARAM_PSP_ROOT_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_XML_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_XPATH, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_NS_AWARE, ValueType.BOOLEAN, 0, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramRootDirEval = valueParams.getParams(PARAM_PSP_ROOT_DIR).get(0).getEvaluation();
            File rootDir = (File) paramRootDirEval.getData();
            if (rootDir == null) {
                return errorResultParamNull(PARAM_PSP_ROOT_DIR, paramRootDirEval);
            } else if (!rootDir.exists()) {
                return errorResultFileDoesNotExist(rootDir);
            } else if (!rootDir.isDirectory()) {
                return errorResultFileIsNotDir(rootDir);
            }

            ValueEvaluation paramXmlFile = valueParams.getParams(PARAM_XML_FILE).get(0).getEvaluation();
            File xmlFile = (File) paramXmlFile.getData();
            if (xmlFile == null) {
                return errorResultParamNull(PARAM_XML_FILE, paramXmlFile);
            } else if (!xmlFile.exists()) {
                return errorResultFileDoesNotExist(xmlFile);
            } else if (xmlFile.isDirectory()) {
                return errorResultFileIsDir(xmlFile);
            } else if (!xmlFile.canRead()) {
                return errorResultCannotReadFile(xmlFile);
            }

            ValueEvaluation paramXpath = valueParams.getParams(PARAM_XPATH).get(0).getEvaluation();
            String xpathStr = (String) paramXpath.getData();
            if (xpathStr == null) {
                return errorResultParamNull(PARAM_XPATH, paramXpath);
            } else if (xpathStr.isEmpty()) {
                return errorResult(String.format("hodnota parametru %s je prázdná", PARAM_XPATH));
            }

            boolean nsAware = true;
            List<ValueParam> nsAwareparams = valueParams.getParams(PARAM_NS_AWARE);
            if (nsAwareparams.size() == 1) {
                ValueEvaluation paramNsAware = nsAwareparams.get(0).getEvaluation();
                nsAware = (boolean) paramNsAware.getData();
            }

            return evaluate(rootDir, xmlFile, xpathStr, nsAware);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    private ValueEvaluation evaluate(File rootDir, File xmlFile, String xpathStr, boolean nsAware) {
        try {
            Document doc = engine.getXmlDocument(xmlFile, nsAware);
            XPathExpression xPathExpression = engine.buildXpath(xpathStr);
            NodeList nodes = (NodeList) xPathExpression.evaluate(doc, XPathConstants.NODESET);
            List<File> fileList = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                String path = nodes.item(i).getTextContent().trim();
                File file = Utils.buildAbsoluteFile(rootDir, path);
                //System.out.println(file.getPath());
                fileList.add(file);
            }
            return okResult(fileList);
        } catch (XPathExpressionException e) {
            return errorResult(String.format("neplatný xpath výraz '%s': %s", xmlFile.getAbsolutePath(), e.getMessage()));
        } catch (XmlFileParsingException e) {
            return errorResult(e);
        } catch (InvalidXPathExpressionException e) {
            return errorResult(e);
        } catch (InvalidPathException e) {
            return errorResult(e);
        }
    }


}
