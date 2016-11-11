package nkp.pspValidator.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Utils;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.*;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by martin on 1.11.16.
 */
public class VfCheckMetsFilesecSizesMatch extends ValidationFunction {

    public static final String PARAM_PSP_DIR = "psp_dir";
    public static final String PARAM_METS_FILE = "mets_file";

    public VfCheckMetsFilesecSizesMatch(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_PSP_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkMetsFilesecSizesMatch";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramMetsFileEval = valueParams.getParams(PARAM_METS_FILE).get(0).getEvaluation();
            File metsFile = (File) paramMetsFileEval.getData();
            if (metsFile == null) {
                return invalidValueParamNull(PARAM_METS_FILE, paramMetsFileEval);
            } else if (metsFile.isDirectory()) {
                return invalidFileIsDir(metsFile);
            } else if (!metsFile.canRead()) {
                return invalidCannotReadDir(metsFile);
            }

            ValueEvaluation paramPspDirEval = valueParams.getParams(PARAM_PSP_DIR).get(0).getEvaluation();
            File pspDir = (File) paramPspDirEval.getData();
            if (pspDir == null) {
                return invalidValueParamNull(PARAM_PSP_DIR, paramPspDirEval);
            } else if (!pspDir.isDirectory()) {
                return invalidFileIsNotDir(pspDir);
            }

            return validate(pspDir, metsFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File pspdir, File metsFile) {
        try {
            Document doc = engine.getXmlDocument(metsFile);
            XPathExpression xpath = engine.buildXpath("//mets:fileSec/mets:fileGrp/mets:file");
            NodeList fileElements = (NodeList) xpath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < fileElements.getLength(); i++) {
                checkFile(pspdir, (Element) fileElements.item(i));
            }
            return valid();
        } catch (XmlParsingException e) {
            return invalid(e);
        } catch (InvalidXPathExpressionException e) {
            return invalid(e);
        } catch (XPathExpressionException e) {
            return invalid(e);
        } catch (InvalidPathException e) {
            return invalid(e);
        } catch (SizeDifferenceException e) {
            return invalid(e);
        }
    }

    private void checkFile(File pspdir, Element fileEl) throws InvalidXPathExpressionException, XPathExpressionException, InvalidPathException, SizeDifferenceException {
        long sizeExpected = Long.valueOf(fileEl.getAttribute("SIZE"));
        XPathExpression xpath = engine.buildXpath("mets:FLocat/@xlink:href");
        String filePath = (String) xpath.evaluate(fileEl, XPathConstants.STRING);
        File file = Utils.buildAbsoluteFile(pspdir, filePath);
        long sizeComputed = file.length();
        if (sizeComputed != sizeExpected) {
            throw new SizeDifferenceException(String.format("uvedená velikost (%d B) se liší od zjištěné velikosti (%d B) souboru %s", sizeExpected, sizeComputed, file.getAbsolutePath()));
        }
    }

}
