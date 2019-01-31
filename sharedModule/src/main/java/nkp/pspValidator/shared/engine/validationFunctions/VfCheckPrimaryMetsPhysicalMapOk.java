package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.*;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckPrimaryMetsPhysicalMapOk extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";
    public static final String PARAM_EXPECTED_TOP_LEVEL_DIV_TYPE = "expected_top_level_div_type";
    public static final String PARAM_FILEGROUP_IDS = "filegroups_to_check";
    public static final String PARAM_PAGE_TYPES = "page_types";

    public VfCheckPrimaryMetsPhysicalMapOk(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_EXPECTED_TOP_LEVEL_DIV_TYPE, ValueType.STRING, 0, 1)
                .withValueParam(PARAM_FILEGROUP_IDS, ValueType.STRING_LIST, 1, 1)
                .withValueParam(PARAM_PAGE_TYPES, ValueType.STRING_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramPrimaryMetsFile = valueParams.getParams(PARAM_PRIMARY_METS_FILE).get(0).getEvaluation();
            File primaryMetsFile = (File) paramPrimaryMetsFile.getData();
            if (primaryMetsFile == null) {
                return invalidValueParamNull(PARAM_PRIMARY_METS_FILE, paramPrimaryMetsFile);
            }

            String expectedTopLevelDivType = null;
            List<ValueParam> paramValuesExpectedTopLevelDivType = valueParams.getParams(PARAM_EXPECTED_TOP_LEVEL_DIV_TYPE);
            if (!paramValuesExpectedTopLevelDivType.isEmpty()) {
                ValueEvaluation paramExpectedTopLevelDivType = paramValuesExpectedTopLevelDivType.get(0).getEvaluation();
                expectedTopLevelDivType = (String) paramExpectedTopLevelDivType.getData();
            }

            ValueEvaluation paramFilegroupIds = valueParams.getParams(PARAM_FILEGROUP_IDS).get(0).getEvaluation();
            List<String> filegroupIds = (List<String>) paramFilegroupIds.getData();
            if (filegroupIds == null) {
                return invalidValueParamNull(PARAM_FILEGROUP_IDS, paramFilegroupIds);
            }

            ValueEvaluation paramPageTypes = valueParams.getParams(PARAM_PAGE_TYPES).get(0).getEvaluation();
            List<String> pageTypes = (List<String>) paramPageTypes.getData();
            if (pageTypes == null) {
                return invalidValueParamNull(PARAM_PAGE_TYPES, paramPageTypes);
            }

            return validate(primaryMetsFile, expectedTopLevelDivType, filegroupIds, pageTypes);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File primaryMetsFile, String expectedTopLevelDivType, List<String> filegroupIds, List<String> pageTypes) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(primaryMetsFile, true);
            Map<String, Set<String>> fileIdsByFilegroup = new HashMap<>();
            for (String filegroupId : filegroupIds) {
                String filePath = String.format("/mets:mets/mets:fileSec/mets:fileGrp[@ID='%s']/mets:file", filegroupId);
                NodeList fileEls = (NodeList) engine.buildXpath(filePath).evaluate(doc, XPathConstants.NODESET);
                if (fileEls == null || fileEls.getLength() == 0) {
                    result.addError(Level.WARNING,
                            "%s: nenalezeny elementy %s",
                            primaryMetsFile.getName(), filePath);
                } else {
                    Set<String> fileIds = new HashSet<>();
                    for (int i = 0; i < fileEls.getLength(); i++) {
                        Element fileEl = (Element) fileEls.item(i);
                        String fileId = fileEl.getAttribute("ID");
                        if (fileId.isEmpty()) {
                            result.addError(Level.WARNING,
                                    "%s: některý z elementů %s má chybějící/prázdný atribut ID",
                                    primaryMetsFile.getName(), filePath);
                        } else {
                            fileIds.add(fileId);
                        }
                    }
                    fileIdsByFilegroup.put(filegroupId, fileIds);
                }
            }

            String structMapXpath = "/mets:mets/mets:structMap[@TYPE='PHYSICAL']";
            Element structMapEl = (Element) engine.buildXpath(structMapXpath).evaluate(doc, XPathConstants.NODE);
            if (structMapEl == null) {
                result.addError(Level.ERROR,
                        "%s: chybí fyzická strukturální mapa (%s)",
                        primaryMetsFile.getName(), structMapXpath);
            } else {
                //top-level div
                String topLevelDivPath = expectedTopLevelDivType == null ? "mets:div[@TYPE]" : String.format("mets:div[@TYPE='%s']", expectedTopLevelDivType);
                Element topLevelDivEl = (Element) engine.buildXpath(topLevelDivPath).evaluate(structMapEl, XPathConstants.NODE);
                if (topLevelDivEl == null) {
                    result.addError(Level.ERROR,
                            "%s: fyzická strukturální mapa neobsahuje element %s",
                            primaryMetsFile.getName(), topLevelDivPath);
                } else {
                    //DMDID
                    String dmdid = topLevelDivEl.getAttribute("DMDID");
                    if (dmdid.isEmpty()) {
                        result.addError(Level.ERROR,
                                "%s: atribut DMDID chybí, nebo je prázdný",
                                primaryMetsFile.getName());
                    } else {
                        String dmdSecPath = String.format("/mets:mets/mets:dmdSec[@ID='%s']", dmdid);
                        Element dmdSecEl = (Element) engine.buildXpath(dmdSecPath).evaluate(doc, XPathConstants.NODE);
                        if (dmdSecEl == null) {
                            result.addError(Level.ERROR,
                                    "%s: fyzická strukturální mapa se odkazuje na neexistující záznam %s",
                                    primaryMetsFile.getName(), dmdSecPath);
                        }
                    }
                    //page divs
                    NodeList pageDivEls = (NodeList) engine.buildXpath("mets:div").evaluate(topLevelDivEl, XPathConstants.NODESET);
                    if (pageDivEls.getLength() == 0) {
                        result.addError(Level.ERROR,
                                "%s: fyzická strukturální mapa neobsahuje žádný záznam stránky",
                                primaryMetsFile.getName());
                    }
                    for (int i = 0; i < pageDivEls.getLength(); i++) {
                        Element pageDivEl = (Element) pageDivEls.item(i);
                        //ID
                        String pageId = pageDivEl.getAttribute("ID");
                        if (pageId.isEmpty()) {
                            result.addError(Level.ERROR,
                                    "%s: některý ze záznamů stránek ve fyzické strukturální mapě má prázdný/chybějící atribut ID",
                                    primaryMetsFile.getName());
                        }
                        //TYPE
                        String pageType = pageDivEl.getAttribute("TYPE");
                        if (pageType.isEmpty()) {
                            result.addError(Level.ERROR,
                                    "%s: záznam stránky %s ve fyzické strukturální mapě má prázdný/chybějící atribut TYPE",
                                    primaryMetsFile.getName(), pageId);
                        } else {
                            if (!pageTypes.contains(pageType)) {
                                result.addError(Level.WARNING,
                                        "%s: záznam stránky %s ve fyzické strukturální mapě: nepovolená hodnota atributu TYPE ('%s')",
                                        primaryMetsFile.getName(), pageId, pageType);
                            }
                        }
                        //ORDERLABEL
                        String pageOrderLabel = pageDivEl.getAttribute("ORDERLABEL");
                        if (pageOrderLabel.isEmpty()) {
                            result.addError(Level.ERROR,
                                    "%s: záznam stránky %s ve fyzické strukturální mapě má prázdný/chybějící atribut ORDERLABEL",
                                    primaryMetsFile.getName(), pageId);
                        }

                        //ORDERLABEL
                        String pageOrder = pageDivEl.getAttribute("ORDER");
                        if (pageOrder.isEmpty()) {
                            result.addError(Level.ERROR,
                                    "%s: záznam stránky %s ve fyzické strukturální mapě má prázdný/chybějící atribut ORDER",
                                    primaryMetsFile.getName(), pageId);
                        } else {
                            try {
                                Integer.valueOf(pageOrder);
                            } catch (NumberFormatException e) {
                                result.addError(Level.ERROR,
                                        "%s: hodnota atributu ORDER ('%s') v záznamu stránky %s ve fyzické strukturální mapě není číslo",
                                        primaryMetsFile.getName(), pageOrder, pageId);
                            }
                        }

                        //kontrola odkazovani z fyzicke mapy na soubory
                        NodeList fptrFileids = (NodeList) engine.buildXpath("mets:fptr/@FILEID").evaluate(pageDivEl, XPathConstants.NODESET);
                        Set<String> fileGroupsUsed = new HashSet<>();
                        for (int j = 0; j < fptrFileids.getLength(); j++) {
                            String fptrFileId = fptrFileids.item(j).getNodeValue();
                            boolean found = false;
                            for (String filegroup : fileIdsByFilegroup.keySet()) {
                                if (fileIdsByFilegroup.get(filegroup).contains(fptrFileId)) {
                                    fileGroupsUsed.add(filegroup);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                result.addError(Level.ERROR,
                                        "%s: nalezen odkaz na neznámý záznam souboru %s v záznamu stránky %s ve fyzické strukturální mapě",
                                        primaryMetsFile.getName(), fptrFileId, pageId);
                            }
                        }
                        for (String fileGroup : fileIdsByFilegroup.keySet()) {
                            if (!fileGroupsUsed.contains(fileGroup)) {
                                result.addError(Level.ERROR,
                                        "%s: pro stránku %s nenalezen žádný odkaz na soubor ze skupiny %s ve fyzické strukturální mapě",
                                        primaryMetsFile.getName(), pageId, fileGroup);
                            }
                        }
                    }
                }
            }
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        } finally {
            return result;
        }
    }
}
