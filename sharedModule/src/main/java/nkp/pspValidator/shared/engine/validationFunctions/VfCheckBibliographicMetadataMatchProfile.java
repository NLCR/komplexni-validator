package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidDataException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.types.EntityType;
import nkp.pspValidator.shared.engine.types.MetadataFormat;
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
public class VfCheckBibliographicMetadataMatchProfile extends ValidationFunction {

    public static final String PARAM_METS_FILE = "mets_file";
    public static final String PARAM_PROFILE_DEFINITION = "profile_definition";
    public static final String PARAM_METADATA_FORMAT = "metadata_format";
    public static final String PARAM_ENTITY_TYPE = "entity_type";


    public VfCheckBibliographicMetadataMatchProfile(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_PROFILE_DEFINITION, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_METADATA_FORMAT, ValueType.METADATA_FORMAT, 1, 1)
                .withValueParam(PARAM_ENTITY_TYPE, ValueType.ENTITY_TYPE, 1, 1)
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

            ValueEvaluation paramProfileDefinition = valueParams.getParams(PARAM_PROFILE_DEFINITION).get(0).getEvaluation();
            File profileDefinition = (File) paramProfileDefinition.getData();
            if (profileDefinition == null) {
                return invalidValueParamNull(PARAM_PROFILE_DEFINITION, paramProfileDefinition);
            } else if (profileDefinition.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(profileDefinition));
            } else if (!profileDefinition.canRead()) {
                return singlErrorResult(invalidCannotReadDir(profileDefinition));
            }

            ValueEvaluation paramMetadataFormat = valueParams.getParams(PARAM_METADATA_FORMAT).get(0).getEvaluation();
            MetadataFormat metadataFormat = (MetadataFormat) paramMetadataFormat.getData();
            if (metadataFormat == null) {
                return invalidValueParamNull(PARAM_METADATA_FORMAT, paramMetadataFormat);
            }

            ValueEvaluation paramEntityType = valueParams.getParams(PARAM_ENTITY_TYPE).get(0).getEvaluation();
            EntityType entityType = (EntityType) paramEntityType.getData();
            if (entityType == null) {
                return invalidValueParamNull(PARAM_ENTITY_TYPE, paramEntityType);
            }


            return validate(metsFile, profileDefinition, metadataFormat, entityType);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File metsFile, File profileDefinition, MetadataFormat metadataFormat, EntityType entityType) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(metsFile, true);
            XPathExpression dmdSecXpath = buildDmdSecXpath(entityType, metadataFormat);
            NodeList fileElements = (NodeList) dmdSecXpath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < fileElements.getLength(); i++) {
                try {
                    Element dmdSecEl = (Element) fileElements.item(i);
                    String dmdSecId = dmdSecEl.getAttribute("ID");
                    validate(profileDefinition, dmdSecId, dmdSecEl, result);
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
        } catch (InvalidDataException e) {
            result.addError(invalid(e));
        } finally {
            return result;
        }
    }

    private XPathExpression buildDmdSecXpath(EntityType entityType, MetadataFormat metadataFormat) throws InvalidDataException, InvalidXPathExpressionException {
        String formatPrefix = null;
        switch (metadataFormat) {
            case DC:
                formatPrefix = "DCMD";
                break;
            case MODS:
                formatPrefix = "MODSMD";
                break;
            default:
                throw new InvalidDataException("neočekávaný metadatový formát " + metadataFormat);
        }
        String idPrefix = formatPrefix + '_' + entityType.getDmdSecCode();
        //System.err.println("idPrefix: " + idPrefix);
        return engine.buildXpath("/mets:mets/mets:dmdSec[starts-with(@ID, \"" + idPrefix + "\")]");
    }

    private void validate(File profileDefinition, String dmdSecId, Element dmdSecEl, ValidationResult result) {
        //TODO: actual validation
    }


}
