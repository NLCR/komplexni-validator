package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidIdException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;
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
 * Created by martin on 1.11.16.
 */
public class VfCheckPrimaryMetsDcIdentifiersMatchModsIdentifiers extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckPrimaryMetsDcIdentifiersMatchModsIdentifiers(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkPrimaryMetsDcIdentifiersMatchModsIdentifiers";
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
            Document doc = engine.getXmlDocument(file);
            Map<String, Map<String, String>> modsIdentifiers = getModsIdentifiers(doc, result);
            Map<String, Map<String, String>> dcIdentifiers = getDcIdentifiers(doc, result);
            Set<String> sectionIds = new HashSet<>();
            sectionIds.addAll(modsIdentifiers.keySet());
            sectionIds.addAll(dcIdentifiers.keySet());
            for (String sectionId : sectionIds) {
                Map<String, String> modsIdsPerSection = modsIdentifiers.get(sectionId);
                Map<String, String> dcIdsPerSection = dcIdentifiers.get(sectionId);
                if (modsIdsPerSection == null) {
                    result.addError(invalid(Level.ERROR, "nenalezen element dmdSec s ID=MODSMD_%S", sectionId));
                } else if (dcIdsPerSection == null) {
                    result.addError(invalid(Level.ERROR, "nenalezen element dmdSec s ID=DCMD_%S", sectionId));
                } else {
                    Set<String> allIdTypes = new HashSet<>();
                    allIdTypes.addAll(modsIdsPerSection.keySet());
                    allIdTypes.addAll(dcIdsPerSection.keySet());
                    for (String idType : allIdTypes) {
                        String modsValue = modsIdsPerSection.get(idType);
                        String dcValue = dcIdsPerSection.get(idType);
                        if (modsValue == null) {
                            result.addError(invalid(Level.ERROR, "MODS neobsahuje identifikátor typu %s pro %s", idType, sectionId));
                        } else if (dcValue == null) {
                            result.addError(invalid(Level.ERROR, "MODS neobsahuje identifikátor typu %s pro %s", idType, sectionId));
                        } else {
                            if (!modsValue.equals(dcValue)) {
                                result.addError(invalid(Level.ERROR, "hodnota identifikátoru typu %s se liší mezi MODS (%s) a DC (%s) pro %s", idType, modsValue, dcValue, sectionId));
                            }
                        }
                    }
                }
            }
            return result;
        } catch (XmlParsingException e) {
            result.addError(invalid(e));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } finally {
            return result;
        }
    }

    private Map<String, Map<String, String>> getDcIdentifiers(Document doc, ValidationResult validationResult) throws InvalidXPathExpressionException, XPathExpressionException {
        Map<String, Map<String, String>> result = new HashMap<>();
        XPathExpression dmdSecXpath = engine.buildXpath("/mets:mets/mets:dmdSec[starts-with(@ID, \"DCMD\")]");
        NodeList dmdSecNodes = (NodeList) dmdSecXpath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < dmdSecNodes.getLength(); i++) {
            Element dmdSecEl = (Element) dmdSecNodes.item(i);
            String id = dmdSecEl.getAttribute("ID").substring("DCMD_".length());
            Map<String, String> ids = new HashMap<>();
            XPathExpression idXpath = engine.buildXpath("mets:mdWrap/mets:xmlData/oai_dc:dc/dc:identifier");
            NodeList idEls = (NodeList) idXpath.evaluate(dmdSecEl, XPathConstants.NODESET);
            for (int j = 0; j < idEls.getLength(); j++) {
                Element idEl = (Element) idEls.item(j);
                String idStr = idEl.getTextContent().trim();
                try {
                    Identifier identifier = Utils.extractIdentifierFromDcString(idStr);
                    ids.put(identifier.getType(), identifier.getValue());
                } catch (InvalidIdException e) {
                    validationResult.addError(invalid(Level.ERROR, "neplatný identifikátor '%s': %s", idStr, e.getMessage()));
                }
            }
            result.put(id, ids);
        }
        return result;
    }

    private Map<String, Map<String, String>> getModsIdentifiers(Document doc, ValidationResult validationResult) throws InvalidXPathExpressionException, XPathExpressionException {
        Map<String, Map<String, String>> result = new HashMap<>();
        XPathExpression dmdSecXpath = engine.buildXpath("/mets:mets/mets:dmdSec[starts-with(@ID, \"MODSMD\")]");
        NodeList dmdSecNodes = (NodeList) dmdSecXpath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < dmdSecNodes.getLength(); i++) {
            Element dmdSecEl = (Element) dmdSecNodes.item(i);
            String id = dmdSecEl.getAttribute("ID").substring("MODSMD_".length());
            Map<String, String> ids = new HashMap<>();
            XPathExpression idXpath = engine.buildXpath("mets:mdWrap/mets:xmlData/mods:mods/mods:identifier");
            NodeList idEls = (NodeList) idXpath.evaluate(dmdSecEl, XPathConstants.NODESET);
            for (int j = 0; j < idEls.getLength(); j++) {
                Element idEl = (Element) idEls.item(j);
                String type = idEl.getAttribute("type");
                String value = idEl.getTextContent().trim();
                ids.put(type, value);
            }
            result.put(id, ids);
        }
        return result;
    }

}
