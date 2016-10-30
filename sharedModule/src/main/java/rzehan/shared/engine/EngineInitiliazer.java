package rzehan.shared.engine;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.exceptions.ValidatorException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 29.10.16.
 */
public class EngineInitiliazer {

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
                    registerPattern(childEl);
                    break;
                case "value-def":
                    registerValue(childEl);
                    break;
                case "rules-secion":
                    processRulesSection(childEl);
                    break;
                default:
                    //nothing
                    System.out.println(String.format("ignoring element %s", elementName));
                    //throw new ValidatorException("unexpected element '" + childEl.getTagName() + "'");
            }
        }
    }

    private void processRulesSection(Element childEl) {



    }


    private void registerPattern(Element patternEl) {
        String varName = patternEl.getAttribute("name");
        System.out.println("registering pattern " + varName);
        List<Pattern.Expression> expressions = new ArrayList<>();
        //  NodeList childNodes = patternEl.getChildNodes();
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

    private void registerValue(Element valueDefEl) {
        String varName = valueDefEl.getAttribute("name");
        ValueType varType = ValueType.valueOf(valueDefEl.getAttribute("type"));
        System.out.println(String.format("registering value %s (%s) ", varName, varType));
        NodeList efEls = valueDefEl.getElementsByTagName("evaluation");
        if (efEls.getLength() != 0) {//evaluation function
            System.out.println("registerig value - reference");
            Element efEl = (Element) efEls.item(0);
            EvaluationFunction ef = buildEf(efEl);
            ValueDefinition valueDefinition = engine.buildValueDefinition(varType, ef);
            engine.registerValueDefinition(varName, valueDefinition);
        } else {//constant
            System.out.println("registerig value - constant");
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
        System.out.println("buildEf (" + efName + ")");
        EvaluationFunction ef = engine.buildEvaluationFunction(efName);
        Element paramsEl = (Element) efEl.getElementsByTagName("params").item(0);
        // value params
        NodeList valueParamEls = paramsEl.getElementsByTagName("value-param");
        for (int i = 0; i < valueParamEls.getLength(); i++) {
            Element valueParamEl = (Element) valueParamEls.item(i);
            String paramName = valueParamEl.getAttribute("name");
            System.out.println("processing value param " + paramName);
            ValueType paramType = ValueType.valueOf(valueParamEl.getAttribute("type"));
            NodeList valueRefEls = valueParamEl.getElementsByTagName("value-ref");
            NodeList evaluationEls = valueParamEl.getElementsByTagName("evaluation");
            if (valueRefEls.getLength() != 0) { //param is reference
                Element valueRefEl = (Element) valueRefEls.item(0);
                String valueRefName = valueRefEl.getAttribute("name");
                ef.withValueReference(paramName, paramType, valueRefName);
            } else if (evaluationEls.getLength() > 0) { //param anonymous definition
                Element evaluationEl = (Element) evaluationEls.item(0);
                EvaluationFunction valueParamEf = buildEf(evaluationEl);
                Object paramValue = valueParamEf.evaluate();
                ef.withValue(paramName, paramType, paramValue);
            } else {//param is constant
                Object paramValue = parseConstantValueDefinition(valueParamEl, paramType);
                ef.withValue(paramName, paramType, paramValue);
            }
        }
        //pattern params
        NodeList patternParamEls = paramsEl.getElementsByTagName("pattern-param");
        for (int i = 0; i < patternParamEls.getLength(); i++) {
            Element patternParamEl = (Element) patternParamEls.item(i);
            String paramName = patternParamEl.getAttribute("name");
            System.out.println("processing pattern param " + paramName);
            NodeList referenceEls = patternParamEl.getElementsByTagName("pattern-ref");
            NodeList expressionsEls = patternParamEl.getElementsByTagName("expressions");
            if (referenceEls.getLength() != 0) {
                Element referenceEl = (Element) referenceEls.item(0);
                String varName = referenceEl.getAttribute("name");
                ef.withPatternReference(paramName, varName);
            } else if (expressionsEls.getLength() != 0) {
                Element expressionsEl = (Element) expressionsEls.item(0);
                NodeList expressionEls = expressionsEl.getElementsByTagName("expression");
                List<Pattern.Expression> expressions = new ArrayList<>();
                for (int j = 0; j < expressionEls.getLength(); j++) {
                    expressions.add(toExpression((Element) expressionEls.item(j)));
                }
                Pattern pattern = engine.buildPattern(expressions);
                ef.withPattern(paramName, pattern);
            }
        }
        return ef;
    }


}
