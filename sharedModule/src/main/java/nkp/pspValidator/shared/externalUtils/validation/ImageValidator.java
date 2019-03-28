package nkp.pspValidator.shared.externalUtils.validation;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExtractionResultType;
import nkp.pspValidator.shared.externalUtils.ImageCopy;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.validation.extractions.AllNonemptyByRegexpDataExtraction;
import nkp.pspValidator.shared.externalUtils.validation.extractions.FirstNonemptyByXpathDataExctraction;
import nkp.pspValidator.shared.externalUtils.validation.rules.MustExistDR;
import nkp.pspValidator.shared.externalUtils.validation.rules.MustMatchAnyDR;
import nkp.pspValidator.shared.externalUtils.validation.rules.MustNotExistDR;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.ConstantConstraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.FlowRangeConstraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.IntRangeConstraint;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.NTimesXRemainingYConstraint;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 16.11.16.
 */
public class ImageValidator {

    private final ExternalUtilManager externalUtilManager;
    private final Map<ExternalUtil, J2kProfile> masterCopyProfiles = new HashMap<>();
    private final Map<ExternalUtil, J2kProfile> userCopyProfiles = new HashMap<>();


    public ImageValidator(ExternalUtilManager externalUtilManager) {
        this.externalUtilManager = externalUtilManager;
    }

    public void processProfile(ExternalUtil util, ImageCopy copy, File profileDefinitionFile) throws ValidatorConfigurationException {
        switch (copy) {
            case MASTER:
                processProfile(util, profileDefinitionFile, masterCopyProfiles);
                break;
            case USER:
                processProfile(util, profileDefinitionFile, userCopyProfiles);
                break;
        }
    }

    public boolean isUtilAvailable(ExternalUtil util) {
        return externalUtilManager.isUtilAvailable(util);
    }

    public J2kProfile getProfile(ImageCopy copy, ExternalUtil util) {
        switch (copy) {
            case MASTER:
                return masterCopyProfiles.get(util);
            case USER:
                return userCopyProfiles.get(util);
            default:
                throw new IllegalStateException();
        }
    }

    private void processProfile(ExternalUtil util, File profileDefinitionFile, Map<ExternalUtil, J2kProfile> profiles) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(profileDefinitionFile, true);
        J2kProfile profile = buildProfile(util, doc.getCurrentTag());
        profiles.put(util, profile);
    }

    private J2kProfile buildProfile(ExternalUtil util, Element rootEl) throws ValidatorConfigurationException {
        Element profileTypeEl = XmlUtils.getChildrenElements(rootEl).get(0);
        switch (profileTypeEl.getTagName()) {
            case "fromXml":
                return buildXmlProfile(profileTypeEl, util);
            case "fromText":
                return buildTextProfile(profileTypeEl, util);
            default:
                throw new ValidatorConfigurationException("neznámý element " + profileTypeEl.getTagName());
        }
    }

    private J2kProfile buildTextProfile(Element rootEl, ExternalUtil util) throws ValidatorConfigurationException {
        J2kTextProfile profile = new J2kTextProfile(externalUtilManager, util);
        //validations
        List<Element> validationEls = XmlUtils.getChildrenElementsByName(rootEl, "validation");
        for (Element validationEl : validationEls) {
            String validationName = validationEl.getAttribute("name");
            //extraction
            Element xmlDataExtractionEl = XmlUtils.getFirstChildElementsByName(validationEl, "textDataExtraction");
            DataExtraction dataExtraction = buildTextDataExtraction(xmlDataExtractionEl);
            //rules
            Element rulesEl = XmlUtils.getFirstChildElementsByName(validationEl, "rules");
            List<DataRule> dataRules = new ArrayList<>();
            List<Element> ruleEls = XmlUtils.getChildrenElements(rulesEl);
            for (Element ruleEl : ruleEls) {
                dataRules.add(buildRule(validationName, ruleEl));
            }
            Validation validation = new Validation(dataExtraction, dataRules);
            profile.addValidation(validation);
        }
        return profile;
    }

    private J2kProfile buildXmlProfile(Element rootEl, ExternalUtil util) throws ValidatorConfigurationException {
        J2kXmlProfile profile = new J2kXmlProfile(externalUtilManager, util);
        //namespaces
        Element namespacesEl = XmlUtils.getFirstChildElementsByName(rootEl, "namespaces");
        if (namespacesEl != null) {
            List<Element> nsEls = XmlUtils.getChildrenElementsByName(namespacesEl, "namespace");
            for (Element nsEl : nsEls) {
                String prefix = nsEl.getAttribute("prefix");
                String url = nsEl.getTextContent().trim();
                profile.addNamespace(prefix, url);
            }
            Element defaultNamespaceEl = XmlUtils.getFirstChildElementsByName(namespacesEl, "defaultNamespace");
            if (defaultNamespaceEl != null) {
                profile.addNamespace(null, defaultNamespaceEl.getTextContent().trim());
            }
        }
        //validations
        List<Element> validationEls = XmlUtils.getChildrenElementsByName(rootEl, "validation");
        for (Element validationEl : validationEls) {
            String validationName = validationEl.getAttribute("name");
            //extraction
            Element xmlDataExtractionEl = XmlUtils.getFirstChildElementsByName(validationEl, "xmlDataExtraction");
            DataExtraction dataExtraction = buildXmlDataExtraction(profile.getNamespaceContext(), xmlDataExtractionEl);
            //rules
            Element rulesEl = XmlUtils.getFirstChildElementsByName(validationEl, "rules");
            List<DataRule> dataRules = new ArrayList<>();
            List<Element> ruleEls = XmlUtils.getChildrenElements(rulesEl);
            for (Element ruleEl : ruleEls) {
                dataRules.add(buildRule(validationName, ruleEl));
            }
            Validation validation = new Validation(dataExtraction, dataRules);
            profile.addValidation(validation);
        }
        return profile;
    }

    private DataExtraction buildTextDataExtraction(Element extractionEl) throws ValidatorConfigurationException {
        String resultTypeStr = extractionEl.getAttribute("resultType");
        ExtractionResultType resultType = ExtractionResultType.valueOf(resultTypeStr);

        Element allAppearancesEl = XmlUtils.getFirstChildElementsByName(extractionEl, "allNonempty");
        if (allAppearancesEl != null) {
            List<String> regexps = new ArrayList<>();
            List<Element> xpathEls = XmlUtils.getChildrenElementsByName(allAppearancesEl, "regexp");
            for (Element regexpEl : xpathEls) {
                regexps.add(regexpEl.getTextContent().trim());
            }
            return new AllNonemptyByRegexpDataExtraction(resultType, regexps);
        }
        throw new ValidatorConfigurationException("neznámá extrakce hodnoty: " + XmlUtils.getChildrenElements(extractionEl).get(0).getTagName());
    }

    private DataExtraction buildXmlDataExtraction(NamespaceContextImpl nsContext, Element extractionEl) throws ValidatorConfigurationException {
        String resultTypeStr = extractionEl.getAttribute("resultType");
        ExtractionResultType resultType = ExtractionResultType.valueOf(resultTypeStr);
        Element firstNonemptyEl = XmlUtils.getFirstChildElementsByName(extractionEl, "firstNonempty");
        if (firstNonemptyEl != null) {
            List<String> xpaths = new ArrayList<>();
            List<Element> xpathEls = XmlUtils.getChildrenElementsByName(firstNonemptyEl, "xpath");
            for (Element xpathEl : xpathEls) {
                xpaths.add(xpathEl.getTextContent().trim());
            }
            return new FirstNonemptyByXpathDataExctraction(resultType, nsContext, xpaths);
        }
        throw new ValidatorConfigurationException("neznámá extrakce hodnoty");
    }

    private DataRule buildRule(String validationName, Element ruleEl) throws ValidatorConfigurationException {
        //mustExist
        //mustNotExist
        //mustMatch

        switch (ruleEl.getTagName()) {
            case "mustExist": {
                return new MustExistDR(validationName);
            }
            case "mustNotExist": {
                return new MustNotExistDR(validationName);
            }
            case "mustMatchAny": {
                List<Constraint> constraints = new ArrayList<>();
                List<Element> constraintEls = XmlUtils.getChildrenElements(ruleEl);
                for (Element constraintEl : constraintEls) {
                    constraints.add(buildConstraint(constraintEl));
                }
                return new MustMatchAnyDR(validationName, constraints);
            }
            default:
                throw new ValidatorConfigurationException("nečekaný element %s", ruleEl.getTagName());
        }
    }

    private Constraint buildConstraint(Element constraintEl) {
        switch (constraintEl.getTagName()) {
            case "constant": {
                String constant = constraintEl.getTextContent().trim();
                return new ConstantConstraint(constant);
            }
            case "floatRange": {
                Element minEl = XmlUtils.getFirstChildElementsByName(constraintEl, "min");
                Float min = minEl == null ? null : Float.valueOf(minEl.getTextContent().trim());
                Element maxEl = XmlUtils.getFirstChildElementsByName(constraintEl, "max");
                Float max = maxEl == null ? null : Float.valueOf(maxEl.getTextContent().trim());
                return new FlowRangeConstraint(min, max);
            }
            case "intRange": {
                Element minEl = XmlUtils.getFirstChildElementsByName(constraintEl, "min");
                Integer min = minEl == null ? null : Integer.valueOf(minEl.getTextContent().trim());
                Element maxEl = XmlUtils.getFirstChildElementsByName(constraintEl, "max");
                Integer max = maxEl == null ? null : Integer.valueOf(maxEl.getTextContent().trim());
                return new IntRangeConstraint(min, max);
            }
            case "nTimesXRemainingY": {
                Integer n = Integer.valueOf(XmlUtils.getFirstChildElementsByName(constraintEl, "n").getTextContent().trim());
                String x = XmlUtils.getFirstChildElementsByName(constraintEl, "x").getTextContent().trim();
                String y = XmlUtils.getFirstChildElementsByName(constraintEl, "y").getTextContent().trim();
                return new NTimesXRemainingYConstraint(n, x, y);
            }
            default:
                throw new IllegalStateException("unknow constraint");
        }
    }
}
