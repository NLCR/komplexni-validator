package rzehan.shared.engine;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import org.w3c.dom.Element;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.exceptions.ValidatorConfigurationException;
import rzehan.shared.engine.validationFunctions.ValidationFunction;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 29.10.16.
 */
public class ConfigProcessor {

    /*TODO: logovani*/

    public void processConfigFile(Engine engine, File configFile) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(configFile, true);
        if (!"fDMF".equals(doc.getCurrentTagName())) {
            throw new ValidatorConfigurationException("root element není dmf");
        }

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
                    processRulesSectionDefinition(engine, childEl);
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

    private void processRulesSectionDefinition(Engine engine, Element rulesSectionEl) throws ValidatorConfigurationException {
        String name = rulesSectionEl.getAttribute("name");
        //System.out.println(String.format("processing rule-section %s'", name));
        RulesSection section = engine.buildRuleSection(name);
        String description = rulesSectionEl.getAttribute("description");
        if (description != null && !description.isEmpty()) {
            section.setDescription(description);
        }
        section.setEnabled(parseBooleanAttribute("enabled", true));
        engine.registerRuleSection(section);
        //rules
        List<Element> ruleEls = XmlUtils.getChildrenElementsByName(rulesSectionEl, "rule");
        for (Element ruleEl : ruleEls) {
            processRule(engine, section, ruleEl);
        }
    }

    private void processRule(Engine engine, RulesSection section, Element ruleEl) throws ValidatorConfigurationException {
        String name = ruleEl.getAttribute("name");
        Rule.Level level = parseLevel(ruleEl.getAttribute("level"), Rule.Level.ERROR);
        Element validationEl = XmlUtils.getChildrenElementsByName(ruleEl, "validation").get(0);
        ValidationFunction function = parseValidationFunction(engine, validationEl);
        Rule rule = new Rule(name, level, function);
        //description
        List<Element> descriptionEls = XmlUtils.getChildrenElementsByName(ruleEl, "description");
        if (!descriptionEls.isEmpty()) {
            rule.setDescription(descriptionEls.get(0).getTextContent().replaceAll("\\s+", " "));
        }
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

    private Rule.Level parseLevel(String levelStr, Rule.Level defaultLevel) {
        if (levelStr == null || levelStr.isEmpty()) {
            return defaultLevel;
        } else {
            return Rule.Level.valueOf(levelStr);
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
        switch (paramType) {
            case STRING:
                return varEl.getTextContent();
            case INTEGER:
                return Integer.valueOf(varEl.getTextContent());
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
            List<Element> expressionsEls = XmlUtils.getChildrenElementsByName(patternEl, "expressions");
            if (!referenceEls.isEmpty()) {
                Element referenceEl = referenceEls.get(0);
                String varName = referenceEl.getAttribute("name");
                function.withPatternParamByReference(paramName, varName);
            } else if (!expressionsEls.isEmpty()) {
                Element expressionsEl = expressionsEls.get(0);
                List<Element> expressionEls = XmlUtils.getChildrenElementsByName(expressionsEl, "expression");
                PatternDefinition patternDefinition = engine.buildPatternDefinition();
                for (Element expressionEl : expressionEls) {
                    patternDefinition.withRawExpression(toExpression(expressionEl));
                }

                //TODO: co, kdyz se uz tady bude odkazovat na promenne? Uz tady se bude vyhodnocovat, to neni dobry
                function.withPatternParam(paramName, patternDefinition.evaluate());
            }
        }

    }


}
