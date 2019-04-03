package nkp.pspValidator.shared.externalUtils.validation;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.*;
import nkp.pspValidator.shared.externalUtils.validation.extractions.AllNonemptyByRegexpDataExtraction;
import nkp.pspValidator.shared.externalUtils.validation.extractions.FirstNonemptyByRegexpDataExtraction;
import nkp.pspValidator.shared.externalUtils.validation.extractions.FirstNonemptyByXpathDataExctraction;
import nkp.pspValidator.shared.externalUtils.validation.rules.MustExistDR;
import nkp.pspValidator.shared.externalUtils.validation.rules.MustMatchAnyDR;
import nkp.pspValidator.shared.externalUtils.validation.rules.MustNotExistDR;
import nkp.pspValidator.shared.externalUtils.validation.rules.constraints.*;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 16.11.16.
 */
public class BinaryFileValidator {

    private final ExternalUtilManager externalUtilManager;
    private final Map<ResourceType, Map<ExternalUtil, BinaryFileProfile>> profiles = new HashMap<>();


    public BinaryFileValidator(ExternalUtilManager externalUtilManager) {
        this.externalUtilManager = externalUtilManager;
    }

    public boolean isUtilAvailable(ExternalUtil util) {
        return externalUtilManager.isUtilAvailable(util);
    }

    public boolean isUtilExecutionDefined(ExternalUtilExecution exec) {
        return externalUtilManager.isUtilExecutionDefined(exec);
    }

    private Map<ExternalUtil, BinaryFileProfile> getProfilesByType(ResourceType type) {
        if (!profiles.containsKey(type)) {
            profiles.put(type, new HashMap<>());
        }
        return profiles.get(type);
    }

    public void registerProfile(ResourceType type, ExternalUtil util, File profileDefinitionFile) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(profileDefinitionFile, true);
        BinaryFileProfile profile = buildProfile(util, doc.getCurrentTag());
        if (getProfilesByType(type).containsKey(util)) {
            // TODO: 2019-04-02 vyresit, docasne bude jen warning
            //throw new IllegalStateException(String.format("profil pro typ %s a utilitu %s už byl registrován: %s", type, util, profileDefinitionFile.getAbsolutePath()));
            //System.err.println(String.format("profil pro typ %s a utilitu %s už byl registrován: %s", type, util, profileDefinitionFile.getAbsolutePath()));
        }
        getProfilesByType(type).put(util, profile);
    }

    public BinaryFileProfile getProfile(ResourceType type, ExternalUtil util) {
        return getProfilesByType(type).get(util);
    }

    private BinaryFileProfile buildProfile(ExternalUtil util, Element rootEl) throws ValidatorConfigurationException {
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

    private BinaryFileProfile buildTextProfile(Element rootEl, ExternalUtil util) throws ValidatorConfigurationException {
        BinaryFileTextProfile profile = new BinaryFileTextProfile(externalUtilManager, util);
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
            Validation validation = new Validation(validationName, dataExtraction, dataRules);
            profile.addValidation(validation);
        }
        return profile;
    }

    private BinaryFileProfile buildXmlProfile(Element rootEl, ExternalUtil util) throws ValidatorConfigurationException {
        BinaryFileXmlProfile profile = new BinaryFileXmlProfile(externalUtilManager, util);
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
            Validation validation = new Validation(validationName, dataExtraction, dataRules);
            profile.addValidation(validation);
        }
        return profile;
    }

    private DataExtraction buildTextDataExtraction(Element extractionEl) throws ValidatorConfigurationException {
        String resultTypeStr = extractionEl.getAttribute("resultType");
        ExtractionResultType resultType = ExtractionResultType.valueOf(resultTypeStr);

        Element allNonemptyEl = XmlUtils.getFirstChildElementsByName(extractionEl, "allNonempty");
        Element firstNonemptyEl = XmlUtils.getFirstChildElementsByName(extractionEl, "firstNonempty");
        if (allNonemptyEl != null) {
            List<String> regexps = new ArrayList<>();
            List<Element> xpathEls = XmlUtils.getChildrenElementsByName(allNonemptyEl, "regexp");
            for (Element regexpEl : xpathEls) {
                regexps.add(regexpEl.getTextContent().trim());
            }
            return new AllNonemptyByRegexpDataExtraction(resultType, regexps);
        } else if (firstNonemptyEl != null) {
            List<String> regexps = new ArrayList<>();
            List<Element> xpathEls = XmlUtils.getChildrenElementsByName(firstNonemptyEl, "regexp");
            for (Element regexpEl : xpathEls) {
                regexps.add(regexpEl.getTextContent().trim());
            }
            return new FirstNonemptyByRegexpDataExtraction(resultType, regexps);
        } else {
            throw new ValidatorConfigurationException("neznámá extrakce hodnoty: " + XmlUtils.getChildrenElements(extractionEl).get(0).getTagName());
        }
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
        String constraintName = constraintEl.getTagName();
        switch (constraintName) {
            case "isExactly": {
                String value = constraintEl.getTextContent().trim();
                return new IsExactlyConstraint(value);
            }
            case "endsWith": {
                String suffix = constraintEl.getTextContent().trim();
                return new EndsWithConstraint(suffix);
            }
            case "isInFloatRange": {
                Element minEl = XmlUtils.getFirstChildElementsByName(constraintEl, "min");
                Float min = minEl == null ? null : Float.valueOf(minEl.getTextContent().trim());
                Element maxEl = XmlUtils.getFirstChildElementsByName(constraintEl, "max");
                Float max = maxEl == null ? null : Float.valueOf(maxEl.getTextContent().trim());
                return new IsInFloatRangeConstraint(min, max);
            }
            case "isInIntRange": {
                Element minEl = XmlUtils.getFirstChildElementsByName(constraintEl, "min");
                Integer min = minEl == null ? null : Integer.valueOf(minEl.getTextContent().trim());
                Element maxEl = XmlUtils.getFirstChildElementsByName(constraintEl, "max");
                Integer max = maxEl == null ? null : Integer.valueOf(maxEl.getTextContent().trim());
                return new IsInIntRangeConstraint(min, max);
            }
            case "nTimesXRemainingY": {
                Integer n = Integer.valueOf(XmlUtils.getFirstChildElementsByName(constraintEl, "n").getTextContent().trim());
                String x = XmlUtils.getFirstChildElementsByName(constraintEl, "x").getTextContent().trim();
                String y = XmlUtils.getFirstChildElementsByName(constraintEl, "y").getTextContent().trim();
                return new NTimesXRemainingYConstraint(n, x, y);
            }
            default:
                throw new IllegalStateException("unknown constraint " + constraintName);
        }
    }
}
