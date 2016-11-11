package nkp.pspValidator.shared.engine.validationFunctions;


import org.w3c.dom.Document;
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
public class VfCheckInfoFileChecksumMatches extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";
    public static final String PARAM_CHECKSUM_FILE = "checksum_file";

    public VfCheckInfoFileChecksumMatches(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkInfoFileChecksumMatches";
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
                return invalidFileIsDir(infoFile);
            } else if (!infoFile.canRead()) {
                return invalidCannotReadDir(infoFile);
            }

            ValueEvaluation paramChecksumFile = valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getEvaluation();
            File checksumFile = (File) paramChecksumFile.getData();
            if (checksumFile == null) {
                return invalidValueParamNull(PARAM_CHECKSUM_FILE, paramChecksumFile);
            } else if (checksumFile.isDirectory()) {
                return invalidFileIsDir(checksumFile);
            } else if (!checksumFile.canRead()) {
                return invalidCannotReadDir(checksumFile);
            }

            return validate(infoFile, checksumFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File infoFile, File checksumFileExisting) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile);
            //check if path to file CHECKSUM is correct
            XPathExpression checksumFileExp = engine.buildXpath("/info/checksum");
            String checksumFilePath = ((String) checksumFileExp.evaluate(infoDoc, XPathConstants.STRING)).trim();
            File checksumFileFound = Utils.buildAbsoluteFile(infoFile.getParentFile(), checksumFilePath);
            if (!checksumFileFound.equals(checksumFileExisting.getAbsoluteFile())) {
                return invalid(String.format("element checksum obsahuje cestu ke špatnému souboru: namísto (%s) obsahuje (%s)",
                        checksumFileExisting.getAbsolutePath(), checksumFileFound.getAbsolutePath()));
            }
            //check that computed hash and hash from INFO file match
            XPathExpression hashExp = engine.buildXpath("/info/checksum/@checksum");
            String hashFound = (String) hashExp.evaluate(infoDoc, XPathConstants.STRING);
            String hashComputed = Utils.computeHash(checksumFileExisting);
            if (!hashComputed.toUpperCase().equals(hashFound.toUpperCase())) {
                return invalid(String.format("uvedený kontrolní součet '%s' nesouhlasí s vypočítaným kontrolním součtem '%s' pro soubor %s",
                        hashFound, hashComputed, checksumFileExisting.getAbsolutePath()));
            }

            return valid();
        } catch (XmlParsingException e) {
            return invalid(e);
        } catch (InvalidXPathExpressionException e) {
            return invalid(e);
        } catch (XPathExpressionException e) {
            return invalid(e);
        } catch (InvalidPathException e) {
            return invalid(String.format("cesta k souboru není zapsána korektně: '%s'", e.getPath()));
        } catch (HashComputationException e) {
            return invalid(String.format("chyba výpočtu kontrolního součtu souboru %s: %s", checksumFileExisting.getAbsolutePath(), e.getMessage()));
        }
    }

}
