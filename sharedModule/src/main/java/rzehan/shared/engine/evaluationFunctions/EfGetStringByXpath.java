package rzehan.shared.engine.evaluationFunctions;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        }

        ValueEvaluation paramXmlFile = valueParams.getParams(PARAM_XML_FILE).get(0).getValueEvaluation();
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

        ValueEvaluation paramXpath = valueParams.getParams(PARAM_XPATH).get(0).getValueEvaluation();
        String xpathStr = (String) paramXpath.getData();
        if (xpathStr == null) {
            return errorResultParamNull(PARAM_XPATH, paramXpath);
        } else if (xpathStr.isEmpty()) {
            return errorResult(String.format("hodnota parametru %s je prázdná", PARAM_XPATH));
        }

        return evaluate(xmlFile, xpathStr);
    }

    private ValueEvaluation evaluate(File file, String xpathStr) {
        try {
            //TODO: pouzivat cache pro ziskavani dokumentu
            DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = b.parse(new FileInputStream(file));

            XPath xPath = XPathFactory.newInstance().newXPath();
            String string = (String) xPath.evaluate(xpathStr, doc.getDocumentElement(), XPathConstants.STRING);
            return okResult(string);
        } catch (ParserConfigurationException e) {
            return errorResult(String.format("ParserConfigurationException při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (SAXException e) {
            return errorResult(String.format("Nebyl nalezen well-built xml dokument v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            return errorResult(String.format("I/O chyba při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (XPathExpressionException e) {
            return errorResult(String.format("Neplatný xpath výraz '%s': %s", file.getAbsolutePath(), e.getMessage()));
        }
    }


}
