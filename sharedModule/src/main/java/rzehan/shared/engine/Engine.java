package rzehan.shared.engine;

import rzehan.shared.engine.evaluationFunctions.*;
import rzehan.shared.engine.exceptions.ValidatorException;
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
    private final Map<String, Object> valuesByVarName = new HashMap<>();
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
        if (name.equals("PROVIDED_FILE")) {
            return new EfProvidedFile(this);
        } else if (name.equals("PROVIDED_STRING")) {
            return new EfProvidedString(this);
        } else if (name.equals("PROVIDED_INTEGER")) {
            return new EfProvidedInteger(this);
        } else if (name.equals("RETURN_FIRST_FILE_FROM_LIST")) {
            return new EfReturnFirstFileFromFileList(this);
        } else if (name.equals("FIND_FILES_IN_DIR_BY_PATTERN")) {
            return new EfFindFilesInDirByPattern(this);
        } else {
            throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }

    public ValidationFunction buildValidationFunction(String name) {
        if (name.equals("CHECK_FILE_LIST_EXACT_SIZE")) {
            return new VfCheckFileListExactSize(this);
        } else if (name.equals("CHECK_FILENAME_MATCHES_PATTERN")) {
            return new VfCheckFilenameMatchesPattern(this);
        } else if (name.equals("CHECK_ALL_FILENAMES_MATCH_PATTERN")) {
            return new VfCheckAllFilenamesMatchePattern(this);
        } else if (name.equals("CHECK_FILE_IS_DIR")) {
            return new VfCheckFileIsDir(this);
        } else if (name.equals("CHECK_FILE_IS_NOT_DIR")) {
            return new VfCheckFileIsNotDir(this);
        } else if (name.equals("CHECK_NO_FILE_IS_DIR")) {
            return new VfCheckNoFileIsDir(this);
        } else if (name.equals("CHECK_NO_OTHER_FILES_IN_DIR")) {
            return new VfCheckNoOtherFilesInDir(this);
        } else {
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

    public void registerValue(String valueVariableName, Object value) {
        //TODO: check if not defined already
        valuesByVarName.put(valueVariableName, value);
    }

    public void registerValueDefinition(String valueVariableName, ValueDefinition definition) {
        //TODO: check if not defined already
        valueDefinitionsByVarName.put(valueVariableName, definition);
    }

    public void registerPattern(String patternVariableName, Pattern pattern) {
        //TODO: check if not defined already
        patternsByVarName.put(patternVariableName, pattern);
    }


    public void registerRuleSection(RulesSection section) {
        rulesManager.addSection(section);
    }

    public void registerRule(RulesSection section, Rule rule) {
        rulesManager.addRule(section, rule);
    }

    //get methods for obtaining values, paterns, rule-sections, rules, etc

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public Object getValueFromVariable(String valueVariableName) {
        Object value = valuesByVarName.get(valueVariableName);
        if (value != null) {
            return value;
        } else {//not evaluated yet
            ValueDefinition definition = valueDefinitionsByVarName.get(valueVariableName);
            if (definition == null) {
                throw new VariableNotDefinedException(valueVariableName);
            } else {
                value = definition.evaluate();
                valuesByVarName.put(valueVariableName, value);
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


        void addSection(RulesSection section) {
            if (sections.contains(section)) {
                throw new ValidatorException(String.format("rules section %s already added", section.getName()));
            } else {
                sections.add(section);
                rulesBySection.put(section, new ArrayList<>());
            }
        }

        void addRule(RulesSection section, Rule rule) {
            if (!rulesBySection.keySet().contains(section)) {
                throw new ValidatorException(String.format("rules section %s not added yet", section.getName()));
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
