package rzehan.shared.engine;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import org.w3c.dom.Element;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.exceptions.ValidatorException;
import rzehan.shared.engine.validationFunctions.ValidationFunction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 29.10.16.
 */
public class EngineInitiliazer {

    /*TODO: logovani*/
    private final Engine engine;

    public EngineInitiliazer(Engine engine) {
        this.engine = engine;
    }

    public void processFile(File patternsFile) {
        XMLTag doc = XMLDoc.from(patternsFile, true);
        String currentTagName = doc.getCurrentTagName();
        if (!"dmf".equals(doc.getCurrentTagName())) {
            throw new ValidatorException("root element není dmf");
        }

        for (Element childEl : doc.getChildElement()) {
            String elementName = childEl.getTagName();
            switch (elementName) {
                case "pattern-def":
                    processPatternDefinition(childEl);
                    break;
                case "value-def":
                    processValueDefinition(childEl);
                    break;
                case "rules-section":
                    processRulesSectionDefinition(childEl);
                    break;
                default:
                    //nothing
                    //System.out.println(String.format("ignoring element %s", elementName));
                    //throw new ValidatorException("unexpected element '" + childEl.getTagName() + "'");
            }
        }
    }

    private void processRulesSectionDefinition(Element rulesSectionEl) {
        String name = rulesSectionEl.getAttribute("name");
        RulesSection section = engine.buildRuleSection(name);
        String description = rulesSectionEl.getAttribute("description");
        if (description != null && !description.isEmpty()) {
            section.setDescription(description);
        }
        section.setEnabled(parseBooleanAttribute("enabled", true));
        //System.out.println(String.format("registering rule-section '%s'", name));
        engine.registerRuleSection(section);
        //rules
        List<Element> ruleEls = XmlUtils.getChildrenElementsByName(rulesSectionEl, "rule");
        for (Element ruleEl : ruleEls) {
            processRule(section, ruleEl);
        }
    }

    private void processRule(RulesSection section, Element ruleEl) {
        String name = ruleEl.getAttribute("name");
        Rule.Level level = parseLevel(ruleEl.getAttribute("level"), Rule.Level.ERROR);

        Element validationEl = XmlUtils.getChildrenElementsByName(ruleEl, "validation").get(0);

        ValidationFunction function = parseValidationFunction(validationEl);

        Rule rule = new Rule(name, level, function);
        //description
        List<Element> descriptionEls = XmlUtils.getChildrenElementsByName(ruleEl, "description");
        if (!descriptionEls.isEmpty()) {
            rule.setDescription(descriptionEls.get(0).getTextContent());
        }
        //System.out.println(String.format("registering rule '%s' (%s)", name, level));
        engine.registerRule(section, rule);

    }

    private ValidationFunction parseValidationFunction(Element validationEl) {
        String name = validationEl.getAttribute("functionName");
        Element paramsEl = XmlUtils.getChildrenElementsByName(validationEl, "params").get(0);
        ValidationFunction function = engine.buildValidationFunction(name);
        //params
        addParams(function, paramsEl);
        return function;
    }

    private Rule.Level parseLevel(String levelStr, Rule.Level defaultLevel) {
        if (levelStr == null || levelStr.isEmpty()) {
            return defaultLevel;
        } else {
            return Rule.Level.valueOf(levelStr);
        }
    }


    private void processPatternDefinition(Element patternEl) {
        String varName = patternEl.getAttribute("name");
        //System.out.println("registering pattern " + varName);
        List<Pattern.Expression> expressions = new ArrayList<>();
        List<Element> expressionEls = XmlUtils.getChildrenElementsByName(patternEl, "expression");
        for (Element expressionEl : expressionEls) {
            expressions.add(toExpression(expressionEl));
        }
        Pattern pattern = engine.buildPattern(expressions);
        engine.registerPattern(varName, pattern);
    }

    private Pattern.Expression toExpression(Element expressionEl) {
        boolean caseSensitive = parseBooleanAttribute(expressionEl.getAttribute("caseSensitive"), true);
        String regexp = expressionEl.getTextContent();
        return engine.buildExpression(caseSensitive, regexp);
    }

    private boolean parseBooleanAttribute(String attrValue, boolean defaultValue) {
        if (attrValue == null || attrValue.isEmpty()) {
            return defaultValue;
        } else {
            return Boolean.valueOf(attrValue);
        }
    }

    private void processValueDefinition(Element valueDefEl) {
        String varName = valueDefEl.getAttribute("name");
        ValueType varType = ValueType.valueOf(valueDefEl.getAttribute("type"));
        //System.out.println(String.format("registering value %s (%s) ", varName, varType));
        List<Element> efEls = XmlUtils.getChildrenElementsByName(valueDefEl, "evaluation");
        if (!efEls.isEmpty()) {//evaluation function
            //System.out.println("registerig value - reference");
            Element efEl = efEls.get(0);
            EvaluationFunction ef = buildEf(efEl);
            ValueDefinition valueDefinition = engine.buildValueDefinition(varType, ef);
            engine.registerValueDefinition(varName, valueDefinition);
        } else {//constant
            //System.out.println("registerig value - constant");
            System.out.println(valueDefEl.toString());
            Object value = parseConstantValueDefinition(valueDefEl, varType);
            engine.registerValue(varName, value);
        }
    }


    private Object parseConstantValueDefinition(Element varEl, ValueType paramType) {
        switch (paramType) {
            case STRING:
                return varEl.getTextContent();
            case INTEGER:
                return Integer.valueOf(varEl.getTextContent());
            default:
                //todo: lepsi chybovou hlasku
                throw new ValidatorException("cannot use " + paramType + " here");
        }
    }


    private EvaluationFunction buildEf(Element efEl) {
        String efName = efEl.getAttribute("functionName");
        //System.out.println("buildEf (" + efName + ")");
        EvaluationFunction ef = engine.buildEvaluationFunction(efName);
        Element paramsEl = XmlUtils.getChildrenElementsByName(efEl, "params").get(0);
        addParams(ef, paramsEl);
        return ef;
    }

    private void addParams(Function function, Element paramsEl) {
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
                EvaluationFunction valueParamEf = buildEf(evaluationEl);
                Object paramValue = valueParamEf.evaluate();
                function.withValueParam(paramName, paramType, paramValue);
            } else {//param is constant
                Object paramValue = parseConstantValueDefinition(valueEl, paramType);
                function.withValueParam(paramName, paramType, paramValue);
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
                List<Pattern.Expression> expressions = new ArrayList<>();
                for (Element expressionEl : expressionEls) {
                    expressions.add(toExpression(expressionEl));
                }
                Pattern pattern = engine.buildPattern(expressions);
                function.withPatternParam(paramName, pattern);
            }
        }

    }


}
