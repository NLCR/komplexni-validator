package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.*;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckInfoFileChecksumMatches extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";
    public static final String PARAM_CHECKSUM_FILE = "checksum_file";

    public VfCheckInfoFileChecksumMatches(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramInfoFile = valueParams.getParams(PARAM_INFO_FILE).get(0).getEvaluation();
            File infoFile = (File) paramInfoFile.getData();
            if (infoFile == null) {
                return invalidValueParamNull(PARAM_INFO_FILE, paramInfoFile);
            } else if (infoFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(infoFile));
            } else if (!infoFile.canRead()) {
                return singlErrorResult(invalidCannotReadDir(infoFile));
            }

            ValueEvaluation paramChecksumFile = valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getEvaluation();
            File checksumFile = (File) paramChecksumFile.getData();
            if (checksumFile == null) {
                return invalidValueParamNull(PARAM_CHECKSUM_FILE, paramChecksumFile);
            } else if (checksumFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(checksumFile));
            } else if (!checksumFile.canRead()) {
                return singlErrorResult(invalidCannotReadDir(checksumFile));
            }

            return validate(infoFile, checksumFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File infoFile, File checksumFileExisting) {
        ValidationResult result = new ValidationResult();
        try {
            Document infoDoc = engine.getXmlDocument(infoFile, false);
            //check if path to file CHECKSUM is correct
            XPathExpression checksumFileExp = engine.buildXpath("/info/checksum");
            String checksumFilePath = ((String) checksumFileExp.evaluate(infoDoc, XPathConstants.STRING)).trim();
            File checksumFileFound = Utils.buildAbsoluteFile(infoFile.getParentFile(), checksumFilePath);
            if (!checksumFileFound.equals(checksumFileExisting.getAbsoluteFile())) {
                result.addError(invalid(Level.ERROR,
                        "element checksum obsahuje cestu ke špatnému souboru: namísto (%s) obsahuje (%s)",
                        checksumFileExisting.getAbsolutePath(), checksumFileFound.getAbsolutePath()));
            }
            //check that computed hash and hash from INFO file match
            String hashFoundLc = (String) engine.buildXpath("/info/checksum/@checksum").evaluate(infoDoc, XPathConstants.STRING);
            String hashFoundUc = (String) engine.buildXpath("/info/checksum/@CHECKSUM").evaluate(infoDoc, XPathConstants.STRING);
            String hashFound = null;
            if (hashFoundLc != null && !hashFoundLc.trim().isEmpty()) {
                hashFound = hashFoundLc.trim();
            } else if (hashFoundUc != null && !hashFoundUc.trim().isEmpty()) {
                hashFound = hashFoundUc.trim();
            }
            String hashComputed = Utils.computeHash(checksumFileExisting);
            if (!hashComputed.toUpperCase().equals(hashFound.toUpperCase())) {
                result.addError(invalid(Level.ERROR,
                        "uvedený kontrolní součet '%s' nesouhlasí s vypočítaným kontrolním součtem '%s' pro soubor %s",
                        hashFound, hashComputed, checksumFileExisting.getAbsolutePath()));
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (InvalidPathException e) {
            result.addError(invalid(Level.ERROR, "cesta k souboru není zapsána korektně: '%s'", e.getPath()));
        } catch (HashComputationException e) {
            result.addError(invalid(Level.ERROR, "chyba výpočtu kontrolního součtu souboru %s: %s", checksumFileExisting.getAbsolutePath(), e.getMessage()));
        } finally {
            return result;
        }
    }

}
