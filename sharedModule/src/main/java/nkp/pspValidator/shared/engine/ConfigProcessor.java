package nkp.pspValidator.shared.engine;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.evaluationFunctions.EvaluationFunction;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.types.EntityType;
import nkp.pspValidator.shared.engine.types.MetadataFormat;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationFunction;
import nkp.pspValidator.shared.externalUtils.ResourceType;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;

/**
 * Created by Martin Řehánek on 29.10.16.
 */
public class ConfigProcessor {

    /*TODO: logovani*/

    public void processConfigFile(Engine engine, File configFile) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(configFile, true);
        int ruleCounter = 0;
        for (Element childEl : doc.getChildElement()) {
            String elementName = childEl.getTagName();
            switch (elementName) {
                case "pattern-def":
                    processNamedPatternDefinition(engine, childEl);
                    break;
                case "value-def":
                    processNamedValueDefinition(engine, childEl);
                    break;
                case "rules-section":
                    processRulesSectionDefinition(engine, childEl, ruleCounter++);
                    break;
                case "namespaces":
                    processNamespaces(engine, childEl);
                default:
                    //nothing
                    //System.out.println(String.format("ignoring element %s", elementName));
            }
        }
    }

    private void processNamespaces(Engine engine, Element childEl) {
        Element altoEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-alto").get(0);
        engine.defineNamespace("alto", altoEl.getTextContent());
        Element cmdEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-copyright_md").get(0);
        engine.defineNamespace("cmd", cmdEl.getTextContent());
        Element cdEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-dublin_core").get(0);
        engine.defineNamespace("dc", cdEl.getTextContent());
        Element metsEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-mets").get(0);
        engine.defineNamespace("mets", metsEl.getTextContent());
        Element mixEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-mix").get(0);
        engine.defineNamespace("mix", mixEl.getTextContent());
        Element modsEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-mods").get(0);
        engine.defineNamespace("mods", modsEl.getTextContent());
        Element oaiDcEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-oai_dc").get(0);
        engine.defineNamespace("oai_dc", oaiDcEl.getTextContent());
        Element premisEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-premis").get(0);
        engine.defineNamespace("premis", premisEl.getTextContent());
        Element xlinkEl = XmlUtils.getChildrenElementsByName(childEl, "namespace-xlink").get(0);
        engine.defineNamespace("xlink", xlinkEl.getTextContent());
    }

    private void processRulesSectionDefinition(Engine engine, Element rulesSectionEl, Integer ruleId) throws ValidatorConfigurationException {
        String name = rulesSectionEl.getAttribute("name");
        //System.out.println(String.format("processing rule-section %s'", name));
        String description = null;
        String descriptionFromEl = rulesSectionEl.getAttribute("description");
        if (descriptionFromEl != null && !descriptionFromEl.trim().isEmpty()) {
            description = descriptionFromEl.trim();
        }
        RulesSection section = engine.buildRuleSection(ruleId, name, description);
        section.setEnabled(parseBooleanAttribute("enabled", true));
        engine.registerRuleSection(section);
        //rules
        List<Element> ruleEls = XmlUtils.getChildrenElementsByName(rulesSectionEl, "rule");
        int ruleCounter = 0;
        for (Element ruleEl : ruleEls) {
            processRule(engine, section, ruleCounter++, ruleEl);
        }
    }

    private void processRule(Engine engine, RulesSection section, int ruleId, Element ruleEl) throws ValidatorConfigurationException {
        //name
        String name = ruleEl.getAttribute("name");
        Element validationEl = XmlUtils.getChildrenElementsByName(ruleEl, "validation").get(0);
        ValidationFunction function = parseValidationFunction(engine, validationEl);
        //description
        List<Element> descriptionEls = XmlUtils.getChildrenElementsByName(ruleEl, "description");
        String description = null;
        if (!descriptionEls.isEmpty()) {
            description = descriptionEls.get(0).getTextContent().replaceAll("\\s+", " ").trim();
        }
        Rule rule = new Rule(section.getId(), ruleId, name, description, function);
        //System.out.println(String.format("registering rule '%s' (%s)", name, level));
        engine.registerRule(section, rule);

    }

    private ValidationFunction parseValidationFunction(Engine engine, Element validationEl) throws ValidatorConfigurationException {
        String name = validationEl.getAttribute("functionName");
        Element paramsEl = XmlUtils.getChildrenElementsByName(validationEl, "params").get(0);
        ValidationFunction function = engine.buildValidationFunction(name);
        //params
        addParams(engine, function, paramsEl);
        return function;
    }

    private Level parseLevel(String levelStr, Level defaultLevel) {
        if (levelStr == null || levelStr.isEmpty()) {
            return defaultLevel;
        } else {
            return Level.valueOf(levelStr);
        }
    }


    private void processNamedPatternDefinition(Engine engine, Element patternEl) {
        String varName = patternEl.getAttribute("name");
        //System.out.println("processing named-pattern " + varName);
        List<Element> expressionEls = XmlUtils.getChildrenElementsByName(patternEl, "expression");
        PatternDefinition patternDefinition = engine.buildPatternDefinition();
        for (Element expressionEl : expressionEls) {
            patternDefinition.withRawExpression(toExpression(expressionEl));
        }
        engine.registerPatternDefinition(varName, patternDefinition);
    }

    private PatternExpression toExpression(Element expressionEl) {
        boolean caseSensitive = parseBooleanAttribute(expressionEl.getAttribute("caseSensitive"), true);
        String regexp = expressionEl.getTextContent();
        return new PatternExpression(caseSensitive, regexp);
    }

    private boolean parseBooleanAttribute(String attrValue, boolean defaultValue) {
        if (attrValue == null || attrValue.isEmpty()) {
            return defaultValue;
        } else {
            return Boolean.valueOf(attrValue);
        }
    }

    private void processNamedValueDefinition(Engine engine, Element valueDefEl) throws ValidatorConfigurationException {
        String varName = valueDefEl.getAttribute("name");
        ValueType varType = ValueType.valueOf(valueDefEl.getAttribute("type"));
        //System.out.println(String.format("processing named-value %s (%s) ", varName, varType));
        List<Element> efEls = XmlUtils.getChildrenElementsByName(valueDefEl, "evaluation");
        if (!efEls.isEmpty()) {//evaluation function
            //System.out.println("processing named-value - by definition");
            Element efEl = efEls.get(0);
            EvaluationFunction ef = buildEf(engine, efEl);
            ValueDefinition valueDefinition = engine.buildValueDefinition(varType, ef);
            //System.out.println(String.format("registering named-value (by definition) %s", varName));
            engine.registerValueDefinition(varName, valueDefinition);
        } else {//constant
            //System.out.println("processing named-value - by constant");
            //System.out.println(valueDefEl.toString());
            Object value = parseConstantValueDefinition(varName, valueDefEl, varType);
            //System.out.println(String.format("registering named-value (by constant)"));
            engine.registerValue(varName, new ValueEvaluation(value));
        }
    }


    private Object parseConstantValueDefinition(String varName, Element varEl, ValueType paramType) throws ValidatorConfigurationException {
        String str = varEl.getTextContent().trim();
        switch (paramType) {
            case STRING:
                return str;
            case INTEGER:
                return Integer.valueOf(str);
            case BOOLEAN:
                if ("true".equals(str.toLowerCase())) {
                    return Boolean.TRUE;
                } else if ("false".equals(str.toLowerCase())) {
                    return Boolean.FALSE;
                } else {
                    throw new IllegalArgumentException(str);
                }
            case LEVEL:
                return Level.valueOf(str);
            case RESOURCE_TYPE:
                return ResourceType.valueOf(str);
            case EXTERNAL_UTIL:
                return ExternalUtil.valueOf(str);
            case METADATA_FORMAT:
                return MetadataFormat.valueOf(str);
            case ENTITY_TYPE:
                return EntityType.valueOf(str);
            default:
                throw new ValidatorConfigurationException(
                        String.format("parametr %s: není zde možné použít parametr typu %s", varName, paramType));
        }
    }


    private EvaluationFunction buildEf(Engine engine, Element efEl) throws ValidatorConfigurationException {
        String efName = efEl.getAttribute("functionName");
        //System.out.println("buildEf (" + efName + ")");
        EvaluationFunction ef = engine.buildEvaluationFunction(efName);
        Element paramsEl = XmlUtils.getChildrenElementsByName(efEl, "params").get(0);
        addParams(engine, ef, paramsEl);
        return ef;
    }

    private void addParams(Engine engine, Function function, Element paramsEl) throws ValidatorConfigurationException {
        // value params
        List<Element> valueEls = XmlUtils.getChildrenElementsByName(paramsEl, "value");
        for (Element valueEl : valueEls) {
            String paramName = valueEl.getAttribute("name");
            //System.out.println("processing value param " + paramName);
            ValueType paramType = ValueType.valueOf(valueEl.getAttribute("type"));
            List<Element> valueRefEls = XmlUtils.getChildrenElementsByName(valueEl, "value-ref");
            List<Element> evaluationEls = XmlUtils.getChildrenElementsByName(valueEl, "evaluation");
            if (!valueRefEls.isEmpty()) { //param is reference
                Element valueRefEl = valueRefEls.get(0);
                String valueRefName = valueRefEl.getAttribute("name");
                function.withValueParamByReference(paramName, paramType, valueRefName);
            } else if (!evaluationEls.isEmpty()) { //param anonymous definition
                Element evaluationEl = evaluationEls.get(0);
                EvaluationFunction valueParamEf = buildEf(engine, evaluationEl);
                ValueEvaluation evaluation = valueParamEf.evaluate();
                function.withValueParam(paramName, paramType, evaluation);
            } else {//param is constant
                Object paramValue = parseConstantValueDefinition(paramName, valueEl, paramType);
                function.withValueParam(paramName, paramType, new ValueEvaluation(paramValue));
            }
        }
        //pattern params
        List<Element> patternEls = XmlUtils.getChildrenElementsByName(paramsEl, "pattern");
        for (Element patternEl : patternEls) {
            String paramName = patternEl.getAttribute("name");
            //System.out.println("processing pattern param " + paramName);
            List<Element> referenceEls = XmlUtils.getChildrenElementsByName(patternEl, "pattern-ref");
            List<Element> expressionEls = XmlUtils.getChildrenElementsByName(patternEl, "expression");
            if (!referenceEls.isEmpty()) {
                Element referenceEl = referenceEls.get(0);
                String varName = referenceEl.getAttribute("name");
                function.withPatternParamByReference(paramName, varName);
            } else if (!expressionEls.isEmpty()) {
                PatternDefinition patternDefinition = engine.buildPatternDefinition();
                for (Element expressionEl : expressionEls) {
                    patternDefinition.withRawExpression(toExpression(expressionEl));
                }
                function.withPatternParam(paramName, patternDefinition.evaluate());
            }
        }
    }
}
