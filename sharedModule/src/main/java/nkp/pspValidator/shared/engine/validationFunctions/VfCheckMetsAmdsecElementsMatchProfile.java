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
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class VfCheckMetsAmdsecElementsMatchProfile extends ValidationFunction {

    public static final String PARAM_METS_FILES = "mets_files";
    public static final String PARAM_METS_FILE = "mets_file";
    public static final String PARAM_ELEMENT_XPATH = "element_xpath";
    public static final String PARAM_PROFILE_ID = "profile_id";
    public static final String PARAM_ELEMENT_MUST_EXIST = "element_must_exist";

    public VfCheckMetsAmdsecElementsMatchProfile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_METS_FILES, ValueType.FILE_LIST, 0, null)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_ELEMENT_XPATH, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_PROFILE_ID, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_ELEMENT_MUST_EXIST, ValueType.BOOLEAN, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            Set<File> metsFiles = mergeAbsolutFilesFromParams();

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

            MetadataProfile profile = engine.getTechnicalMetadataProfilesManager().buildProfile(profileId);
            if (profile == null) {
                return singlErrorResult(invalid(Level.ERROR, "nenalezen profil '%s'", profileId));
            } else {
                return validate(metsFiles, elementXpath, elementMustExist, profile);
            }
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

    private ValidationResult validate(Set<File> files, String elementXpath, Boolean elementMustExist, MetadataProfile profile) {
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

    private void validateFile(File file, ValidationResult result, String elementXpath, Boolean elementMustExist, MetadataProfile profile) {
        try {
            Document doc = engine.getXmlDocument(file, true);
            Element amdSecEl = (Element) engine.buildXpath("/mets:mets/mets:amdSec").evaluate(doc, XPathConstants.NODE);
            if (amdSecEl == null) {
                if (elementMustExist) {
                    result.addError(invalid(Level.ERROR, "nenalezen element mets:amdSec v souboru %s", file.getName()));
                }
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
                            String id = (String) engine.buildXpath("@ID").evaluate(elementToValidate, XPathConstants.STRING);
                            Document newDoc = XmlUtils.elementToNewDocument(elementToValidate, true);
                            MetadataProfileValidator.validate(profile, newDoc, result, String.format("%s: %s", file.getName(), id));
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
