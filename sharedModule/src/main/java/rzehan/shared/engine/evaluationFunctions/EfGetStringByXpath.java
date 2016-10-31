package rzehan.shared.engine.evaluationFunctions;

import com.sun.xml.internal.bind.v2.TODO;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ValidatorException;
import rzehan.shared.engine.validationFunctions.ValidationResult;

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
    public String evaluate() {
        checkContractCompliance();

        File file = (File) valueParams.getParams(PARAM_XML_FILE).get(0).getValue();
        String xpathStr = (String) valueParams.getParams(PARAM_XPATH).get(0).getValue();

        return evaluate(file, xpathStr);
    }

    private String evaluate(File file, String xpathStr) {
        try {
            //TODO: pouzivat cache pro ziskavani dokumentu
            DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = b.parse(new FileInputStream(file));

            XPath xPath = XPathFactory.newInstance().newXPath();
            String result = (String) xPath.evaluate(xpathStr, doc.getDocumentElement(), XPathConstants.STRING);
            return result;
        } catch (ParserConfigurationException e) {
            /*TODO: ve vysledku vracet pripadnou chybu*/
            return null;
            /*return new ValidationResult(false).withMessage(
                    String.format("ParserConfigurationException při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));*/
        } catch (SAXException e) {
            /*TODO: ve vysledku vracet pripadnou chybu*/
            return null;
            /*return new ValidationResult(false).withMessage(
                    String.format("Nebyl nalezen well-built xml dokument v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));*/
        } catch (IOException e) {
            /*TODO: ve vysledku vracet pripadnou chybu*/
            return null;
            /*return new ValidationResult(false).withMessage(
                    String.format("I/O chyba při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));*/
        } catch (XPathExpressionException e) {
            /*TODO: ve vysledku vracet pripadnou chybu*/
            return null;
        }
    }


}
