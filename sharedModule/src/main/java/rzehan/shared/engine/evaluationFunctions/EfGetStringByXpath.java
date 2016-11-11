package rzehan.shared.engine.evaluationFunctions;

import org.w3c.dom.Document;
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
 * Created by martin on 21.10.16.
 */
public class EfGetStringByXpath extends EvaluationFunction {

    private static final String PARAM_XML_FILE = "xml_file";
    private static final String PARAM_XPATH = "xpath";

    public EfGetStringByXpath(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.STRING)
                .withValueParam(PARAM_XML_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_XPATH, ValueType.STRING, 1, 1)

        );
    }

    @Override
    public String getName() {
        return "getStringByXpath";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

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

            return evaluate(xmlFile, xpathStr);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    private ValueEvaluation evaluate(File file, String xpathStr) {
        try {
            Document doc = engine.getXmlDocument(file);
            XPathExpression xPathExpression = engine.buildXpath(xpathStr);
            String string = (String) xPathExpression.evaluate(doc, XPathConstants.STRING);
            return okResult(string);
        } catch (XPathExpressionException e) {
            return errorResult(String.format("neplatný xpath výraz '%s': %s", file.getAbsolutePath(), e.getMessage()));
        } catch (XmlParsingException e) {
            return errorResult(e);
        } catch (InvalidXPathExpressionException e) {
            return errorResult(e);
        }
    }


}
