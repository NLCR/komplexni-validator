package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.biblio.BiblioValidator;
import nkp.pspValidator.shared.biblio.biblioValidator.BiblioTemplate;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.List;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckSecondaryMetsAmdsecElementsMatchProfile extends ValidationFunction {

    public static final String PARAM_SECONDARY_METS_FILES = "secondary-mets_files";
    public static final String PARAM_ELEMENT_XPATH = "element_xpath";
    public static final String PARAM_PROFILE_ID = "profile_id";
    public static final String PARAM_ELEMENT_MUST_EXIST = "element_must_exist";

    public VfCheckSecondaryMetsAmdsecElementsMatchProfile(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_SECONDARY_METS_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_ELEMENT_XPATH, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_PROFILE_ID, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_ELEMENT_MUST_EXIST, ValueType.BOOLEAN, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkSecondaryMetsAmdsecElementsMatchProfile";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramSecondaryMetsFiles = valueParams.getParams(PARAM_SECONDARY_METS_FILES).get(0).getEvaluation();
            List<File> secondaryMetsFiles = (List<File>) paramSecondaryMetsFiles.getData();
            if (secondaryMetsFiles == null) {
                return invalidValueParamNull(PARAM_SECONDARY_METS_FILES, paramSecondaryMetsFiles);
            }

            ValueEvaluation paramElementXpath = valueParams.getParams(PARAM_ELEMENT_XPATH).get(0).getEvaluation();
            String elementXpath = (String) paramElementXpath.getData();
            if (elementXpath == null) {
                return invalidValueParamNull(PARAM_ELEMENT_XPATH, paramElementXpath);
            }

            ValueEvaluation paramProfileId = valueParams.getParams(PARAM_PROFILE_ID).get(0).getEvaluation();
            String profileId = (String) paramProfileId.getData();
            if (profileId == null) {
                return invalidValueParamNull(PARAM_PROFILE_ID, paramProfileId);
            }

            ValueEvaluation paramElementMustExist = valueParams.getParams(PARAM_ELEMENT_MUST_EXIST).get(0).getEvaluation();
            Boolean elementMustExist = (Boolean) paramElementMustExist.getData();
            if (elementMustExist == null) {
                return invalidValueParamNull(PARAM_PROFILE_ID, paramElementMustExist);
            }

            BiblioTemplate profile = engine.getTechnicalTemplatesManager().buildTemplate(profileId);
            if (profile == null) {
                return singlErrorResult(invalid(Level.ERROR, "nenalezen profil '%s'", profileId));
            } else {
                return validate(secondaryMetsFiles, elementXpath, elementMustExist, profile);
            }
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<File> files, String elementXpath, Boolean elementMustExist, BiblioTemplate profile) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            if (file.isDirectory()) {
                result.addError(invalidFileIsDir(file));
            } else if (!file.canRead()) {
                result.addError(invalidCannotReadDir(file));
            } else {
                validateFile(file, result, elementXpath, elementMustExist, profile);
            }
        }
        return result;
    }

    private void validateFile(File file, ValidationResult result, String elementXpath, Boolean elementMustExist, BiblioTemplate profile) {
        try {
            Document doc = engine.getXmlDocument(file, true);
            Element amdSecEl = (Element) engine.buildXpath("/mets:mets/mets:amdSec").evaluate(doc, XPathConstants.NODE);
            if (amdSecEl == null) {
                result.addError(invalid(Level.ERROR, "nenalezen element mets:amdSec v souboru %s", file.getName()));
            } else {
                NodeList nodesToValidate = (NodeList) engine.buildXpath(elementXpath).evaluate(amdSecEl, XPathConstants.NODESET);
                if (nodesToValidate.getLength() == 0) {
                    if (elementMustExist) {
                        result.addError(invalid(Level.ERROR, "v mets:amdSec nenalezen ani jeden element '%s' v souboru %s", elementXpath, file.getName()));
                    }
                } else {
                    for (int i = 0; i < nodesToValidate.getLength(); i++) {
                        Node nodeToValidate = nodesToValidate.item(i);
                        if (nodeToValidate.getNodeType() != Node.ELEMENT_NODE) {
                            result.addError(invalid(Level.ERROR, "výsledek '%s' není element (soubor %s)", elementXpath, file.getName()));
                        } else {
                            Element elementToValidate = (Element) nodeToValidate;
                            Document newDoc = XmlUtils.elementToNewDocument(elementToValidate, true);
                            BiblioValidator.validate(profile, newDoc, result, file.getName());
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
        } catch (ParserConfigurationException e) {
            result.addError(invalid(e));
        }
    }

}
