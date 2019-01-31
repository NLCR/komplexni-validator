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
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.List;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckPrimaryMetsLogicalMapOk extends ValidationFunction {

    public static final String PARAM_PRIMARY_METS_FILE = "primary-mets_file";
    public static final String PARAM_DIV_TYPES_ALLOWED = "div_types_allowed";

    public VfCheckPrimaryMetsLogicalMapOk(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_PRIMARY_METS_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_DIV_TYPES_ALLOWED, ValueType.STRING_LIST, 1, 1)
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

            ValueEvaluation paramDivTypesAllowed = valueParams.getParams(PARAM_DIV_TYPES_ALLOWED).get(0).getEvaluation();
            List<String> divTypesAllowed = (List<String>) paramDivTypesAllowed.getData();
            if (divTypesAllowed == null) {
                return invalidValueParamNull(PARAM_DIV_TYPES_ALLOWED, paramDivTypesAllowed);
            }

            return validate(primaryMetsFile, divTypesAllowed);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File primaryMetsFile, List<String> divTypesAllowed) {
        ValidationResult result = new ValidationResult();
        try {
            Document doc = engine.getXmlDocument(primaryMetsFile, true);

            String structMapXpath = "/mets:mets/mets:structMap[@TYPE='LOGICAL']";
            Element structMapEl = (Element) engine.buildXpath(structMapXpath).evaluate(doc, XPathConstants.NODE);
            if (structMapEl == null) {
                result.addError(Level.ERROR,
                        "%s: chybí logická strukturální mapa (%s)",
                        primaryMetsFile.getName(), structMapXpath);
            } else {
                checkDivs(primaryMetsFile, doc, structMapEl, structMapXpath, divTypesAllowed, result);
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

    private void checkDivs(File primaryMetsFile, Document doc, Element parentEl, String parentPath, List<String> divTypesAllowed, ValidationResult result) throws InvalidXPathExpressionException, XPathExpressionException {
        NodeList divEls = (NodeList) engine.buildXpath("mets:div").evaluate(parentEl, XPathConstants.NODESET);
        for (int i = 0; i < divEls.getLength(); i++) {
            Element divEl = (Element) divEls.item(i);
            String id = divEl.getAttribute("ID");
            String path = id.isEmpty() ? String.format("%s/mets:div[%d]", parentPath, i) : String.format("%s/mets:div[ID='%s']", parentPath, id);
            if (id.isEmpty()) {
                result.addError(Level.ERROR,
                        "%s: %s: prázdný/chybějící atribut ID",
                        primaryMetsFile.getName(), path);
            }
            String type = divEl.getAttribute("TYPE");
            if (type.isEmpty()) {
                result.addError(Level.ERROR,
                        "%s: %s: prázdný/chybějící atribut TYPE",
                        primaryMetsFile.getName(), path);
            } else {
                if (!divTypesAllowed.contains(type)) {
                    result.addError(Level.ERROR,
                            "%s: %s: nepovolená hodnota atributu TYPE: '%s'",
                            primaryMetsFile.getName(), path, type);
                }
            }

            //DMDID
            String dmdid = divEl.getAttribute("DMDID");
            if (!dmdid.isEmpty()) {
                String dmdSecPath = String.format("/mets:mets/mets:dmdSec[@ID='%s']", dmdid);
                Element dmdSecEl = (Element) engine.buildXpath(dmdSecPath).evaluate(doc, XPathConstants.NODE);
                if (dmdSecEl == null) {
                    result.addError(Level.ERROR,
                            "%s: %s: odkaz na neexistující metadatový záznam %s",
                            primaryMetsFile.getName(), path, dmdSecPath);
                }
            }

            checkDivs(primaryMetsFile, doc, divEl, path, divTypesAllowed, result);
        }
    }
}
