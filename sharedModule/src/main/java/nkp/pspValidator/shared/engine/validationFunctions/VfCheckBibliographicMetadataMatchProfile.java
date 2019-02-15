package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.XsdImportsResourceResolver;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileValidator;
import nkp.pspValidator.shared.metadataProfile.biblio.CatalogingConventions;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.types.EntityType;
import nkp.pspValidator.shared.engine.types.MetadataFormat;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckBibliographicMetadataMatchProfile extends ValidationFunction {

    public static final String PARAM_METS_FILE = "mets_file";
    public static final String PARAM_ENTITY_TYPE = "entity_type";
    public static final String PARAM_PROFILE_DETECTION_XPATH = "profile_detection_xpath";
    public static final String PARAM_XSD_DC = "xsd_file_dc";
    public static final String PARAM_XSD_MODS = "xsd_file_mods";

    public VfCheckBibliographicMetadataMatchProfile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_ENTITY_TYPE, ValueType.ENTITY_TYPE, 1, 1)
                .withValueParam(PARAM_PROFILE_DETECTION_XPATH, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_XSD_DC, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_XSD_MODS, ValueType.FILE, 1, 1)
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

            ValueEvaluation paramProfileDetectionXpath = valueParams.getParams(PARAM_PROFILE_DETECTION_XPATH).get(0).getEvaluation();
            String profileDetectionXpath = (String) paramProfileDetectionXpath.getData();
            if (profileDetectionXpath == null) {
                return invalidValueParamNull(PARAM_PROFILE_DETECTION_XPATH, paramProfileDetectionXpath);
            }

            ValueEvaluation paramEntityType = valueParams.getParams(PARAM_ENTITY_TYPE).get(0).getEvaluation();
            EntityType entityType = (EntityType) paramEntityType.getData();
            if (entityType == null) {
                return invalidValueParamNull(PARAM_ENTITY_TYPE, paramEntityType);
            }

            ValueEvaluation paramXsdFileDc = valueParams.getParams(PARAM_XSD_DC).get(0).getEvaluation();
            File xsdFileDc = (File) paramXsdFileDc.getData();
            if (xsdFileDc == null) {
            } else if (!xsdFileDc.exists()) {
                return singlErrorResult(invalidFileDoesNotExist(xsdFileDc));
            } else if (xsdFileDc.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(xsdFileDc));
            } else if (!xsdFileDc.canRead()) {
                return singlErrorResult(invalidCannotReadFile(xsdFileDc));
            }

            ValueEvaluation paramXsdFileMods = valueParams.getParams(PARAM_XSD_MODS).get(0).getEvaluation();
            File xsdFileMods = (File) paramXsdFileMods.getData();
            if (xsdFileMods == null) {
            } else if (!xsdFileMods.exists()) {
                return singlErrorResult(invalidFileDoesNotExist(xsdFileMods));
            } else if (xsdFileMods.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(xsdFileMods));
            } else if (!xsdFileMods.canRead()) {
                return singlErrorResult(invalidCannotReadFile(xsdFileMods));
            }

            return validate(metsFile, entityType, profileDetectionXpath, xsdFileDc, xsdFileMods);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File metsFile, EntityType entityType, String profileDetectionXpath, File xsdFileDc, File xsdFileMods) {
        ValidationResult result = new ValidationResult();
        try {
            Document metsDoc = engine.getXmlDocument(metsFile, true);
            List<String> entityIds = detectEntityIds(metsDoc, entityType);
            for (String entityId : entityIds) {
                //System.err.println("entity id: " + entityId);
                CatalogingConventions conventions = detectCatalogingConventions(entityId, entityType, metsDoc, result);
                //System.err.println("conventions: " + conventions);
                String entityTypeAndId = entityType.getDmdSecCode() + '_' + entityId;
                String profileName = detectProfileName(metsDoc, entityTypeAndId, profileDetectionXpath, result);
                //System.err.println("profile name: " + profileName);
                if (profileName == null) {
                    result.addError(invalid(Level.ERROR, "prázdný typ profilu pro %s; přeskakuji validaci MODS a DC zázanamů", entityTypeAndId));
                } else {
                    validateMetadata(metsDoc, entityType, entityId, MetadataFormat.DC, profileName, conventions, result, xsdFileDc);
                    validateMetadata(metsDoc, entityType, entityId, MetadataFormat.MODS, profileName, conventions, result, xsdFileMods);
                }
            }
            return result;
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
            return result;
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
            return result;
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
            return result;
        }
    }

    private String detectProfileName(Document metsDoc, String entityTypeAndId, String profileDetectionXpath, ValidationResult result)
            throws InvalidXPathExpressionException, XPathExpressionException {
        String dmdSecId = "MODSMD_" + entityTypeAndId;
        XPathExpression modsRootXpath = engine.buildXpath("/mets:mets/mets:dmdSec[@ID=\"" + dmdSecId + "\"]/mets:mdWrap/mets:xmlData/mods:mods");
        Element modsEl = (Element) modsRootXpath.evaluate(metsDoc, XPathConstants.NODE);
        if (modsEl == null) {
            result.addError(invalid(Level.INFO, "nenalezen element mets:mets pro záznam %s", dmdSecId));
            return null;
        } else {
            XPathExpression xPathExpression = engine.buildXpath(profileDetectionXpath);
            return (String) xPathExpression.evaluate(modsEl, XPathConstants.STRING);
        }
    }

    private CatalogingConventions detectCatalogingConventions(String entityId, EntityType entityType, Document metsDoc, ValidationResult result)
            throws InvalidXPathExpressionException, XPathExpressionException {
        String entityGlobalId = entityType.getDmdSecCode() + '_' + entityId;
        String dmdSecId = "MODSMD_" + entityGlobalId;
        XPathExpression xPathExpression = engine.buildXpath("/mets:mets/mets:dmdSec[@ID=\"" + dmdSecId + "\"]/mets:mdWrap/mets:xmlData/mods:mods/mods:recordInfo/mods:descriptionStandard");

        String specStr = (String) xPathExpression.evaluate(metsDoc, XPathConstants.STRING);
        if (specStr == null || specStr.isEmpty()) {
            result.addError(invalid(Level.INFO, "záznam %s neobsahuje informaci o použitých katalogizačních pravidlech; validuji %s oproti AACR2", dmdSecId, entityGlobalId));
            return CatalogingConventions.AACR2;
        } else {
            if (specStr.toLowerCase().equals("aacr") || specStr.toLowerCase().equals("aacr2")) {
                return CatalogingConventions.AACR2;
            } else if (specStr.equals("rda")) {
                return CatalogingConventions.RDA;
            } else {
                result.addError(invalid(Level.WARNING, "záznam %s obsahuje neplatnou informaci o použitých katalogizačních pravidlech (%s); validuji %s oproti AACR2", dmdSecId, specStr, entityGlobalId));
                return CatalogingConventions.AACR2;
            }
        }
    }

    private List<String> detectEntityIds(Document metsDoc, EntityType entityType) throws InvalidXPathExpressionException, XPathExpressionException {
        XPathExpression xPathExpression = engine.buildXpath("/mets:mets/mets:dmdSec[contains(@ID, \"" + entityType.getDmdSecCode() + "\")]/@ID");
        NodeList idAttrs = (NodeList) xPathExpression.evaluate(metsDoc, XPathConstants.NODESET);
        Set<String> set = new HashSet<>(idAttrs.getLength());
        for (int i = 0; i < idAttrs.getLength(); i++) {
            Attr attr = (Attr) idAttrs.item(i);
            String id = attr.getValue();
            if (id.startsWith("MODSMD_")) {
                id = id.substring("MODSMD_".length());
            } else if (id.startsWith("DCMD_")) {
                id = id.substring("DCMD_".length());
            }
            String typePrefix = entityType.getDmdSecCode() + '_';
            id = id.substring(typePrefix.length());
            set.add(id);
        }
        return new ArrayList<>(set);
    }

    private void validateMetadata(Document metsDoc, EntityType entityType, String entityId, MetadataFormat format, String profileName,
                                  CatalogingConventions catalogingConventions, ValidationResult result, File xsdFile) {
        String dmdSecId = null;
        try {
            dmdSecId = buildDmdSecId(format, entityType, entityId);
            XPathExpression xPathExpression = engine.buildXpath("/mets:mets/mets:dmdSec[@ID=\"" + dmdSecId + "\"]/mets:mdWrap/mets:xmlData/*[1]");
            Element dataElement = (Element) xPathExpression.evaluate(metsDoc, XPathConstants.NODE);
            if (dataElement == null) {
                result.addError(invalid(Level.ERROR, "nenalezen záznam %s", dmdSecId));
            } else {
                Document metadataDoc = XmlUtils.elementToNewDocument(dataElement, true);
                boolean validByXsd = validateByXsd(xsdFile, metadataDoc, dmdSecId, result);
                if (validByXsd) {
                    //result.addError(Level.INFO, String.format("%s je validní podle %s", dmdSecId, xsdFile.getName()));
                    MetadataProfile profile = engine.getBibliographicMetadataProfilesManager().buildProfile(profileName, format, catalogingConventions);
                    if (profile == null) {
                        result.addError(invalid(Level.ERROR, "nenalezen profil '%s' (verze %s, %s), pravděpodobně chybí element mods:genre určující druh záznamu; ignoruji validaci záznamu %s", profileName, format, catalogingConventions, dmdSecId));
                    } else {
                        MetadataProfileValidator.validate(profile, metadataDoc, result, dmdSecId);
                    }
                }
            }
        } catch (InvalidXPathExpressionException e) {
            result.addError(Level.ERROR, dmdSecId + ": " + e.getMessage());
        } catch (XPathExpressionException e) {
            result.addError(Level.ERROR, dmdSecId + ": " + e.getMessage());
        } catch (ParserConfigurationException e) {
            result.addError(Level.ERROR, dmdSecId + ": " + e.getMessage());
        } catch (Throwable e) {
            result.addError(Level.ERROR, dmdSecId + ": " + e.getMessage());
        }
    }

    private boolean validateByXsd(File xsdFile, Document xmlDoc, String dmdSecId, ValidationResult result) {
        try {
            DOMSource source = new DOMSource(xmlDoc);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setResourceResolver(new XsdImportsResourceResolver(xsdFile.getParentFile()));
            Schema schema = schemaFactory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.validate(source);
            return true;
        } catch (SAXException e) {
            result.addError(invalid(Level.ERROR, "záznam %s není validní podle  %s: %s", dmdSecId, xsdFile.getName(), e.getMessage()));
            return false;
        } catch (IOException e) {
            result.addError(invalid(Level.ERROR, "I/O chyba při čtení záznamu %s: %s", dmdSecId, e.getMessage()));
            return false;
        }
    }

    private String buildDmdSecId(MetadataFormat dc, EntityType entityType, String entityId) {
        switch (dc) {
            case DC:
                return "DCMD_" + entityType.getDmdSecCode() + '_' + entityId;
            case MODS:
                return "MODSMD_" + entityType.getDmdSecCode() + '_' + entityId;
            default:
                throw new IllegalStateException("unexpected value " + dc);
        }
    }

}
