package nkp.pspValidator.shared.imageUtils.validation;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ExtractionType;
import nkp.pspValidator.shared.imageUtils.ImageCopy;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.validation.extractions.FirstNonemptyDataExctraction;
import nkp.pspValidator.shared.imageUtils.validation.rules.MustExistDR;
import nkp.pspValidator.shared.imageUtils.validation.rules.MustMatchDR;
import nkp.pspValidator.shared.imageUtils.validation.rules.MustMatchOneDR;
import nkp.pspValidator.shared.imageUtils.validation.rules.MustNotExistDR;
import nkp.pspValidator.shared.imageUtils.validation.rules.constraints.ConstantConstraint;
import nkp.pspValidator.shared.imageUtils.validation.rules.constraints.FlowRangeConstraint;
import nkp.pspValidator.shared.imageUtils.validation.rules.constraints.IntRangeConstraint;
import nkp.pspValidator.shared.imageUtils.validation.rules.constraints.NTimesXRemainingYConstraint;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 16.11.16.
 */
public class ImageValidator {

    private final ImageUtilManager imageUtilManager;
    private final Map<ImageUtil, J2kProfile> masterCopyProfiles = new HashMap<>();
    private final Map<ImageUtil, J2kProfile> userCopyProfiles = new HashMap<>();


    public ImageValidator(ImageUtilManager imageUtilManager) {
        this.imageUtilManager = imageUtilManager;
    }

    public void processProfile(ImageUtil util, ImageCopy copy, File profileDefinitionFile) throws ValidatorConfigurationException {
        switch (copy) {
            case MASTER:
                processProfile(util, profileDefinitionFile, masterCopyProfiles);
                break;
            case USER:
                processProfile(util, profileDefinitionFile, userCopyProfiles);
                break;
        }
    }

    public J2kProfile getProfile(ImageCopy copy, ImageUtil util) {
        switch (copy) {
            case MASTER:
                return masterCopyProfiles.get(util);
            case USER:
                return userCopyProfiles.get(util);
            default:
                throw new IllegalStateException();
        }
    }

    private void processProfile(ImageUtil util, File profileDefinitionFile, Map<ImageUtil, J2kProfile> profiles) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(profileDefinitionFile, true);
        J2kProfile profile = buildProfile(util, doc.getCurrentTag());
        profiles.put(util, profile);
    }

    private J2kProfile buildProfile(ImageUtil util, Element rootEl) throws ValidatorConfigurationException {
        Element profileTypeEl = XmlUtils.getChilrenElements(rootEl).get(0);
        switch (profileTypeEl.getTagName()) {
            case "xmlValidations":
                return buildXmlProfile(profileTypeEl, util);
            /*TODO: jeste pro parsovani samotneho textu, treba neco jako "textValidation"*/
            default:
                throw new ValidatorConfigurationException("neznámý element " + profileTypeEl.getTagName());
        }
    }

    private J2kProfile buildXmlProfile(Element rootEl, ImageUtil util) throws ValidatorConfigurationException {
        J2kXmlProfile profile = new J2kXmlProfile(imageUtilManager, util);
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
        for (Element attributeEl : validationEls) {
            String validationName = attributeEl.getAttribute("name");
            //extraction
            Element extractionEl = XmlUtils.getFirstChildElementsByName(attributeEl, "extraction");
            DataExtraction dataExtraction = buildExtraction(profile.getNamespaceContext(), extractionEl);
            //rules
            Element validationEl = XmlUtils.getFirstChildElementsByName(attributeEl, "validation");
            List<DataRule> dataRules = new ArrayList<>();
            List<Element> ruleEls = XmlUtils.getChilrenElements(validationEl);
            for (Element ruleEl : ruleEls) {
                dataRules.add(buildRule(validationName, ruleEl));
            }
            Validation validation = new Validation(dataExtraction, dataRules);
            profile.addValidation(validation);
        }
        return profile;
    }

    private DataExtraction buildExtraction(NamespaceContextImpl nsContext, Element extractionEl) throws ValidatorConfigurationException {
        String typeStr = extractionEl.getAttribute("type");
        ExtractionType type = ExtractionType.valueOf(typeStr);

        Element firstPathAvailableEl = XmlUtils.getFirstChildElementsByName(extractionEl, "firstNonempty");
        if (firstPathAvailableEl != null) {
            List<String> xpaths = new ArrayList<>();
            List<Element> xpathEls = XmlUtils.getChildrenElementsByName(firstPathAvailableEl, "xpath");
            for (Element xpathEl : xpathEls) {
                xpaths.add(xpathEl.getTextContent().trim());
            }
            return new FirstNonemptyDataExctraction(type, nsContext, xpaths);
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
            case "mustMatch": {
                Element childEl = XmlUtils.getChilrenElements(ruleEl).get(0);
                Constraint constraint = buildConstraint(childEl);
                return new MustMatchDR(validationName, constraint);
            }
            case "mustMatchOne": {
                List<Constraint> constraints = new ArrayList<>();
                List<Element> constraintEls = XmlUtils.getChilrenElements(ruleEl);
                for (Element constraintEl : constraintEls) {
                    constraints.add(buildConstraint(constraintEl));
                }
                return new MustMatchOneDR(validationName, constraints);
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
