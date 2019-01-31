package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidIdException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckPrimaryMetsDcIdentifiersMatchModsIdentifiers extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckPrimaryMetsDcIdentifiersMatchModsIdentifiers(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramEvaluation = valueParams.getParams(PARAM_PRIMARY_METS_FILE).get(0).getEvaluation();
            File file = (File) paramEvaluation.getData();
            if (file == null) {
                return invalidValueParamNull(PARAM_PRIMARY_METS_FILE, paramEvaluation);
            } else if (file.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(file));
            } else if (!file.canRead()) {
                return singlErrorResult(invalidCannotReadDir(file));
            }

            return validate(file);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(file, true);
            Map<String, Map<String, Set<String>>> modsIdentifiers = getModsIdentifiersValidOnly(doc, result);
            Map<String, Map<String, Set<String>>> dcIdentifiers = getDcIdentifiers(doc, result);
            Set<String> sectionIds = new HashSet<>();
            sectionIds.addAll(modsIdentifiers.keySet());
            sectionIds.addAll(dcIdentifiers.keySet());
            for (String sectionId : sectionIds) {
                Map<String, Set<String>> modsIdsPerSection = modsIdentifiers.get(sectionId);
                Map<String, Set<String>> dcIdsPerSection = dcIdentifiers.get(sectionId);
                if (modsIdsPerSection == null) {
                    result.addError(invalid(Level.ERROR, "nenalezen element dmdSec s ID=MODSMD_%S", sectionId));
                } else if (dcIdsPerSection == null) {
                    result.addError(invalid(Level.ERROR, "nenalezen element dmdSec s ID=DCMD_%S", sectionId));
                } else {
                    Set<String> allIdTypes = new HashSet<>();
                    allIdTypes.addAll(modsIdsPerSection.keySet());
                    allIdTypes.addAll(dcIdsPerSection.keySet());
                    for (String idType : allIdTypes) {
                        compareIds(idType, modsIdsPerSection.get(idType), dcIdsPerSection.get(idType), result, sectionId);
                    }
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

    private void compareIds(String idType, Set<String> modsValues, Set<String> dcValues, ValidationResult result, String sectionId) {
        if (modsValues == null || modsValues.isEmpty()) {
            result.addError(invalid(Level.WARNING, "MODS neobsahuje identifikátor typu %s pro %s", idType, sectionId));
        } else if (dcValues == null || dcValues.isEmpty()) {
            result.addError(invalid(Level.WARNING, "DC neobsahuje identifikátor typu %s pro %s", idType, sectionId));
        } else {
            if (modsValues.size() == 1 && dcValues.size() == 1) {
                String modsValue = modsValues.iterator().next();
                String dcValue = dcValues.iterator().next();
                if (!modsValue.equals(dcValue)) {
                    result.addError(invalid(Level.WARNING, "hodnota jediného identifikátoru typu %s se liší mezi MODS (%s) a DC (%s) pro %s", idType, modsValue, dcValue, sectionId));
                }
            } else {
                Set<String> modsValuesRemainging = new HashSet<>();
                modsValuesRemainging.addAll(modsValues);
                Set<String> dcValuesRemainging = new HashSet<>();
                dcValuesRemainging.addAll(dcValues);
                //remove matching values
                for (String modsValue : modsValues) {
                    dcValuesRemainging.remove(modsValue);
                }
                for (String dcValue : dcValues) {
                    modsValuesRemainging.remove(dcValue);
                }
                //log identifiers that are in mods and not in dc or vise versa
                for (String modsValue : modsValuesRemainging) {
                    Identifier id = new Identifier(idType, modsValue);
                    result.addError(invalid(Level.WARNING, "identifikátor '%s' nalezen v MODS záznamu, ale nenalezen v DC záznamu pro %s", id, sectionId));
                }
                for (String dcValue : dcValuesRemainging) {
                    Identifier id = new Identifier(idType, dcValue);
                    result.addError(invalid(Level.WARNING, "identifikátor '%s' nalezen v DC záznamu, ale nenalezen v MODS záznamu pro %s", id, sectionId));
                }
            }
        }
    }

    private Map<String, Map<String, Set<String>>> getDcIdentifiers(Document doc, ValidationResult validationResult) throws InvalidXPathExpressionException, XPathExpressionException {
        Map<String, Map<String, Set<String>>> result = new HashMap<>();
        XPathExpression dmdSecXpath = engine.buildXpath("/mets:mets/mets:dmdSec[starts-with(@ID, \"DCMD\")]");
        NodeList dmdSecNodes = (NodeList) dmdSecXpath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < dmdSecNodes.getLength(); i++) {
            Element dmdSecEl = (Element) dmdSecNodes.item(i);
            String entityId = dmdSecEl.getAttribute("ID").substring("DCMD_".length());
            Map<String, Set<String>> ids = new HashMap<>();
            XPathExpression idXpath = engine.buildXpath("mets:mdWrap/mets:xmlData/oai_dc:dc/dc:identifier");
            NodeList idEls = (NodeList) idXpath.evaluate(dmdSecEl, XPathConstants.NODESET);
            for (int j = 0; j < idEls.getLength(); j++) {
                Element idEl = (Element) idEls.item(j);
                String idStr = idEl.getTextContent().trim();
                try {
                    Identifier identifier = Utils.extractIdentifierFromDcString(idStr);
                    Set<String> values = ids.get(identifier.getType());
                    if (values == null) {
                        values = new HashSet<>();
                        ids.put(identifier.getType(), values);
                    }
                    values.add(identifier.getValue());
                } catch (InvalidIdException e) {
                    //just ignore
                    //validationResult.addError(invalid(Level.ERROR, "neplatný identifikátor '%s': %s", idStr, e.getMessage()));
                }
            }
            result.put(entityId, ids);
        }
        return result;
    }

    private Map<String, Map<String, Set<String>>> getModsIdentifiersValidOnly(Document doc, ValidationResult validationResult) throws InvalidXPathExpressionException, XPathExpressionException {
        Map<String, Map<String, Set<String>>> result = new HashMap<>();
        XPathExpression dmdSecXpath = engine.buildXpath("/mets:mets/mets:dmdSec[starts-with(@ID, \"MODSMD\")]");
        NodeList dmdSecNodes = (NodeList) dmdSecXpath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < dmdSecNodes.getLength(); i++) {
            Element dmdSecEl = (Element) dmdSecNodes.item(i);
            String id = dmdSecEl.getAttribute("ID").substring("MODSMD_".length());
            Map<String, Set<String>> ids = new HashMap<>();
            XPathExpression idXpath = engine.buildXpath("mets:mdWrap/mets:xmlData/mods:mods/mods:identifier[not(@invalid='yes')]");
            NodeList idEls = (NodeList) idXpath.evaluate(dmdSecEl, XPathConstants.NODESET);
            for (int j = 0; j < idEls.getLength(); j++) {
                Element idEl = (Element) idEls.item(j);
                String type = idEl.getAttribute("type");
                String value = idEl.getTextContent().trim();
                Set<String> values = ids.get(type);
                if (values == null) {
                    values = new HashSet<>();
                    ids.put(type, values);
                }
                values.add(value);
            }
            result.put(id, ids);
        }
        return result;
    }

}
