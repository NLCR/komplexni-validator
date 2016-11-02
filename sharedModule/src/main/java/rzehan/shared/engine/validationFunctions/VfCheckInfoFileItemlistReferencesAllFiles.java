package rzehan.shared.engine.validationFunctions;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Utils;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.exceptions.InvalidXPathExpressionException;
import rzehan.shared.engine.exceptions.XmlParsingException;
import rzehan.shared.engine.params.ValueParam;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by martin on 1.11.16.
 */
public class VfCheckInfoFileItemlistReferencesAllFiles extends ValidationFunction {

    public static final String PARAM_INFO_FILE = "info_file";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";

    public VfCheckInfoFileItemlistReferencesAllFiles(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_INFO_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public String getName() {
        return "checkInfoFileItemlistReferencesAllFiles";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramInfoFile = valueParams.getParams(PARAM_INFO_FILE).get(0).getEvaluation();
        File infoFile = (File) paramInfoFile.getData();
        if (infoFile == null) {
            return invalidValueParamNull(PARAM_INFO_FILE, paramInfoFile);
        } else if (infoFile.isDirectory()) {
            return invalidFileIsDir(infoFile);
        } else if (!infoFile.canRead()) {
            return invalidCannotReadDir(infoFile);
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
                return invalidValueParamNull(PARAM_FILES, filesEvaluation);
            } else {
                for (File file : files) {
                    expectedFiles.add(file.getAbsoluteFile());
                }
            }
        }

        return validate(infoFile, expectedFiles);
    }

    private ValidationResult validate(File infoFile, Set<File> expectedFiles) {
        try {
            Document infoDoc = engine.getXmlDocument(infoFile);
            XPathExpression exp = engine.buildXpath("/info/itemlist/item");
            NodeList nodes = (NodeList) exp.evaluate(infoDoc, XPathConstants.NODESET);
            File rootDir = infoFile.getParentFile();
            List<File> foundFilesList = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String path = node.getTextContent().trim();
                foundFilesList.add(Utils.buildAbsoluteFile(rootDir, path));
            }

            Set<File> foundFiles = new HashSet<>(foundFilesList.size());
            foundFiles.addAll(foundFilesList);
            if (!foundFiles.equals(expectedFiles)) {//something is different
                for (File foundFile : foundFiles) {
                    if (!expectedFiles.contains(foundFile)) {
                        return invalid(String.format("soubor INFO se odkazuje na neexistující soubor %s", foundFile.getAbsolutePath()));
                    }
                }
                for (File expectedFile : expectedFiles) {
                    if (!foundFiles.contains(expectedFile)) {
                        return invalid(String.format("soubor INFO se neodkazuje na existující soubor %s", expectedFile.getAbsolutePath()));
                    }
                }
            }
            return valid();
        } catch (XmlParsingException e) {
            return invalid(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (XPathExpressionException e) {
            return invalid(e.getMessage());
        } catch (Throwable e) {
            return invalid("neočekávaná chyba: " + e.getMessage());
        }
    }

}
