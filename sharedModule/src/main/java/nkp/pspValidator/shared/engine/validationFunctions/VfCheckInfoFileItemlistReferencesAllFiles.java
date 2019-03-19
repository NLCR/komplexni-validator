package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidPathException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckInfoFileItemlistReferencesAllFiles extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";
    public static final String PARAM_LEVEL = "level";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";

    public VfCheckInfoFileItemlistReferencesAllFiles(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
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

            ValueEvaluation paramLevel = valueParams.getParams(PARAM_LEVEL).get(0).getEvaluation();
            Level level = (Level) paramLevel.getData();
            if (level == null) {
                return invalidValueParamNull(PARAM_LEVEL, paramLevel);
            }

            Set<File> expectedFiles = new HashSet<>();

            List<ValueParam> fileParams = valueParams.getParams(PARAM_FILE);
            for (ValueParam fileParam : fileParams) {
                ValueEvaluation fileEvaluation = fileParam.getEvaluation();
                File file = (File) fileEvaluation.getData();
                if (file == null) {
                    return invalidValueParamNull(PARAM_FILE, fileEvaluation);
                } else {
                    expectedFiles.add(file.getAbsoluteFile());
                }
            }

            List<ValueParam> filesParams = valueParams.getParams(PARAM_FILES);
            for (ValueParam fileParam : filesParams) {
                ValueEvaluation filesEvaluation = fileParam.getEvaluation();
                List<File> files = (List<File>) filesEvaluation.getData();
                if (files == null) {
                    //ignore
                    //return invalidValueParamNull(PARAM_FILES, filesEvaluation);
                } else {
                    for (File file : files) {
                        expectedFiles.add(file.getAbsoluteFile());
                    }
                }
            }

            return validate(infoFile, expectedFiles, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File infoFile, Set<File> expectedFiles, Level level) {
        ValidationResult result = new ValidationResult();
        try {
            Document infoDoc = engine.getXmlDocument(infoFile, false);
            XPathExpression exp = engine.buildXpath("/info/itemlist/item");
            NodeList nodes = (NodeList) exp.evaluate(infoDoc, XPathConstants.NODESET);
            File rootDir = infoFile.getParentFile();
            List<File> foundFilesList = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String path = node.getTextContent().trim();
                try {
                    File file = Utils.buildAbsoluteFile(rootDir, path);
                    foundFilesList.add(file);
                } catch (InvalidPathException e) {
                    result.addError(invalid(e));
                }
            }

            Set<File> foundFiles = new HashSet<>(foundFilesList.size());
            foundFiles.addAll(foundFilesList);
            if (!foundFiles.equals(expectedFiles)) {//something is different
                for (File foundFile : foundFiles) {
                    if (!expectedFiles.contains(foundFile)) {
                        result.addError(invalid(level, "soubor INFO se odkazuje na neočekávaný soubor %s", foundFile.getAbsolutePath()));
                    }
                }
                for (File expectedFile : expectedFiles) {
                    if (!foundFiles.contains(expectedFile)) {
                        result.addError(invalid(level, "soubor INFO se neodkazuje na očekávaný soubor %s", expectedFile.getAbsolutePath()));
                    }
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

}
