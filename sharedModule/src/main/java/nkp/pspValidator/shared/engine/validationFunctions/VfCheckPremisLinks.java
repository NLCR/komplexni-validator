package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.types.Identifier;
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
public class VfCheckPremisLinks extends ValidationFunction {

    public static final String PARAM_METS_FILES = "mets_files";
    public static final String PARAM_METS_FILE = "mets_file";

    public VfCheckPremisLinks(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_METS_FILES, ValueType.FILE_LIST, 0, null)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 0, null)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            Set<File> metsFiles = mergeAbsolutFilesFromParams();
            return validate(metsFiles);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private Set<File> mergeAbsolutFilesFromParams() throws EmptyParamEvaluationException {
        Set<File> result = new HashSet<>();
        List<ValueParam> fileParams = valueParams.getParams(PARAM_METS_FILE);
        for (ValueParam param : fileParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            File file = (File) evaluation.getData();
            if (file == null) {
                throw new EmptyParamEvaluationException(PARAM_METS_FILE, evaluation);
            }
            result.add(file.getAbsoluteFile());
        }
        List<ValueParam> filesParams = valueParams.getParams(PARAM_METS_FILES);
        for (ValueParam param : filesParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            List<File> files = (List<File>) evaluation.getData();
            if (files == null) {
                throw new EmptyParamEvaluationException(PARAM_METS_FILES, evaluation);
            }
            for (File file : files) {
                result.add(file.getAbsoluteFile());
            }
        }
        return result;
    }

    private ValidationResult validate(Set<File> files) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            if (file.isDirectory()) {
                result.addError(invalidFileIsDir(file));
            } else if (!file.canRead()) {
                result.addError(invalidCannotReadDir(file));
            } else {
                validateFile(file, result);
            }
        }
        return result;
    }

    private void validateFile(File file, ValidationResult result) {
        try {
            Document doc = engine.getXmlDocument(file, true);
            Element amdSecEl = (Element) engine.buildXpath("/mets:mets/mets:amdSec").evaluate(doc, XPathConstants.NODE);
            if (amdSecEl == null) {
                result.addError(invalid(Level.ERROR, "nenalezen element mets:amdSec v souboru %s", file.getName()));
            } else {
                //identifiers must be unique
                checkObjectIdsUnique(file, amdSecEl, result);
                checkEventIdsUnique(file, amdSecEl, result);
                checkAgentIdsUnique(file, amdSecEl, result);

                //objects must refer to existing related objects
                checkObjectsReferToExistingRelatedObjects(file, amdSecEl, result);
                //objects must refer to existing related events
                checkObjectsReferToExistingRelatedEvents(file, amdSecEl, result);
                //objects must refer to existing linking events
                checkObjectsReferToExistingLinkingEvents(file, amdSecEl, result);

                //events must refer to existing linking objects
                checkEventsReferToExistingLinkingObjects(file, amdSecEl, result);
                //events must refer to existing linking agents
                checkEventsReferToExistingLinkingAgents(file, amdSecEl, result);

                //rerefences
                //OBJ_002 (MC) should refer to OBJ_001 (PS) through relatedObjectIdentification
                checkMcRefersToPs(file, amdSecEl, result);
                //OBJ_003 (ALTO) should refer to OBJ_002 (MC) through relatedObjectIdentification
                checkAltoRefersToMc(file, amdSecEl, result);
            }
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XPathExpressionException e) {
            result.addError(invalid(e));
        } catch (XmlFileParsingException e) {
            result.addError(invalid(e));
        }
    }


    private NodeList getAllObjectsElements(Element amdSecEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return (NodeList) engine.buildXpath("mets:techMD[starts-with(@ID,\"OBJ_\")]").evaluate(amdSecEl, XPathConstants.NODESET);
    }

    private NodeList getAllEventsElements(Element amdSecEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return (NodeList) engine.buildXpath("mets:digiprovMD[starts-with(@ID,\"EVT_\")]").evaluate(amdSecEl, XPathConstants.NODESET);
    }

    private NodeList getAllAgentsElements(Element amdSecEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return (NodeList) engine.buildXpath("mets:digiprovMD[starts-with(@ID,\"AGENT_\")]").evaluate(amdSecEl, XPathConstants.NODESET);
    }

    private List<Identifier> getIdentifiers(Element amdsecChildEl, String identifierXpath, String typeXpath, String valueXpath) throws InvalidXPathExpressionException, XPathExpressionException {
        List<Identifier> result = new ArrayList<>();
        NodeList idEls = (NodeList) engine.buildXpath(identifierXpath).evaluate(amdsecChildEl, XPathConstants.NODESET);
        for (int i = 0; i < idEls.getLength(); i++) {
            Element idEl = (Element) idEls.item(i);
            Element typeEl = XmlUtils.getFirstChildElementsByName(idEl, typeXpath);
            if (typeEl != null) {
                String type = XmlUtils.getDirectTextContent(typeEl);
                Element valueEl = XmlUtils.getFirstChildElementsByName(idEl, valueXpath);
                if (valueEl != null) {
                    String value = XmlUtils.getDirectTextContent(valueEl);
                    result.add(new Identifier(type, value));
                }
            }
        }
        return result;
    }


    //object
    private List<Identifier> getObjectIdentifiers(Element objectDigiprovMdEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return getIdentifiers(objectDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:object/premis:objectIdentifier",
                "premis:objectIdentifierType",
                "premis:objectIdentifierValue");
    }

    private List<Identifier> getObjectRelatedObjectsIdentifiers(Element objectDigiprovMdEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return getIdentifiers(objectDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:object/premis:relationship/premis:relatedObjectIdentification",
                "premis:relatedObjectIdentifierType",
                "premis:relatedObjectIdentifierValue");
    }


    private List<Identifier> getObjectRelatedEventsIdentifiers(Element objectDigiprovMdEl) throws XPathExpressionException, InvalidXPathExpressionException {
        return getIdentifiers(objectDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:object/premis:relationship/premis:relatedEventIdentification",
                "premis:relatedEventIdentifierType",
                "premis:relatedEventIdentifierValue");
    }

    private List<Identifier> getObjectLinkingEventsIdentifiers(Element objectDigiprovMdEl) throws XPathExpressionException, InvalidXPathExpressionException {
        return getIdentifiers(objectDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:object/premis:linkingEventIdentifier",
                "premis:linkingEventIdentifierType",
                "premis:linkingEventIdentifierValue");
    }

    //event
    private List<Identifier> getEventIdentifiers(Element eventDigiprovMdEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return getIdentifiers(eventDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:event/premis:eventIdentifier",
                "premis:eventIdentifierType",
                "premis:eventIdentifierValue");
    }

    private List<Identifier> getEventLinkingObjectsIdentifiers(Element eventDigiprovMdEl) throws XPathExpressionException, InvalidXPathExpressionException {
        return getIdentifiers(eventDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:event/premis:linkingObjectIdentifier",
                "premis:linkingObjectIdentifierType",
                "premis:linkingObjectIdentifierValue");
    }

    private List<Identifier> getEventLinkingAgentsIdentifiers(Element eventDigiprovMdEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return getIdentifiers(eventDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:event/premis:linkingAgentIdentifier",
                "premis:linkingAgentIdentifierType",
                "premis:linkingAgentIdentifierValue");
    }

    //agent
    private List<Identifier> getAgentIdentifiers(Element agentDigiprovMdEl) throws InvalidXPathExpressionException, XPathExpressionException {
        return getIdentifiers(agentDigiprovMdEl,
                "mets:mdWrap/mets:xmlData/premis:agent/premis:agentIdentifier",
                "premis:agentIdentifierType",
                "premis:agentIdentifierValue");
    }

    //METS ids by Identifier
    private String getMetsIdByIdentifier(Element amdSecEl, String amdsecChildPath) throws InvalidXPathExpressionException, XPathExpressionException {
        NodeList techmdEls = (NodeList) engine.buildXpath(amdsecChildPath).evaluate(amdSecEl, XPathConstants.NODESET);
        if (techmdEls.getLength() != 0) {
            Element objectEl = (Element) techmdEls.item(0);
            return objectEl.getAttribute("ID");
        } else {
            return null;
        }
    }

    private String getMetsIdOfObjectByIdentifier(Element amdSecEl, Identifier id) throws InvalidXPathExpressionException, XPathExpressionException {
        return getMetsIdByIdentifier(amdSecEl, String.format(
                "mets:techMD[starts-with(@ID,\"OBJ_\") and mets:mdWrap/mets:xmlData/premis:object/premis:objectIdentifier[premis:objectIdentifierType=\"%s\" and premis:objectIdentifierValue=\"%s\"]]",
                id.getType(),
                id.getValue()));
    }


    private String getMetsIdOfEVentByIdentifier(Element amdSecEl, Identifier id) throws XPathExpressionException, InvalidXPathExpressionException {
        return getMetsIdByIdentifier(amdSecEl, String.format(
                "mets:digiprovMD[starts-with(@ID,\"EVT_\") and mets:mdWrap/mets:xmlData/premis:event/premis:eventIdentifier[premis:eventIdentifierType=\"%s\" and premis:eventIdentifierValue=\"%s\"]]",
                id.getType(),
                id.getValue()));
    }


    private String getMetsIdOfAgentByIdentifier(Element amdSecEl, Identifier id) throws XPathExpressionException, InvalidXPathExpressionException {
        return getMetsIdByIdentifier(amdSecEl, String.format(
                "mets:digiprovMD[starts-with(@ID,\"AGENT_\") and mets:mdWrap/mets:xmlData/premis:agent/premis:agentIdentifier[premis:agentIdentifierType=\"%s\" and premis:agentIdentifierValue=\"%s\"]]",
                id.getType(),
                id.getValue()));
    }

    //unique id checks
    private void checkObjectIdsUnique(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        Map<Identifier, String> idMap = new HashMap<>();
        NodeList objectEls = getAllObjectsElements(amdSecEl);
        for (int i = 0; i < objectEls.getLength(); i++) {
            Element digiprovmdEl = (Element) objectEls.item(i);
            String eventMetsId = digiprovmdEl.getAttribute("ID");
            List<Identifier> eventIdentifiers = getObjectIdentifiers(digiprovmdEl);
            for (Identifier id : eventIdentifiers) {
                if (!idMap.containsKey(id)) {
                    idMap.put(id, eventMetsId);
                } else {
                    String colidingEventMetsId = idMap.get(id);
                    result.addError(Level.ERROR, String.format("%s: identifikátor není unikátní: Objekty '%s' a '%s' mají stejný identifikátor (typ=\"%s\", hodnota=\"%s\")", file.getName(), eventMetsId, colidingEventMetsId, id.getType(), id.getValue()));
                }
            }
        }
    }

    private void checkEventIdsUnique(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        Map<Identifier, String> idMap = new HashMap<>();
        NodeList eventEls = getAllEventsElements(amdSecEl);
        for (int i = 0; i < eventEls.getLength(); i++) {
            Element digiprovmdEl = (Element) eventEls.item(i);
            String eventMetsId = digiprovmdEl.getAttribute("ID");
            List<Identifier> eventIdentifiers = getEventIdentifiers(digiprovmdEl);
            for (Identifier id : eventIdentifiers) {
                if (!idMap.containsKey(id)) {
                    idMap.put(id, eventMetsId);
                } else {
                    String colidingEventMetsId = idMap.get(id);
                    result.addError(Level.ERROR, String.format("%s: identifikátor není unikátní: Události '%s' a '%s' mají stejný identifikátor (typ=\"%s\", hodnota=\"%s\")", file.getName(), eventMetsId, colidingEventMetsId, id.getType(), id.getValue()));
                }
            }
        }
    }

    private void checkAgentIdsUnique(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        Map<Identifier, String> idMap = new HashMap<>();
        NodeList agentEls = getAllAgentsElements(amdSecEl);
        for (int i = 0; i < agentEls.getLength(); i++) {
            Element digiprovmdEl = (Element) agentEls.item(i);
            String agentMetsId = digiprovmdEl.getAttribute("ID");
            List<Identifier> agentIdentifiers = getAgentIdentifiers(digiprovmdEl);
            for (Identifier id : agentIdentifiers) {
                if (!idMap.containsKey(id)) {
                    idMap.put(id, agentMetsId);
                } else {
                    String colidingAgentMetsId = idMap.get(id);
                    result.addError(Level.ERROR, String.format("%s: identifikátor není unikátní: Agenti '%s' a '%s' mají stejný identifikátor (typ=\"%s\", hodnota=\"%s\")", file.getName(), agentMetsId, colidingAgentMetsId, id.getType(), id.getValue()));
                }
            }
        }
    }

    //refering to existing elements
    private void checkObjectsReferToExistingRelatedObjects(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        NodeList objectEls = getAllObjectsElements(amdSecEl);
        for (int i = 0; i < objectEls.getLength(); i++) {
            Element objectEl = (Element) objectEls.item(i);
            String objectMetsId = objectEl.getAttribute("ID");
            List<Identifier> relatedObjectIds = getObjectRelatedObjectsIdentifiers(objectEl);
            //System.err.println("related objects to " + objectMetsId + ": " + relatedObjectIds.size());
            for (Identifier relatedObjectId : relatedObjectIds) {
                String relatedObjectMetsId = getMetsIdOfObjectByIdentifier(amdSecEl, relatedObjectId);
                if (relatedObjectMetsId == null) {
                    result.addError(Level.ERROR, String.format("%s: Objekt '%s' se odkazuje se na neexistující Objekt identifikátorem typu '%s' a hodnoty '%s'",
                            file.getName(), objectMetsId, relatedObjectId.getType(), relatedObjectId.getValue()));
                } else {
                    //System.err.println(String.format("%s -> %s -> %s", objectMetsId, relatedObjectId.toString(), relatedObjectMetsId));
                }
            }
        }
    }

    private void checkObjectsReferToExistingRelatedEvents(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        NodeList objectEls = getAllObjectsElements(amdSecEl);
        for (int i = 0; i < objectEls.getLength(); i++) {
            Element objectEl = (Element) objectEls.item(i);
            String objectMetsId = objectEl.getAttribute("ID");
            List<Identifier> relatedEventIds = getObjectRelatedEventsIdentifiers(objectEl);
            //System.err.println("related events to " + objectMetsId + ": " + relatedEventIds.size());
            for (Identifier relatedEventId : relatedEventIds) {
                String relatedEventtMetsId = getMetsIdOfEVentByIdentifier(amdSecEl, relatedEventId);
                if (relatedEventtMetsId == null) {
                    result.addError(Level.ERROR, String.format("%s: Objekt '%s' se odkazuje se na neexistující Událost identifikátorem typu '%s' a hodnoty '%s'",
                            file.getName(), objectMetsId, relatedEventId.getType(), relatedEventId.getValue()));
                } else {
                    //System.err.println(String.format("%s -> %s -> %s", objectMetsId, relatedEventId.toString(), relatedEventtMetsId));
                }
            }
        }
    }

    private void checkObjectsReferToExistingLinkingEvents(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        NodeList objectEls = getAllObjectsElements(amdSecEl);
        for (int i = 0; i < objectEls.getLength(); i++) {
            Element objectEl = (Element) objectEls.item(i);
            String objectMetsId = objectEl.getAttribute("ID");
            List<Identifier> linkingEventIds = getObjectLinkingEventsIdentifiers(objectEl);
            //System.err.println("related events to " + objectMetsId + ": " + linkingEventIds.size());
            for (Identifier linkingEventId : linkingEventIds) {
                String relatedEventtMetsId = getMetsIdOfEVentByIdentifier(amdSecEl, linkingEventId);
                if (relatedEventtMetsId == null) {
                    result.addError(Level.ERROR, String.format("%s: Objekt '%s' se odkazuje se na neexistující Událost identifikátorem typu '%s' a hodnoty '%s'",
                            file.getName(), objectMetsId, linkingEventId.getType(), linkingEventId.getValue()));
                } else {
                    //System.err.println(String.format("%s -> %s -> %s", objectMetsId, linkingEventId.toString(), relatedEventtMetsId));
                }
            }
        }
    }

    private void checkEventsReferToExistingLinkingObjects(File file, Element amdSecEl, ValidationResult result) throws XPathExpressionException, InvalidXPathExpressionException {
        NodeList eventEls = getAllEventsElements(amdSecEl);
        for (int i = 0; i < eventEls.getLength(); i++) {
            Element eventEl = (Element) eventEls.item(i);
            String eventMetsId = eventEl.getAttribute("ID");
            List<Identifier> linkingObjectIds = getEventLinkingObjectsIdentifiers(eventEl);
            //System.err.println("linking objects to " + eventMetsId + ": " + linkingObjectIds.size());
            for (Identifier linkingObjectId : linkingObjectIds) {
                String linkingObjectMetsId = getMetsIdOfObjectByIdentifier(amdSecEl, linkingObjectId);
                if (linkingObjectMetsId == null) {
                    result.addError(Level.ERROR, String.format("%s: Událost '%s' se odkazuje se na neexistující Objekt identifikátorem typu '%s' a hodnoty '%s'",
                            file.getName(), eventMetsId, linkingObjectId.getType(), linkingObjectId.getValue()));
                } else {
                    //System.err.println(String.format("%s -> %s -> %s", eventMetsId, linkingObjectId.toString(), linkingObjectMetsId));
                }
            }
        }
    }

    private void checkEventsReferToExistingLinkingAgents(File file, Element amdSecEl, ValidationResult result) throws InvalidXPathExpressionException, XPathExpressionException {
        NodeList eventEls = getAllEventsElements(amdSecEl);
        for (int i = 0; i < eventEls.getLength(); i++) {
            Element eventEl = (Element) eventEls.item(i);
            String eventMetsId = eventEl.getAttribute("ID");
            List<Identifier> linkingAgentIds = getEventLinkingAgentsIdentifiers(eventEl);
            //System.err.println("linking agents to " + eventMetsId + ": " + linkingAgentIds.size());
            for (Identifier linkingAgentId : linkingAgentIds) {
                String linkingAgentMetsId = getMetsIdOfAgentByIdentifier(amdSecEl, linkingAgentId);
                if (linkingAgentMetsId == null) {
                    result.addError(Level.ERROR, String.format("%s: Událost '%s' se odkazuje se na neexistujícího Agenta identifikátorem typu '%s' a hodnoty '%s'",
                            file.getName(), eventMetsId, linkingAgentId.getType(), linkingAgentId.getValue()));
                } else {
                    //System.err.println(String.format("%s -> %s -> %s", eventMetsId, linkingAgentId.toString(), linkingAgentMetsId));
                }
            }
        }
    }

    //check static references
    private void checkMcRefersToPs(File file, Element amdSecEl, ValidationResult result) throws InvalidXPathExpressionException, XPathExpressionException {
        NodeList mcEls = (NodeList) engine.buildXpath("mets:techMD[@ID=\"OBJ_002\"]").evaluate(amdSecEl, XPathConstants.NODESET);
        if (mcEls.getLength() == 0) {
            result.addError(Level.ERROR, String.format("%s: Chybí objekt archivní kopie (ID=OBJ_002)", file.getName()));
        } else if (mcEls.getLength() > 1) {
            result.addError(Level.ERROR, String.format("%s: Duplikovaný objekt archivní kopie (ID=OBJ_002)", file.getName()));
        } else {
            Element mcEl = (Element) mcEls.item(0);
            List<Identifier> objectRelatedObjectsIdentifiers = getObjectRelatedObjectsIdentifiers(mcEl);
            for (Identifier id : objectRelatedObjectsIdentifiers) {
                String metsId = getMetsIdOfObjectByIdentifier(amdSecEl, id);
                if ("OBJ_001".equals(metsId)) {
                    return;
                }
            }
            result.addError(Level.WARNING, String.format("%s: Chybí odkaz (relatedObject) z archivní kopie (ID=OBJ_002) na primární sken (ID=OBJ_001)", file.getName()));
        }
    }

    private void checkAltoRefersToMc(File file, Element amdSecEl, ValidationResult result) throws InvalidXPathExpressionException, XPathExpressionException {
        NodeList altoEls = (NodeList) engine.buildXpath("mets:techMD[@ID=\"OBJ_003\"]").evaluate(amdSecEl, XPathConstants.NODESET);
        if (altoEls.getLength() == 0) {
            result.addError(Level.ERROR, String.format("%s: Chybí objekt ALTO (ID=OBJ_003)", file.getName()));
        } else if (altoEls.getLength() > 1) {
            result.addError(Level.ERROR, String.format("%s: Duplikovaný objekt ALTO (ID=OBJ_003)", file.getName()));
        } else {
            Element mcEl = (Element) altoEls.item(0);
            List<Identifier> objectRelatedObjectsIdentifiers = getObjectRelatedObjectsIdentifiers(mcEl);
            for (Identifier id : objectRelatedObjectsIdentifiers) {
                String metsId = getMetsIdOfObjectByIdentifier(amdSecEl, id);
                if ("OBJ_002".equals(metsId)) {
                    return;
                }
            }
            result.addError(Level.WARNING, String.format("%s: Chybí odkaz (relatedObject) z ALTO (ID=OBJ_003) na archivní kopii (ID=OBJ_002)", file.getName()));
        }
    }

}
