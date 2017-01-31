package nkp.pspValidator.shared.biblio;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.biblio.biblioValidator.*;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.w3c.dom.Element;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static nkp.pspValidator.shared.XmlUtils.getFirstChildElementsByName;

/**
 * Created by Martin Řehánek on 2.1.17.
 */
public class BiblioTemplatesParser {

    //private static final Logger LOG = Logger.getLogger(BiblioTemplatesParser.class.getSimpleName());

    private final DictionaryManager dictionaryManager;

    public BiblioTemplatesParser(DictionaryManager dictionaryManager) {
        this.dictionaryManager = dictionaryManager;
    }

    public BiblioTemplate parseTemplate(File templateXml) throws ValidatorConfigurationException {
        //LOG.info(String.format("parsing %s", templateXml.getAbsolutePath()));
        BiblioTemplate biblioTemplate = new BiblioTemplate();
        XMLTag doc = XMLDoc.from(templateXml, true); //ignoring namespaces
        for (Element childEl : doc.getChildElement()) {
            String elementName = childEl.getTagName();
            switch (elementName) {
                case "info": //won't be machine processed
                    break;
                case "namespaces":
                    biblioTemplate.setNamespaces(parseNamespaces(childEl));
                    break;
                case "dictionaries":
                    biblioTemplate.setDeclaredDictionaries(parseDeclaredDictionaries(childEl));
                    break;
                case "rootElement":
                    ExpectedElementDefinition rootElementDef = parseElementDefinition(biblioTemplate, null, childEl);
                    rootElementDef.setRepeatable(false); //quick fix
                    biblioTemplate.setRootElementDefinition(rootElementDef);
                    break;
                default:
                    //nothing
            }
        }
        return biblioTemplate;
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

    private Set<String> parseDeclaredDictionaries(Element dictionariesEl) throws ValidatorConfigurationException {
        Set<String> result = new HashSet<>();
        List<Element> namespaceEls = XmlUtils.getChildrenElementsByName(dictionariesEl, "dictionary");
        for (Element nsEl : namespaceEls) {
            String dictionaryName = nsEl.getAttribute("name");
            //LOG.info(String.format("deklarován slovník '%s'", dictionaryName));
            if (result.contains(dictionaryName)) {
                //LOG.warning(String.format("slovník '%s' deklarován vícenásobně", dictionaryName));
            }
            if (!dictionaryManager.hasDictionary(dictionaryName)) {
                throw new ValidatorConfigurationException("kontrolovaný slovník %s nenalezen", dictionaryName);
            }
            result.add(dictionaryName);
        }
        return result;
    }

    private ExpectedElementDefinition parseElementDefinition(BiblioTemplate biblioTemplate, ExpectedElementDefinition parentDef, Element rootEl) throws ValidatorConfigurationException {
        String errorMessage = rootEl.getAttribute("errorMessage");
        ExpectedElementDefinition elementDef = new ExpectedElementDefinition(biblioTemplate, parentDef, errorMessage);
        //name
        String nsPrefix = null;
        String name = rootEl.getAttribute("name");
        if (name.contains(":")) {
            String[] tokens = name.split(":");
            nsPrefix = tokens[0];
            if (!biblioTemplate.getNamespaces().keySet().contains(nsPrefix)) {
                throw new ValidatorConfigurationException(String.format("prefix jmenného prostoru '%s' není registrován", nsPrefix));
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

        //expected attributes
        Element expectedAttributesEl = getFirstChildElementsByName(rootEl, "expectedAttributes");
        if (expectedAttributesEl != null) {
            List<Element> attributeEls = XmlUtils.getChildrenElementsByName(expectedAttributesEl, "attribute");
            List<ExpectedAttributeDefinition> expectedAttributes = new ArrayList<>(attributeEls.size());
            Set<String> expectedAttributeNames = new HashSet<>();
            for (Element attributeEl : attributeEls) {
                ExpectedAttributeDefinition definition = parseAttributeDefinition(biblioTemplate, attributeEl);
                if (expectedAttributeNames.contains(definition.getAttributeName())) {
                    throw new ValidatorConfigurationException("zdvojená definice očekávaného atributu '%s' v rámci definice očekávaného elementu '%s'", definition.getAttributeName(), elementDef.buildAbsoluteXpath());
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
                ExpectedElementDefinition newElementDefinition = parseElementDefinition(biblioTemplate, elementDef, elementEl);
                //check whether not duplicate definition
                for (ExpectedElementDefinition previousDefinition : expectedElements) {
                    if (previousDefinition.getElementName().equals(newElementDefinition.getElementName())
                            && isEqual(previousDefinition.getSpecification(), newElementDefinition.getSpecification())) {
                        throw new ValidatorConfigurationException("zdvojená definice elementu '%s' se shodnou specifikací '%s'", newElementDefinition.getElementName(), newElementDefinition.getSpecification());
                    }
                }
                expectedElements.add(newElementDefinition);
            }
            if (!expectedElements.isEmpty()) {
                elementDef.setExpectedElementDefinitions(expectedElements);
            }
        }

        //expected content
        Element expectedContentEl = getFirstChildElementsByName(rootEl, "expectedContent");
        if (expectedContentEl != null) { //nothing in element
            List<Element> expectedContentChildrenEls = XmlUtils.getChildrenElements(expectedContentEl);
            if (expectedContentChildrenEls.isEmpty()) { //empty element 'expectedContent' means any content
                elementDef.setExpectedContentDefinition(new ExpectedContentDefinitionAnything());
            } else { //some element inside 'expectedContent'
                ExpectedContentDefinition contentDefinition = parseContentDefinition(biblioTemplate, expectedContentChildrenEls.get(0));
                elementDef.setExpectedContentDefinition(contentDefinition);
            }
        } else {
            //nothing if element 'expectedContent' is not present
        }

        //TODO: co takhle pridat jeste "recomended content" jako alternativu k expected content, at uz pro atributy, nebo hodnoty. Lisilo by se to jen v tom, ze by pripadne hazelo WARNING/INFO namisto ERROR

        //extra rules
        Element extraRulesEl = getFirstChildElementsByName(rootEl, "extraRules");
        if (extraRulesEl != null) {
            List<Element> ruleEls = XmlUtils.getChildrenElements(extraRulesEl);
            if (!ruleEls.isEmpty()) {
                List<ExtraRule> extraRules = new ArrayList<>();
                for (Element ruleEl : ruleEls) {
                    switch (ruleEl.getTagName()) {
                        case "atLeastOneElementExists":
                            ExtraRule rule = parseExtraRuleAtLeastOneElementExists(biblioTemplate, ruleEl);
                            extraRules.add(rule);
                            break;
                    }
                }
                elementDef.setExtraRules(extraRules);
            }
        }

        return elementDef;
    }

    private ExtraRule parseExtraRuleAtLeastOneElementExists(BiblioTemplate templateDef, Element ruleEl) {
        String xpath = ruleEl.getAttribute("xpath");
        return new ExtraRuleAtLeastOneElementExists(xpath);
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

    private ExpectedAttributeDefinition parseAttributeDefinition(BiblioTemplate templateDef, Element attributeEl) throws ValidatorConfigurationException {
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
        //expected content
        Element expectedContentEl = getFirstChildElementsByName(attributeEl, "expectedContent");
        if (expectedContentEl == null) { //any content but not empty
            definition.setExpectedContentDefinition(new ExpectedContentDefinitionAnything());
        } else {
            List<Element> expectedContentChildrenEls = XmlUtils.getChildrenElements(expectedContentEl);
            if (expectedContentChildrenEls.isEmpty()) {
                definition.setExpectedContentDefinition(new ExpectedContentDefinitionAnything());
            } else {
                ExpectedContentDefinition contentDefinition = parseContentDefinition(templateDef, expectedContentChildrenEls.get(0));
                definition.setExpectedContentDefinition(contentDefinition);
            }
        }
        return definition;
    }

    //parametrem je primo element value, regexp, fromDictionary, oneOf
    private ExpectedContentDefinition parseContentDefinition(BiblioTemplate templateDef, Element expectedContentChildEl) throws ValidatorConfigurationException {
        switch (expectedContentChildEl.getTagName()) {
            case "value":
                return new ExpectedContentDefinitionValue(expectedContentChildEl.getTextContent().trim());
            case "regexp":
                return new ExpectedContentDefinitionRegexp(expectedContentChildEl.getTextContent().trim());
            case "fromDictionary":
                String dictionaryName = expectedContentChildEl.getAttribute("name");
                if (!templateDef.getDeclaredDictionaries().contains(dictionaryName)) {
                    throw new ValidatorConfigurationException(String.format("kontrolovaný slovník  '%s' není definován", dictionaryName));
                } else {
                    return new ExpectedContentDefinitionFromDictionary(dictionaryName, dictionaryManager.getDictionaryValues(dictionaryName));
                }
            case "oneOf":
                List<Element> oneOfChildEls = XmlUtils.getChildrenElements(expectedContentChildEl);
                List<ExpectedContentDefinition> definitions = new ArrayList<>(oneOfChildEls.size());
                for (Element element : oneOfChildEls) {
                    definitions.add(parseContentDefinition(templateDef, element));
                }
                return new ExpectedContentDefinitionOneOf(definitions);
            default:
                throw new IllegalStateException(String.format("neočekávaný element '%s'", expectedContentChildEl.getTagName()));

        }
    }

}
