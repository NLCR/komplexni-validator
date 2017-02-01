package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.biblio.BiblioValidator;
import nkp.pspValidator.shared.biblio.CatalogingConventions;
import nkp.pspValidator.shared.biblio.biblioValidator.BiblioTemplate;
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

import javax.xml.parsers.ParserConfigurationException;
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
public class VfCheckBibliographicMetadataMatchProfile extends ValidationFunction {

    public static final String PARAM_METS_FILE = "mets_file";
    public static final String PARAM_ENTITY_TYPE = "entity_type";
    public static final String PARAM_TEMPLATE_DETECTION_XPATH = "template_detection_xpath";

    public VfCheckBibliographicMetadataMatchProfile(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_ENTITY_TYPE, ValueType.ENTITY_TYPE, 1, 1)
                .withValueParam(PARAM_TEMPLATE_DETECTION_XPATH, ValueType.STRING, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkBibliographicMetadataMatchProfile";
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

            ValueEvaluation paramTemplateDetectionXpath = valueParams.getParams(PARAM_TEMPLATE_DETECTION_XPATH).get(0).getEvaluation();
            String templateDetectionXpath = (String) paramTemplateDetectionXpath.getData();
            if (templateDetectionXpath == null) {
                return invalidValueParamNull(PARAM_TEMPLATE_DETECTION_XPATH, paramTemplateDetectionXpath);
            }

            ValueEvaluation paramEntityType = valueParams.getParams(PARAM_ENTITY_TYPE).get(0).getEvaluation();
            EntityType entityType = (EntityType) paramEntityType.getData();
            if (entityType == null) {
                return invalidValueParamNull(PARAM_ENTITY_TYPE, paramEntityType);
            }

            return validate(metsFile, entityType, templateDetectionXpath);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File metsFile, EntityType entityType, String templateDetectionXpath) {
        ValidationResult result = new ValidationResult();
        try {
            Document metsDoc = engine.getXmlDocument(metsFile, true);
            List<String> entityIds = detectEntityIds(metsDoc, entityType);
            for (String entityId : entityIds) {
                try {
                    //System.err.println("entity id: " + entityId);
                    CatalogingConventions conventions = detectCatalogingConventions(entityId, entityType, metsDoc, result);
                    //System.err.println("conventions: " + conventions);
                    String entityTypeAndId = entityType.getDmdSecCode() + '_' + entityId;
                    String templateName = detectTemplateName(metsDoc, entityTypeAndId, templateDetectionXpath, result);
                    //System.err.println("template name: " + templateName);
                    if (templateName == null) {
                        result.addError(invalid(Level.ERROR, "prázdný typ šablony pro %s; přeskakuji validaci MODS a DC zázanamů", entityTypeAndId));
                    } else {
                        validateMetadata(metsDoc, entityType, entityId, MetadataFormat.DC, templateName, conventions, result);
                        validateMetadata(metsDoc, entityType, entityId, MetadataFormat.MODS, templateName, conventions, result);
                    }
                } catch (InvalidXPathExpressionException e) {
                    result.addError(Level.ERROR, e.getMessage());
                } catch (XPathExpressionException e) {
                    result.addError(Level.ERROR, e.getMessage());
                }
            }
            return result;
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

    private String detectTemplateName(Document metsDoc, String entityTypeAndId, String templateDetectionXpath, ValidationResult result)
            throws InvalidXPathExpressionException, XPathExpressionException {
        String dmdSecId = "MODSMD_" + entityTypeAndId;
        XPathExpression modsRootXpath = engine.buildXpath("/mets:mets/mets:dmdSec[@ID=\"" + dmdSecId + "\"]/mets:mdWrap/mets:xmlData/mods:mods");
        Element modsEl = (Element) modsRootXpath.evaluate(metsDoc, XPathConstants.NODE);
        if (modsEl == null) {
            result.addError(invalid(Level.INFO, "nenalezen element mets:mets pro záznam %s", dmdSecId));
            return null;
        } else {
            XPathExpression xPathExpression = engine.buildXpath(templateDetectionXpath);
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
            result.addError(invalid(Level.INFO, "záznam %s neobsahuje informaci o použitých digitalizačních pravidlech; validuji %s oproti AACR2", dmdSecId, entityGlobalId));
            return CatalogingConventions.AACR2;
        } else {
            if (specStr.equals("aacr2")) {
                return CatalogingConventions.AACR2;
            } else if (specStr.equals("rda")) {
                return CatalogingConventions.RDA;
            } else {
                result.addError(invalid(Level.WARNING, "záznam %s obsahuje neplatnou informaci o použitých digitalizačních pravidlech (%s); validuji %s oproti AACR2", dmdSecId, specStr, entityGlobalId));
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

    private void validateMetadata(Document metsDoc, EntityType entityType, String entityId, MetadataFormat format, String templateName, CatalogingConventions catalogingConventions, ValidationResult result) {
        try {
            String dmdSecId = buildDmdSecId(format, entityType, entityId);
            XPathExpression xPathExpression = engine.buildXpath("/mets:mets/mets:dmdSec[@ID=\"" + dmdSecId + "\"]/mets:mdWrap/mets:xmlData/*[1]");
            Element dataElement = (Element) xPathExpression.evaluate(metsDoc, XPathConstants.NODE);
            if (dataElement == null) {
                result.addError(invalid(Level.ERROR, "nenalezen záznam %s", dmdSecId));
            } else {
                Document metadataDoc = XmlUtils.elementToNewDocument(dataElement, true);
                BiblioTemplate biblioTemplate = engine.getBiblioMgr().buildTemplate(templateName, format, catalogingConventions);
                if (biblioTemplate == null) {
                    result.addError(invalid(Level.INFO, "nenalezena šablona '%s' (verze %s, %s); ignoruji validaci záznamu %s", templateName, format, catalogingConventions, dmdSecId));
                } else {
                    BiblioValidator.validate(biblioTemplate, metadataDoc, result, dmdSecId);
                }
            }
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (ParserConfigurationException e) {
            result.addError(invalid(e));
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
