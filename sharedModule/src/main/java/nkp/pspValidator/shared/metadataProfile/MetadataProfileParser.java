package nkp.pspValidator.shared.metadataProfile;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.Version;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.w3c.dom.Element;

import java.io.File;
import java.util.*;

import static nkp.pspValidator.shared.XmlUtils.getFirstChildElementsByName;

/**
 * Created by Martin Řehánek on 2.1.17.
 */
public class MetadataProfileParser {

    //private static final Logger LOG = Logger.getLogger(MetadataProfileParser.class.getSimpleName());

    private final DictionaryManager dictionaryManager;

    public MetadataProfileParser(DictionaryManager dictionaryManager) {
        this.dictionaryManager = dictionaryManager;
    }

    public MetadataProfile parseProfile(File profileXmlFile) throws ValidatorConfigurationException {
        //LOG.info(String.format("parsing %s", profileXmlFile.getAbsolutePath()));
        MetadataProfile metadataProfile = new MetadataProfile();
        XMLTag doc = XMLDoc.from(profileXmlFile, true); //ignoring namespaces
        metadataProfile.setDmf(doc.getAttribute("dmf"));
        metadataProfile.setValidatorVersion(doc.getAttribute("validatorVersion"));
        checkValidatorVersionCorrect(metadataProfile.getValidatorVersion(), profileXmlFile);
        //TODO: podobne testovat dmf

        for (Element childEl : doc.getChildElement()) {
            String elementName = childEl.getTagName();
            switch (elementName) {
                case "info": //won't be machine processed
                    break;
                case "namespaces":
                    metadataProfile.setNamespaces(parseNamespaces(childEl));
                    break;
                case "dictionaries":
                    metadataProfile.setDeclaredDictionaries(parseDeclaredDictionaries(childEl, profileXmlFile));
                    break;
                case "rootElement":
                    ExpectedElementDefinition rootElementDef = parseElementDefinition(metadataProfile, null, childEl, profileXmlFile);
                    rootElementDef.setRepeatable(false); //quick fix
                    metadataProfile.setRootElementDefinition(rootElementDef);
                    break;
                default:
                    //nothing
            }
        }
        return metadataProfile;
    }

    private void checkValidatorVersionCorrect(String versionFromProfile, File profileXmlFile) throws ValidatorConfigurationException {
        String versionPure = Version.VERSION_CODE;
        if (versionPure.contains("-")) {
            //everything after "-" is fair game, i.e. validator in version 2.0-dev-whatever will work with metadata-profile with validatorVersion="2.0"
            versionPure = versionPure.split("-")[0];
        }
        if (!versionPure.equals(versionFromProfile)) {
            throw new ValidatorConfigurationException("nesouhlasí verze validátoru - očekávaná '%s', nalezena '%s' v souboru %s", versionPure, versionFromProfile, profileXmlFile.getAbsolutePath());
        }
    }

    private Map<String, String> parseNamespaces(Element namespacesEl) {
        Map<String, String> result = new HashMap<>();
        List<Element> namespaceEls = XmlUtils.getChildrenElementsByName(namespacesEl, "namespace");
        for (Element nsEl : namespaceEls) {
            String prefix = nsEl.getAttribute("prefix");
            String url = nsEl.getTextContent().trim();
            //LOG.info(String.format("deklarován jmenný prostor s prefixem '%s' a url: %s", prefix, url));
            result.put(prefix, url);
        }
        return result;
    }

    private Set<String> parseDeclaredDictionaries(Element dictionariesEl, File file) throws ValidatorConfigurationException {
        Set<String> result = new HashSet<>();
        List<Element> namespaceEls = XmlUtils.getChildrenElementsByName(dictionariesEl, "dictionary");
        for (Element nsEl : namespaceEls) {
            String dictionaryName = nsEl.getAttribute("name");
            //LOG.info(String.format("deklarován slovník '%s'", dictionaryName));
            if (result.contains(dictionaryName)) {
                //LOG.warning(String.format("slovník '%s' deklarován vícenásobně", dictionaryName));
            }
            if (!dictionaryManager.hasDictionary(dictionaryName)) {
                throw new ValidatorConfigurationException("kontrolovaný slovník %s nenalezen (soubor %s)", dictionaryName, file.getAbsolutePath());
            }
            result.add(dictionaryName);
        }
        return result;
    }

    private ExpectedElementDefinition parseElementDefinition(MetadataProfile profile, ExpectedElementDefinition parentDef, Element rootEl, File file) throws ValidatorConfigurationException {
        String errorMessage = rootEl.getAttribute("errorMessage");
        ExpectedElementDefinition elementDef = new ExpectedElementDefinition(profile, parentDef, errorMessage);
        //name
        String nsPrefix = null;
        String name = rootEl.getAttribute("name");
        if (name.contains(":")) {
            String[] tokens = name.split(":");
            nsPrefix = tokens[0];
            if (!profile.getNamespaces().keySet().contains(nsPrefix)) {
                throw new ValidatorConfigurationException(String.format("prefix jmenného prostoru '%s' není registrován (soubor %s)", nsPrefix, file.getAbsolutePath()));
            }
            name = tokens[1];
        }
        elementDef.setElementName(name);
        elementDef.setElementNameNsPrefix(nsPrefix);

        //specification
        String specification = rootEl.getAttribute("specification");
        if (specification != null && specification.isEmpty()) {
            specification = null;
        }
        elementDef.setSpecification(specification);

        //mandatory
        boolean mandatory = true; //default is mandatory
        String mandatoryStr = rootEl.getAttribute("mandatory");
        if (mandatoryStr != null && !mandatoryStr.isEmpty()) {
            mandatory = Boolean.valueOf(mandatoryStr);
        }
        elementDef.setMandatory(mandatory);

        //repeatable
        boolean repeatable = true;
        String repeatableStr = rootEl.getAttribute("repeatable");
        if (repeatableStr != null && !repeatableStr.isEmpty()) {
            repeatable = Boolean.valueOf(repeatableStr);
        }
        elementDef.setRepeatable(repeatable);

        //ignore unexpected child elements
        boolean ignoreUnexpectedChildelements = false;
        String ignoreUnexpectedChildelementsStr = rootEl.getAttribute("ignoreUnexpectedChildElements");
        if (ignoreUnexpectedChildelementsStr != null && !ignoreUnexpectedChildelementsStr.isEmpty()) {
            ignoreUnexpectedChildelements = Boolean.valueOf(ignoreUnexpectedChildelementsStr);
        }
        elementDef.setIgnoreUnexpectedChildElements(ignoreUnexpectedChildelements);

        //ignore unexpected attributes
        boolean ignoreUnexpectedAttributes = false;
        String ignoreUnexpectedAttributesStr = rootEl.getAttribute("ignoreUnexpectedAttributes");
        if (ignoreUnexpectedAttributesStr != null && !ignoreUnexpectedAttributesStr.isEmpty()) {
            ignoreUnexpectedAttributes = Boolean.valueOf(ignoreUnexpectedAttributesStr);
        }
        elementDef.setIgnoreUnexpectedAttributes(ignoreUnexpectedAttributes);

        //expected attributes
        Element expectedAttributesEl = getFirstChildElementsByName(rootEl, "expectedAttributes");
        if (expectedAttributesEl != null) {
            List<Element> attributeEls = XmlUtils.getChildrenElementsByName(expectedAttributesEl, "attribute");
            List<ExpectedAttributeDefinition> expectedAttributes = new ArrayList<>(attributeEls.size());
            Set<String> expectedAttributeNames = new HashSet<>();
            for (Element attributeEl : attributeEls) {
                ExpectedAttributeDefinition definition = parseAttributeDefinition(profile, attributeEl, file);
                if (expectedAttributeNames.contains(definition.getAttributeName())) {
                    throw new ValidatorConfigurationException("zdvojená definice očekávaného atributu '%s' v rámci definice očekávaného elementu '%s' (soubor %s)", definition.getAttributeName(), elementDef.buildAbsoluteXpath(), file.getAbsolutePath());
                } else {
                    expectedAttributeNames.add(definition.getAttributeName());
                    expectedAttributes.add(definition);
                }
            }
            if (!expectedAttributes.isEmpty()) {
                elementDef.setExpectedAttributes(expectedAttributes);
            }
        }

        //expected elements
        Element expectedElementsEl = getFirstChildElementsByName(rootEl, "expectedElements");
        if (expectedElementsEl != null) {
            List<Element> elementEls = XmlUtils.getChildrenElementsByName(expectedElementsEl, "element");
            List<ExpectedElementDefinition> expectedElements = new ArrayList<>(elementEls.size());
            for (Element elementEl : elementEls) {
                ExpectedElementDefinition newElementDefinition = parseElementDefinition(profile, elementDef, elementEl, file);
                //check whether not duplicate definition
                for (ExpectedElementDefinition previousDefinition : expectedElements) {
                    if (previousDefinition.getElementName().equals(newElementDefinition.getElementName())
                            && isEqual(previousDefinition.getSpecification(), newElementDefinition.getSpecification())) {
                        throw new ValidatorConfigurationException("zdvojená definice elementu '%s' se shodnou specifikací '%s' (soubor %s)", newElementDefinition.getElementName(), newElementDefinition.getSpecification(), file.getAbsolutePath());
                    }
                }
                expectedElements.add(newElementDefinition);
            }
            if (!expectedElements.isEmpty()) {
                elementDef.setExpectedChildElementDefinitions(expectedElements);
            }
        }

        //content
        Element expectedContentEl = getFirstChildElementsByName(rootEl, "expectedContent");
        Element recommendedContentEl = getFirstChildElementsByName(rootEl, "recommendedContent");
        if (expectedContentEl != null) { //expected content
            List<Element> expectedContentChildrenEls = XmlUtils.getChildrenElements(expectedContentEl);
            if (expectedContentChildrenEls.isEmpty()) { //empty element 'expectedContent' means any content
                elementDef.setExpectedContentDefinition(new ContentDefinitionAnything());
            } else { //some element inside 'expectedContent'
                ContentDefinition contentDefinition = parseContentDefinition(profile, expectedContentChildrenEls.get(0), file);
                elementDef.setExpectedContentDefinition(contentDefinition);
            }
        } else if (recommendedContentEl != null) { //recommended content
            List<Element> recommendedContentChildrenEls = XmlUtils.getChildrenElements(recommendedContentEl);
            if (recommendedContentChildrenEls.isEmpty()) { //empty element 'recommendedContent' means any content
                elementDef.setRecommendedContentDefinition(new ContentDefinitionAnything());
            } else { //some element inside 'recommendedContent'
                ContentDefinition contentDefinition = parseContentDefinition(profile, recommendedContentChildrenEls.get(0), file);
                elementDef.setRecommendedContentDefinition(contentDefinition);
            }
        }

        //extra rules
        Element extraRulesEl = getFirstChildElementsByName(rootEl, "extraRules");
        if (extraRulesEl != null) {
            List<Element> ruleEls = XmlUtils.getChildrenElements(extraRulesEl);
            if (!ruleEls.isEmpty()) {
                List<ExtraRule> extraRules = new ArrayList<>();
                for (Element ruleEl : ruleEls) {
                    switch (ruleEl.getTagName()) {
                        case "existsAtLeastOnce": {
                            String xpath = ruleEl.getAttribute("xpath");
                            String description = ruleEl.getAttribute("description");
                            ExtraRule rule = new ExtraRuleExistsAtLeastOnce(xpath, description);
                            extraRules.add(rule);
                            break;
                        }
                        case "existsAtMostOnce": {
                            String xpath = ruleEl.getAttribute("xpath");
                            String description = ruleEl.getAttribute("description");
                            ExtraRule rule = new ExtraRuleExistsAtMostOnce(xpath, description);
                            extraRules.add(rule);
                            break;
                        }
                    }
                }
                elementDef.setExtraRules(extraRules);
            }
        }

        return elementDef;
    }

    private boolean isEqual(String first, String second) {
        if (first == null && second == null) {
            return true;
        } else if (first != null && second != null) {
            return first.equals(second);
        } else {
            return false;
        }
    }

    private ExpectedAttributeDefinition parseAttributeDefinition(MetadataProfile profile, Element attributeEl, File file) throws ValidatorConfigurationException {
        ExpectedAttributeDefinition definition = new ExpectedAttributeDefinition();
        //name
        String name = attributeEl.getAttribute("name");
        definition.setAttributeName(name);
        //mandatory
        boolean mandatory = true; //default is mandatory
        String mandatoryStr = attributeEl.getAttribute("mandatory");
        if (mandatoryStr != null && !mandatoryStr.isEmpty()) {
            mandatory = Boolean.valueOf(mandatoryStr);
        }
        definition.setMandatory(mandatory);
        //content
        Element expectedContentEl = getFirstChildElementsByName(attributeEl, "expectedContent");
        Element recommendedContentEl = getFirstChildElementsByName(attributeEl, "recommendedContent");
        if (expectedContentEl != null) {//expected content
            List<Element> expectedContentChildrenEls = XmlUtils.getChildrenElements(expectedContentEl);
            if (expectedContentChildrenEls.isEmpty()) {
                definition.setExpectedContentDefinition(new ContentDefinitionAnything());
            } else {
                ContentDefinition contentDefinition = parseContentDefinition(profile, expectedContentChildrenEls.get(0), file);
                definition.setExpectedContentDefinition(contentDefinition);
            }
        } else if (recommendedContentEl != null) {//recomended content
            List<Element> recommendedContentChildrenEls = XmlUtils.getChildrenElements(recommendedContentEl);
            if (recommendedContentChildrenEls.isEmpty()) {
                definition.setRecommendedContentDefinition(new ContentDefinitionAnything());
            } else {
                ContentDefinition contentDefinition = parseContentDefinition(profile, recommendedContentChildrenEls.get(0), file);
                definition.setRecommendedContentDefinition(contentDefinition);
            }
        } else {//not expected nor recomended content
            definition.setExpectedContentDefinition(new ContentDefinitionAnything());//any content but not empty
        }
        return definition;
    }

    //parametrem je primo element value, regexp, fromDictionary, oneOf
    private ContentDefinition parseContentDefinition(MetadataProfile profile, Element expectedContentChildEl, File file) throws ValidatorConfigurationException {
        switch (expectedContentChildEl.getTagName()) {
            case "value":
                return new ContentDefinitionValue(expectedContentChildEl.getTextContent().trim());
            case "regexp":
                return new ContentDefinitionRegexp(expectedContentChildEl.getTextContent().trim());
            case "fromDictionary":
                String dictionaryName = expectedContentChildEl.getAttribute("name");
                if (!profile.getDeclaredDictionaries().contains(dictionaryName)) {
                    throw new ValidatorConfigurationException(String.format("kontrolovaný slovník  '%s' není definován (soubor %s)", dictionaryName, file.getAbsolutePath()));
                } else {
                    return new ContentDefinitionFromDictionary(dictionaryName, dictionaryManager.getDictionaryValues(dictionaryName));
                }
            case "oneOf":
                List<Element> oneOfChildEls = XmlUtils.getChildrenElements(expectedContentChildEl);
                List<ContentDefinition> definitions = new ArrayList<>(oneOfChildEls.size());
                for (Element element : oneOfChildEls) {
                    definitions.add(parseContentDefinition(profile, element, file));
                }
                return new ContentDefinitionOneOf(definitions);
            default:
                throw new IllegalStateException(String.format("neočekávaný element '%s'", expectedContentChildEl.getTagName()));

        }
    }

}
