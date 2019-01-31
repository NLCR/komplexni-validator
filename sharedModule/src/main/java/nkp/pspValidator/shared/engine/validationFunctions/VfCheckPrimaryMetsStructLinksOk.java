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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.*;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckPrimaryMetsStructLinksOk extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";

    public VfCheckPrimaryMetsStructLinksOk(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
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

            return validate(primaryMetsFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File primaryMetsFile) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(primaryMetsFile, true);
            List<String> pageIds = getPageIdsFromPhysicalMap(doc);
            List<String> logicalObjectIds = getObjectIdsFromLogicalMap(doc);

            NodeList smLinkEls = (NodeList) engine.buildXpath("/mets:mets/mets:structLink/mets:smLink")
                    .evaluate(doc, XPathConstants.NODESET);
            Set<String> pageIdsConected = new HashSet<>();
            for (int i = 0; i < smLinkEls.getLength(); i++) {
                Element smLinkEl = (Element) smLinkEls.item(i);
                String from = smLinkEl.getAttribute("xlink:from");
                String to = smLinkEl.getAttribute("xlink:to");
                if (from.isEmpty() && to.isEmpty()) {
                    result.addError(Level.ERROR,
                            "%s: nalezen element smLink s prázdnými/chybějícími atributy xlink:from a xlink:to",
                            primaryMetsFile.getName());
                } else if (from.isEmpty()) {
                    result.addError(Level.ERROR,
                            "%s: nalezen element smLink s prázdným/chybějícím atributem xlink:from (xlink:to='%s')",
                            primaryMetsFile.getName(), to);
                } else if (to.isEmpty()) {
                    result.addError(Level.ERROR,
                            "%s: nalezen element smLink s prázdným/chybějícím atributem xlink:to (xlink:from='%s')",
                            primaryMetsFile.getName(), from);
                } else {
                    if (!logicalObjectIds.contains(from)) {
                        result.addError(Level.ERROR,
                                "%s: nalezen element smLink s hodnotou atributu xlink:from (%s), která neodpovídá žádnému atributu mets:div/@ID v logické strukturální mapě",
                                primaryMetsFile.getName(), from);
                    }
                    if (!pageIds.contains(to)) {
                        result.addError(Level.ERROR,
                                "%s: nalezen element smLink s hodnotou atributu xlink:to (%s), která neodpovídá žádné stránce ve fyzické strukturální mapě",
                                primaryMetsFile.getName(), to);
                    } else {
                        pageIdsConected.add(to);
                    }
                }
            }

            for (String pageId : pageIds) {
                if (!pageIdsConected.contains(pageId)) {
                    result.addError(Level.ERROR,
                            "%s: sekce structLink neobsahuje záznam pro stránku %s",
                            primaryMetsFile.getName(), pageId);
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

    private List<String> getObjectIdsFromLogicalMap(Document doc) throws InvalidXPathExpressionException, XPathExpressionException {
        List<String> result = new ArrayList<>();
        String structMapXpath = "/mets:mets/mets:structMap[@TYPE='LOGICAL']//mets:div/@ID";
        NodeList ids = (NodeList) engine.buildXpath(structMapXpath).evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < ids.getLength(); i++) {
            Node idAttr = ids.item(i);
            String id = idAttr.getNodeValue();
            result.add(id);
        }
        return result;
    }

    private List<String> getPageIdsFromPhysicalMap(Document doc) throws InvalidXPathExpressionException, XPathExpressionException {
        List<String> result = new ArrayList<>();
        String structMapXpath = "/mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div/@ID";
        NodeList ids = (NodeList) engine.buildXpath(structMapXpath).evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < ids.getLength(); i++) {
            Node idAttr = ids.item(i);
            String id = idAttr.getNodeValue();
            result.add(id);
        }
        return result;
    }
}
