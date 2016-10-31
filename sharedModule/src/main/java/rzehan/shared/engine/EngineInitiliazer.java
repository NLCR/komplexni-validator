package rzehan.shared.engine;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
        NodeList ruleEls = rulesSectionEl.getElementsByTagName("rule");
        for (int i = 0; i < ruleEls.getLength(); i++) {
            Element ruleEl = (Element) ruleEls.item(i);
            processRule(section, ruleEl);
        }
    }

    private void processRule(RulesSection section, Element ruleEl) {
        String name = ruleEl.getAttribute("name");
        Rule.Level level = parseLevel(ruleEl.getAttribute("level"), Rule.Level.ERROR);

        Element validationEl = (Element) ruleEl.getElementsByTagName("validation").item(0);
        ValidationFunction function = parseValidationFunction(validationEl);

        Rule rule = new Rule(name, level, function);
        //description
        NodeList descriptionEls = ruleEl.getElementsByTagName("description");
        if (descriptionEls.getLength() != 0) {
            Element descEl = (Element) descriptionEls.item(0);
            rule.setDescription(descEl.getTextContent());
        }
        //System.out.println(String.format("registering rule '%s' (%s)", name, level));
        engine.registerRule(section, rule);

    }

    private ValidationFunction parseValidationFunction(Element validationEl) {
        String name = validationEl.getAttribute("functionName");
        Element paramsEl = (Element) validationEl.getElementsByTagName("params").item(0);
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
        NodeList expressionEls = patternEl.getElementsByTagName("expression");
        for (int i = 0; i < expressionEls.getLength(); i++) {
            expressions.add(toExpression((Element) expressionEls.item(i)));
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
        NodeList efEls = valueDefEl.getElementsByTagName("evaluation");
        if (efEls.getLength() != 0) {//evaluation function
            //System.out.println("registerig value - reference");
            Element efEl = (Element) efEls.item(0);
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
        Element paramsEl = (Element) efEl.getElementsByTagName("params").item(0);
        addParams(ef, paramsEl);
        return ef;
    }

    private void addParams(Function function, Element paramsEl) {
        // value params
        NodeList valueParamEls = paramsEl.getElementsByTagName("value");
        for (int i = 0; i < valueParamEls.getLength(); i++) {
            Element valueParamEl = (Element) valueParamEls.item(i);
            String paramName = valueParamEl.getAttribute("name");
            //System.out.println("processing value param " + paramName);
            ValueType paramType = ValueType.valueOf(valueParamEl.getAttribute("type"));
            NodeList valueRefEls = valueParamEl.getElementsByTagName("value-ref");
            NodeList evaluationEls = valueParamEl.getElementsByTagName("evaluation");
            if (valueRefEls.getLength() != 0) { //param is reference
                Element valueRefEl = (Element) valueRefEls.item(0);
                String valueRefName = valueRefEl.getAttribute("name");
                function.withValueParamByReference(paramName, paramType, valueRefName);
            } else if (evaluationEls.getLength() > 0) { //param anonymous definition
                Element evaluationEl = (Element) evaluationEls.item(0);
                EvaluationFunction valueParamEf = buildEf(evaluationEl);
                Object paramValue = valueParamEf.evaluate();
                function.withValueParam(paramName, paramType, paramValue);
            } else {//param is constant
                Object paramValue = parseConstantValueDefinition(valueParamEl, paramType);
                function.withValueParam(paramName, paramType, paramValue);
            }
        }
        //pattern params
        NodeList patternParamEls = paramsEl.getElementsByTagName("pattern");
        for (int i = 0; i < patternParamEls.getLength(); i++) {
            Element patternParamEl = (Element) patternParamEls.item(i);
            String paramName = patternParamEl.getAttribute("name");
            //System.out.println("processing pattern param " + paramName);
            NodeList referenceEls = patternParamEl.getElementsByTagName("pattern-ref");
            NodeList expressionsEls = patternParamEl.getElementsByTagName("expressions");
            if (referenceEls.getLength() != 0) {
                Element referenceEl = (Element) referenceEls.item(0);
                String varName = referenceEl.getAttribute("name");
                function.withPatternParamByReference(paramName, varName);
            } else if (expressionsEls.getLength() != 0) {
                Element expressionsEl = (Element) expressionsEls.item(0);
                NodeList expressionEls = expressionsEl.getElementsByTagName("expression");
                List<Pattern.Expression> expressions = new ArrayList<>();
                for (int j = 0; j < expressionEls.getLength(); j++) {
                    expressions.add(toExpression((Element) expressionEls.item(j)));
                }
                Pattern pattern = engine.buildPattern(expressions);
                function.withPatternParam(paramName, pattern);
            }
        }

    }


}
