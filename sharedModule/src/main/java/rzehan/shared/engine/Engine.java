package rzehan.shared.engine;

import rzehan.shared.engine.evaluationFunctions.*;
import rzehan.shared.engine.exceptions.ValidatorConfigurationException;
import rzehan.shared.engine.exceptions.VariableNotDefinedException;
import rzehan.shared.engine.validationFunctions.*;

import java.io.File;
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
    private final Map<String, Pattern> patternsByVarName = new HashMap<>();
    private final RulesManager rulesManager = new RulesManager();

    public Engine(ProvidedVarsManager providedVarsManager) {
        this.providedVarsManager = providedVarsManager;
    }


    //build methods for creating stuff

    public ValueDefinition buildValueDefinition(ValueType type, EvaluationFunction evaluationFunction) {
        ValueDefinition definition = new ValueDefinition(type, evaluationFunction);
        return definition;
    }

    public Pattern.Expression buildExpression(boolean caseSensitive, String originalRegexp) {
        return new Pattern.Expression(this, caseSensitive, originalRegexp);
    }

    public Pattern buildPattern(Pattern.Expression... expressions) {
        return new Pattern(this, expressions);
    }

    public Pattern buildPattern(List<Pattern.Expression> expressions) {
        return new Pattern(this, expressions);
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

    public ValidationFunction buildValidationFunction(String name) {
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
            default:
                throw new RuntimeException(String.format("validační funkce %s neexistuje", name));
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

    public void registerPattern(String patternVariableName, Pattern pattern) {
        //TODO: check if not defined already
        patternsByVarName.put(patternVariableName, pattern);
    }


    public void registerRuleSection(RulesSection section) throws ValidatorConfigurationException {
        rulesManager.addSection(section);
    }

    public void registerRule(RulesSection section, Rule rule) throws ValidatorConfigurationException {
        rulesManager.addRule(section, rule);
    }

    //get methods for obtaining values, patterns, rule-sections, rules, etc

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public ValueEvaluation getValueEvaluationByVariable(String valueVariableName) {
        ValueEvaluation value = valueEvaluationsByVarName.get(valueVariableName);
        if (value != null) {
            return value;
        } else {//not evaluated yet
            ValueDefinition definition = valueDefinitionsByVarName.get(valueVariableName);
            if (definition == null) {
                return new ValueEvaluation(null, String.format("Proměnná %s není definována", valueVariableName));
            } else {
                value = definition.evaluate();
                valueEvaluationsByVarName.put(valueVariableName, value);
                return value;
            }
        }
    }

    public Pattern getPatternFromVariable(String patternVariableName) {
        return patternsByVarName.get(patternVariableName);
    }

    public List<RulesSection> getRuleSections() {
        return rulesManager.getSections();
    }

    public List<Rule> getRules(RulesSection section) {
        return rulesManager.getRules(section);
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


}
