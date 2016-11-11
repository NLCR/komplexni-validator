package rzehan.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Utils;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.*;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by martin on 1.11.16.
 */
public class VfCheckMetsFilesecChecksumsMatch extends ValidationFunction {

    public static final String PARAM_PSP_DIR = "psp_dir";
    public static final String PARAM_METS_FILE = "mets_file";

    public VfCheckMetsFilesecChecksumsMatch(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_PSP_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkMetsFilesecChecksumsMatch";
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
            return invalid(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (XPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (InvalidPathException e) {
            return invalid(e.getMessage());
        } catch (HashComputationException e) {
            return invalid(e.getMessage());
        } catch (HashMismatchException e) {
            return invalid(e.getMessage());
        }
    }

    private void checkFile(File pspdir, Element fileEl) throws InvalidXPathExpressionException, XPathExpressionException, InvalidPathException, HashMismatchException, HashComputationException {
        String hashExpected = fileEl.getAttribute("CHECKSUM");
        XPathExpression xpath = engine.buildXpath("mets:FLocat/@xlink:href");
        String filePath = (String) xpath.evaluate(fileEl, XPathConstants.STRING);
        File file = Utils.buildAbsoluteFile(pspdir, filePath);
        String hashComputed = Utils.computeHash(file);
        if (!hashComputed.toUpperCase().equals(hashExpected.toUpperCase())) {
            throw new HashMismatchException(String.format("uvedený kontrolní součet (%s) se liší od vypočítaného kontrolního součtu (%s) souboru %s", hashExpected, hashComputed, file.getAbsolutePath()));
        }
    }

}
