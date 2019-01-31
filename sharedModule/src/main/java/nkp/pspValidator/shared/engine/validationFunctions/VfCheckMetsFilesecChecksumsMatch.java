package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckMetsFilesecChecksumsMatch extends ValidationFunction {

    public static final String PARAM_PSP_DIR = "psp_dir";
    public static final String PARAM_METS_FILE = "mets_file";

    public VfCheckMetsFilesecChecksumsMatch(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PSP_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
        );
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
                return singlErrorResult(invalidFileIsDir(metsFile));
            } else if (!metsFile.canRead()) {
                return singlErrorResult(invalidCannotReadDir(metsFile));
            }

            ValueEvaluation paramPspDirEval = valueParams.getParams(PARAM_PSP_DIR).get(0).getEvaluation();
            File pspDir = (File) paramPspDirEval.getData();
            if (pspDir == null) {
                return invalidValueParamNull(PARAM_PSP_DIR, paramPspDirEval);
            } else if (!pspDir.isDirectory()) {
                return singlErrorResult(invalidFileIsNotDir(pspDir));
            }

            return validate(pspDir, metsFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File pspdir, File metsFile) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(metsFile, true);
            XPathExpression xpath = engine.buildXpath("//mets:fileSec/mets:fileGrp/mets:file");
            NodeList fileElements = (NodeList) xpath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < fileElements.getLength(); i++) {
                try {
                    checkFile(pspdir, (Element) fileElements.item(i));
                } catch (HashMismatchException e) {
                    result.addError(invalid(Level.WARNING, e.getMessage()));
                } catch (Exception e) {
                    result.addError(invalid(e));
                }
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } finally {
            return result;
        }
    }

    private void checkFile(File pspdir, Element fileEl) throws InvalidXPathExpressionException, XPathExpressionException, InvalidPathException, HashComputationException, HashMismatchException {
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
