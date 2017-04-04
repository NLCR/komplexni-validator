package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckSecondaryMetsPhysicalMapOk extends ValidationFunction {

    public static final String PARAM_SECONDARY_METS_FILES = "secondary-mets_files";
    public static final String PARAM_EXPECTED_PAGE_TYPE = "expected_page_type";
    public static final String PARAM_FILEGROUP_IDS = "filegroups_to_check";

    public VfCheckSecondaryMetsPhysicalMapOk(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_SECONDARY_METS_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_EXPECTED_PAGE_TYPE, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_FILEGROUP_IDS, ValueType.STRING_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkSecondaryMetsPhysicalMapOk";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramEvaluation = valueParams.getParams(PARAM_SECONDARY_METS_FILES).get(0).getEvaluation();
            List<File> metsFiles = (List<File>) paramEvaluation.getData();
            if (metsFiles == null) {
                return invalidValueParamNull(PARAM_SECONDARY_METS_FILES, paramEvaluation);
            }

            ValueEvaluation paramExpectedPageType = valueParams.getParams(PARAM_EXPECTED_PAGE_TYPE).get(0).getEvaluation();
            String expectedPageType = (String) paramExpectedPageType.getData();
            if (expectedPageType == null) {
                return invalidValueParamNull(PARAM_EXPECTED_PAGE_TYPE, paramExpectedPageType);
            }

            ValueEvaluation paramFilegroupIds = valueParams.getParams(PARAM_FILEGROUP_IDS).get(0).getEvaluation();
            List<String> filegroupIds = (List<String>) paramFilegroupIds.getData();
            if (filegroupIds == null) {
                return invalidValueParamNull(PARAM_FILEGROUP_IDS, paramFilegroupIds);
            }

            return validate(metsFiles, expectedPageType, filegroupIds);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<File> files, String expectedPageType, List<String> filegroupIds) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            if (file.isDirectory()) {
                result.addError(invalidFileIsDir(file));
            } else if (!file.canRead()) {
                result.addError(invalidCannotReadDir(file));
            } else {
                checkFileValid(file, expectedPageType, filegroupIds, result);
            }
        }
        return result;
    }

    private void checkFileValid(File file, String expectedPageType, List<String> filegroupIds, ValidationResult result) {
        try {
            Document doc = engine.getXmlDocument(file, true);
            List<String> fileIdsFromFileSec = new ArrayList<>();
            for (String filegroupId : filegroupIds) {
                String fileIdXpath = String.format("/mets:mets/mets:fileSec/mets:fileGrp[@ID='%s']/mets:file/@ID", filegroupId);
                String fileId = (String) engine.buildXpath(fileIdXpath).evaluate(doc, XPathConstants.STRING);
                if (fileId == null || fileId.isEmpty()) {
                    result.addError(Level.WARNING, "%s: nenalezena hodnota %s", file.getName(), fileIdXpath);
                } else {
                    fileIdsFromFileSec.add(fileId);
                }
            }

            String structMapXpath = "/mets:mets/mets:structMap";
            Element structMapEl = (Element) engine.buildXpath(structMapXpath).evaluate(doc, XPathConstants.NODE);
            if (structMapEl == null) {
                result.addError(Level.ERROR, "%s: nenalezen element %s ", file.getName(), structMapXpath);
            } else {
                // mets:structMap/@TYPE must be "PHYSICAL"
                String actualStructMapType = structMapEl.getAttribute("TYPE");
                if (actualStructMapType == null || actualStructMapType.isEmpty()) {
                    result.addError(Level.ERROR, "%s: chybějící nebo prázdný atribut TYPE elementu %s ", file.getName(), structMapXpath);
                } else {
                    String expectedStructMapType = "PHYSICAL";
                    if (!actualStructMapType.equals(expectedStructMapType)) {
                        result.addError(Level.ERROR, "%s: špatná hodnota atributu TYPE elementu %s: očekáváno '%s', nalezeno '%s'",
                                file.getName(), structMapXpath, expectedStructMapType, actualStructMapType);
                    }
                }

                // mets:structMap/mets:div/@TYPE must equal expectedPageType
                String actualPageType = (String) engine.buildXpath("mets:div/@TYPE").evaluate(structMapEl, XPathConstants.STRING);
                if (actualPageType == null || actualPageType.isEmpty()) {
                    result.addError(Level.ERROR, "%s: chybějící nebo prázdný atribut TYPE elementu mets:structMap/mets:div", file.getName());
                } else {
                    if (!actualPageType.equals(expectedPageType)) {
                        result.addError(Level.ERROR, "%s: špatná hodnota atributu TYPE elementu mets:structMap/mets:div: očekáváno '%s', nalezeno '%s'",
                                file.getName(), expectedPageType, actualPageType);
                    }
                }

                NodeList nodeList = (NodeList) engine.buildXpath("mets:div/mets:fptr/@FILEID").evaluate(structMapEl, XPathConstants.NODESET);
                List<String> fileIdsFromStructMap = new ArrayList<>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node item = nodeList.item(i);
                    String value = item.getNodeValue();
                    fileIdsFromStructMap.add(value);
                }

                //all files from fileSec must be referenced in structMap
                for (String fileIdFromFileSec : fileIdsFromFileSec) {
                    if (!fileIdsFromStructMap.contains(fileIdFromFileSec)) {
                        result.addError(Level.ERROR, "%s: fyzická strukturální mapa neobsahuje odkaz na soubor %s", file.getName(), fileIdFromFileSec);
                    }
                }

                //all files in structMap must also be referenced in fileSec
                for (String fileIdFromStructMap : fileIdsFromStructMap) {
                    if (!fileIdsFromFileSec.contains(fileIdFromStructMap)) {
                        result.addError(Level.ERROR, "%s: fyzická strukturální mapa obsahuje odkaz na neočekávaný soubor %s", file.getName(), fileIdFromStructMap);
                    }
                }
            }
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        }
    }
}
