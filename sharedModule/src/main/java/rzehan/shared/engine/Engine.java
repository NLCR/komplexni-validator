package rzehan.shared.engine;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rzehan.shared.engine.evaluationFunctions.*;
import rzehan.shared.engine.exceptions.InvalidXPathExpressionException;
import rzehan.shared.engine.exceptions.ValidatorConfigurationException;
import rzehan.shared.engine.exceptions.XmlParsingException;
import rzehan.shared.engine.validationFunctions.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 20.10.16.
 */
public class Engine {

    private final ProvidedVarsManager providedVarsManager;

    private final Map<String, ValueDefinition> valueDefinitionsByVarName = new HashMap<>();
    private final Map<String, ValueEvaluation> valueEvaluationsByVarName = new HashMap<>();

    private final Map<String, PatternDefinition> patternDefinitionsByVarName = new HashMap<>();
    private final Map<String, PatternEvaluation> patternEvaluationsByVarName = new HashMap<>();

    private final RulesManager rulesManager = new RulesManager();

    public Engine(ProvidedVarsManager providedVarsManager) {
        this.providedVarsManager = providedVarsManager;
    }


    //build methods for creating stuff

    public ValueDefinition buildValueDefinition(ValueType type, EvaluationFunction evaluationFunction) {
        ValueDefinition definition = new ValueDefinition(type, evaluationFunction);
        return definition;
    }

    public PatternDefinition buildPatternDefinition() {
        return new PatternDefinition(this);
    }

    public PatternDefinition buildPatternDefinition(PatternExpression... expressions) {
        PatternDefinition patternDefinition = new PatternDefinition(this);
        for (PatternExpression expression : expressions) {
            patternDefinition.withRawExpression(expression);
        }
        return patternDefinition;
    }

    public EvaluationFunction buildEvaluationFunction(String name) {
        switch (name) {
            case "getProvidedFile":
                return new EfGetProvidedFile(this);
            case "getProvidedString":
                return new EfGetProvidedString(this);
            case "getProvidedInteger":
                return new EfGetProvidedInteger(this);
            case "getFirstFileFromFileList":
                return new EfGetFirstFileFromFileList(this);
            case "findFilesInDirByPattern":
                return new EfFindFilesInDirByPattern(this);
            case "getStringByXpath":
                return new EfGetStringByXpath(this);
            default:
                throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }

    public ValidationFunction buildValidationFunction(String name) throws ValidatorConfigurationException {
        switch (name) {
            case "checkFilelistHasExactSize":
                return new VfCheckFileListHasExactSize(this);
            case "checkFilenameMatchesPattern":
                return new VfCheckFilenameMatchesPattern(this);
            case "checkAllFilenamesMatchPattern":
                return new VfCheckAllFilenamesMatchPattern(this);
            case "checkFileIsDir":
                return new VfCheckFileIsDir(this);
            case "checkFileIsNotDir":
                return new VfCheckFileIsNotDir(this);
            case "checkNoFileIsDir":
                return new VfCheckNoFileIsDir(this);
            case "checkNoOtherFilesInDir":
                return new VfCheckNoOtherFilesInDir(this);
            case "checkAllFileListsHaveSameSize":
                return new VfCheckAllFileListsHaveSameSize(this);
            case "checkFilenamesLengthsSame":
                return new VfCheckFilenamesLengthsSame(this);
            case "checkChecksumFileGeneratedByGrammar":
                return new VfCheckChecksumFileGeneratedByGrammar(this);
            case "checkChecksumFileAllPathsMatchFiles":
                return new VfCheckChecksumFileAllPathsMatchFiles(this);
            case "checkChecksumFileAllChecksumsMatch":
                return new VfCheckChecksumFileAllChecksumsMatch(this);
            case "checkXmlIsWellBuilt":
                return new VfCheckXmlIsWellBuilt(this);
            case "checkXmlIsValidByXsd":
                return new VfCheckXmlIsValidByXsd(this);
            case "checkInfoFileReferencesPrimaryMets":
                return new VfCheckInfoFileReferencesPrimaryMets(this);
            default:
                throw new ValidatorConfigurationException(String.format("validační funkce %s neexistuje", name));
        }
    }

    public RulesSection buildRuleSection(String sectionName) {
        return new RulesSection(sectionName);
    }

    public Rule buildRule(String ruleName, Rule.Level error, ValidationFunction function) {
        return new Rule(ruleName, error, function);
    }

    //register methods for defining variables (values, patterns, ruleSections, rules)

    public void registerValue(String valueVariableName, ValueEvaluation valueEvaluation) {
        //TODO: check if not defined already
        valueEvaluationsByVarName.put(valueVariableName, valueEvaluation);
    }

    public void registerValueDefinition(String valueVariableName, ValueDefinition definition) {
        //TODO: check if not defined already
        valueDefinitionsByVarName.put(valueVariableName, definition);
    }

    public void registerPatternDefinition(String patternName, PatternDefinition definition) {
        //TODO: check if not defined already
        //nejlepe ValidatorConfigurationException
        patternDefinitionsByVarName.put(patternName, definition);
    }

    public void registerRuleSection(RulesSection section) throws ValidatorConfigurationException {
        //TODO: check if not defined already
        //nejlepe ValidatorConfigurationException
        rulesManager.addSection(section);
    }

    public void registerRule(RulesSection section, Rule rule) throws ValidatorConfigurationException {
        //TODO: check if not defined already
        //nejlepe ValidatorConfigurationException
        rulesManager.addRule(section, rule);
    }

    //get methods for obtaining values, patterns, rule-sections, rules, etc

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public ValueEvaluation getValueEvaluationByVariable(String varName) {
        ValueEvaluation evaluation = valueEvaluationsByVarName.get(varName);
        if (evaluation != null) {
            return evaluation;
        } else {//not evaluated yet
            ValueDefinition definition = valueDefinitionsByVarName.get(varName);
            if (definition == null) {
                return new ValueEvaluation(null, String.format("proměnná pro hodnotu %s není definována", varName));
            } else {
                evaluation = definition.evaluate();
                valueEvaluationsByVarName.put(varName, evaluation);
                return evaluation;
            }
        }
    }

    public PatternEvaluation getPatternEvaluationByVariable(String varName) {
        PatternEvaluation evaluation = patternEvaluationsByVarName.get(varName);
        if (evaluation != null) {
            return evaluation;
        } else {
            PatternDefinition definition = patternDefinitionsByVarName.get(varName);
            if (definition == null) {
                return new PatternEvaluation(null, String.format("proměnná pro vzor %s není definována", varName));
            } else {
                evaluation = definition.evaluate();
                patternEvaluationsByVarName.put(varName, evaluation);
                return evaluation;
            }
        }
    }

    public List<RulesSection> getRuleSections() {
        return rulesManager.getSections();
    }

    public List<Rule> getRules(RulesSection section) {
        return rulesManager.getRules(section);
    }

    public PatternExpression buildRawPatternExpression(boolean caseSensitive, String regexp) {
        return new PatternExpression(caseSensitive, regexp);
    }

    //helper classes, interfaces

    public interface ProvidedVarsManager {

        File getProvidedFile(String fileId);

        String getProvidedString(String stringId);

        Integer getProvidedInteger(String intId);

    }

    private static class RulesManager {
        private final List<RulesSection> sections = new ArrayList<>();
        private final Map<RulesSection, List<Rule>> rulesBySection = new HashMap<>();


        void addSection(RulesSection section) throws ValidatorConfigurationException {
            if (sections.contains(section)) {
                throw new ValidatorConfigurationException(String.format("rules section %s already added", section.getName()));
            } else {
                sections.add(section);
                rulesBySection.put(section, new ArrayList<>());
            }
        }

        void addRule(RulesSection section, Rule rule) throws ValidatorConfigurationException {
            if (!rulesBySection.keySet().contains(section)) {
                throw new ValidatorConfigurationException(String.format("rules section %s not added yet", section.getName()));
            } else {
                rulesBySection.get(section).add(rule);
            }
        }

        List<RulesSection> getSections() {
            return sections;
        }

        List<Rule> getRules(RulesSection section) {
            return rulesBySection.get(section);
        }
    }


    public Document getXmlDocument(File file) throws XmlParsingException {
        //TODO: cachovani DOMu
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file.getAbsoluteFile());
        } catch (SAXException e) {
            throw new XmlParsingException(file, String.format("chyba parsování xml v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new XmlParsingException(file, String.format("chyba čtení v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new XmlParsingException(file, String.format("chyba konfigurace parseru při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    public XPathExpression buildExpath(String xpathExpression) throws InvalidXPathExpressionException {
        try {
            //TODO: doplnit namespacy
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException(xpathExpression, String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }

}
