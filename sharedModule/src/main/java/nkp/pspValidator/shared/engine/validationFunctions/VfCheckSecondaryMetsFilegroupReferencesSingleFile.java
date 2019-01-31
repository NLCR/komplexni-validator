package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.FileUtils;
import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidPathException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckSecondaryMetsFilegroupReferencesSingleFile extends ValidationFunction {

    public static final String PARAM_PSP_DIR = "psp_dir";
    public static final String PARAM_SECONDARY_METS_FILES = "secondary-mets_files";
    public static final String PARAM_REFERENCED_FILES = "referenced_files";
    public static final String PARAM_FILEGROUP_ID = "filegroup_id";

    public VfCheckSecondaryMetsFilegroupReferencesSingleFile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PSP_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_SECONDARY_METS_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_REFERENCED_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_FILEGROUP_ID, ValueType.STRING, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation pspDirParam = valueParams.getParams(PARAM_PSP_DIR).get(0).getEvaluation();
            File pspDir = (File) pspDirParam.getData();
            if (pspDir == null) {
                return invalidValueParamNull(PARAM_PSP_DIR, pspDirParam);
            } else if (!pspDir.isDirectory()) {
                return singlErrorResult(invalidFileIsNotDir(pspDir));
            }

            ValueEvaluation secondaryMetsFilesParam = valueParams.getParams(PARAM_SECONDARY_METS_FILES).get(0).getEvaluation();
            List<File> secondaryMetsFiles = (List<File>) secondaryMetsFilesParam.getData();
            if (secondaryMetsFiles == null) {
                return invalidValueParamNull(PARAM_SECONDARY_METS_FILES, secondaryMetsFilesParam);
            }

            ValueEvaluation referencedFilesParam = valueParams.getParams(PARAM_REFERENCED_FILES).get(0).getEvaluation();
            List<File> referencedFiles = (List<File>) referencedFilesParam.getData();
            if (referencedFiles == null) {
                return invalidValueParamNull(PARAM_REFERENCED_FILES, referencedFilesParam);
            }

            ValueEvaluation filegroupIdParam = valueParams.getParams(PARAM_FILEGROUP_ID).get(0).getEvaluation();
            String filegroupId = (String) filegroupIdParam.getData();
            if (filegroupId == null) {
                return invalidValueParamNull(PARAM_FILEGROUP_ID, filegroupIdParam);
            }

            return validate(pspDir, secondaryMetsFiles, referencedFiles, filegroupId);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File pspRootDir, List<File> secondaryMetsFiles, List<File> referencedFiles, String filegroupId) {
        ValidationResult result = new ValidationResult();
        for (File secondaryMetsFile : secondaryMetsFiles) {
            //System.err.println("file: " + secondaryMetsFile.getAbsolutePath());
            //System.err.println("ID: " + filegroupId);
            String pageId = extractPageId(secondaryMetsFile);
            //System.err.println("page id: " + pageId);
            try {
                Document secondaryMetsDoc = engine.getXmlDocument(secondaryMetsFile, true);
                String xpathStr = String.format("/mets:mets/mets:fileSec/mets:fileGrp[@ID='%s']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href", filegroupId);
                //System.err.println(xpathStr);
                XPathExpression xPathExpression = engine.buildXpath(xpathStr);
                NodeList nodes = (NodeList) xPathExpression.evaluate(secondaryMetsDoc, XPathConstants.NODESET);
                //System.err.println("nodes:" + nodes.getLength());
                if (nodes.getLength() == 0) {
                    result.addError(Level.ERROR, "fileSec/fileGrp s ID '%s' neobsahuje element file (sekundární mets %s)",
                            filegroupId, FileUtils.toLimitedPath(secondaryMetsFile, 1));
                } else if (nodes.getLength() > 1) {
                    result.addError(Level.ERROR, "fileSec/fileGrp s ID '%s' obsahuje více elementů file (sekundární mets %s)",
                            filegroupId, FileUtils.toLimitedPath(secondaryMetsFile, 1));
                } else {
                    String path = nodes.item(0).getNodeValue();
                    //System.err.println("path: " + path);
                    File referencedFile = Utils.buildAbsoluteFile(pspRootDir, path);
                    if (!contains(referencedFiles, referencedFile)) {//check if referenced file in candidate files set
                        result.addError(Level.ERROR,
                                "fileSec/fileGrp s ID '%s': odkazovaný soubor (%s) není mezi očekávanými soubory (sekundární mets %s)",
                                filegroupId, path, FileUtils.toLimitedPath(secondaryMetsFile, 1));
                        /*for (File r : referencedFiles) {
                            System.err.println("r: " + r.getName());
                        }*/
                    } else {
                        if (!matches(referencedFile, pageId)) { //check if page id matches
                            result.addError(Level.ERROR,
                                    "fileSec/fileGrp s ID '%s': odkazovaný soubor (%s) nesouhlasí pro stránku %s (sekundární mets %s)",
                                    filegroupId, FileUtils.toLimitedPath(referencedFile, 1), pageId, FileUtils.toLimitedPath(secondaryMetsFile, 1));
                        }
                    }
                }
            } catch (XPathExpressionException e) {
                result.addError(Level.ERROR, e.getMessage());
            } catch (XmlFileParsingException e) {
                result.addError(Level.ERROR, e.getMessage());
            } catch (InvalidXPathExpressionException e) {
                result.addError(Level.ERROR, e.getMessage());
            } catch (InvalidPathException e) {
                result.addError(Level.ERROR, e.getMessage());
            } catch (IOException e) {
                result.addError(Level.ERROR, e.getMessage());
            }
        }
        return result;
    }

    private boolean matches(File referencedFile, String pageId) {
        String[] tokens = referencedFile.getName().split("\\.");
        /*System.err.println("tokens: " + tokens.length);
        for (String token : tokens) {
            System.err.println("token: " + token);
        }*/
        return tokens[tokens.length - 2].endsWith(pageId);
    }

    private boolean contains(List<File> list, File file) throws IOException {
        for (File fileFromList : list) {
            if (fileFromList.getCanonicalPath().equals(file.getCanonicalPath())) {
                return true;
            }
        }
        return false;
    }

    private String extractPageId(File secondaryMetsFile) {
        String nameWithoutSuffix = secondaryMetsFile.getName().substring(0, secondaryMetsFile.getName().length() - ".xml".length());
        String[] tokens = nameWithoutSuffix.split("_");
        return tokens[tokens.length - 1];
    }

}
