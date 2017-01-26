package nkp.pspValidator.gui.validation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 16.12.16.
 */
public class ValidationStateManager {


    private final Map<Integer, SectionWithState> sectionsById;
    private final Map<Pair<Integer, Integer>, RuleWithState> rulesByIds;

    //observables
    private final ObservableList<SectionWithState> sectionsObservable;
    private final Map<Integer, ObservableList<RuleWithState>> rulesObservableBySectionId;
    private final Map<Pair<Integer, Integer>, ObservableList<ValidationProblem>> problemsObservableBySectionAndRuleId = new HashMap<>();

    public ValidationStateManager(List<RulesSection> sections, Map<RulesSection, List<Rule>> rules) {
        //sections
        List<SectionWithState> sectionsWithState = PojoFactory.buildSections(sections);
        this.sectionsById = initSectionsById(sectionsWithState);
        this.sectionsObservable = FXCollections.observableList(sectionsWithState);
        //rules
        Map<Integer, List<RuleWithState>> rulesWithState = PojoFactory.buildRules(rules);
        this.rulesByIds = initRulesByIds(rulesWithState);
        this.rulesObservableBySectionId = initObservableRules(rulesWithState);
    }

    private Map<Integer, ObservableList<RuleWithState>> initObservableRules(Map<Integer, List<RuleWithState>> plainRulesBySectionId) {
        Map<Integer, ObservableList<RuleWithState>> result = new HashMap<>();
        for (Integer sectionId : plainRulesBySectionId.keySet()) {
            result.put(sectionId, FXCollections.observableList(plainRulesBySectionId.get(sectionId)));
        }
        return result;
    }

    private Map<Pair<Integer, Integer>, RuleWithState> initRulesByIds(Map<Integer, List<RuleWithState>> rulesWithState) {
        Map<Pair<Integer, Integer>, RuleWithState> result = new HashMap<>();
        for (Integer sectionId : rulesWithState.keySet()) {
            List<RuleWithState> rulesOfSection = rulesWithState.get(sectionId);
            for (RuleWithState rule : rulesOfSection) {
                result.put(new Pair<>(sectionId, rule.getId()), rule);
            }
        }
        return result;
    }

    private Map<Integer, SectionWithState> initSectionsById(List<SectionWithState> sectionsWithState) {
        Map<Integer, SectionWithState> result = new HashMap<>();
        for (SectionWithState section : sectionsWithState) {
            result.put(section.getId(), section);
        }
        return result;
    }

    public ObservableList<SectionWithState> getSectionsObservable() {
        return sectionsObservable;
    }

    public void updateSectionStatus(Integer sectionId, ProcessingState state) {
        SectionWithState section = sectionsById.get(sectionId);
        section.setState(state);
        updateSectionObservable(section);
    }

    private void updateSectionObservable(SectionWithState updated) {
        for (int i = 0; i < sectionsObservable.size(); i++) {
            SectionWithState section = sectionsObservable.get(i);
            if (section.getId() == updated.getId()) {
                sectionsObservable.set(i, updated);
                break;
            }
        }
    }


    public void updateSectionProblems(int sectionId, Map<Level, Integer> sectionProblemsByLevel) {
        SectionWithState section = sectionsById.get(sectionId);
        Integer errors = sectionProblemsByLevel.get(Level.ERROR);
        if (errors != null) {
            section.setErrors(errors);
        }
        Integer warnings = sectionProblemsByLevel.get(Level.WARNING);
        if (warnings != null) {
            section.setWarnings(warnings);
        }
        Integer infos = sectionProblemsByLevel.get(Level.INFO);
        if (infos != null) {
            section.setInfos(infos);
        }
        updateSectionObservable(section);
    }

    public ObservableList<RuleWithState> getRulesObervable(Integer selectedSectionId) {
        return rulesObservableBySectionId.get(selectedSectionId);
    }

    public void updateRule(int sectionId, int ruleId, ProcessingState state, Map<Level, Integer> ruleProblemsByLevel, List<ValidationProblem> problems) {
        RuleWithState rule = rulesByIds.get(new Pair<>(sectionId, ruleId));
        rule.setState(state);
        if (ruleProblemsByLevel != null) {
            Integer errors = ruleProblemsByLevel.get(Level.ERROR);
            if (errors != null) {
                rule.setErrors(errors);
            }
            Integer warnings = ruleProblemsByLevel.get(Level.WARNING);
            if (warnings != null) {
                rule.setWarnings(warnings);
            }
            Integer infos = ruleProblemsByLevel.get(Level.INFO);
            if (infos != null) {
                rule.setInfos(infos);
            }
        }
        updateRuleObservable(rule);
    }

    public void updateRuleState(int sectionId, int ruleId, ProcessingState state) {
        RuleWithState rule = rulesByIds.get(new Pair<>(sectionId, ruleId));
        rule.setState(state);
        updateRuleObservable(rule);
    }


    private void updateRuleObservable(RuleWithState updated) {
        ObservableList<RuleWithState> rules = rulesObservableBySectionId.get(updated.getSectionId());
        for (int i = 0; i < rules.size(); i++) {
            RuleWithState rule = rules.get(i);
            if (rule.getId() == updated.getId()) {
                rules.set(i, updated);
                break;
            }
        }
    }


    public void setRuleResults(int sectionId, int ruleId, Map<Level, Integer> ruleProblemsByLevel, List<ValidationProblem> problems) {
        RuleWithState rule = rulesByIds.get(new Pair<>(sectionId, ruleId));
        Integer errors = ruleProblemsByLevel.get(Level.ERROR);
        if (errors != null) {
            rule.setErrors(errors);
        }
        Integer warnings = ruleProblemsByLevel.get(Level.WARNING);
        if (warnings != null) {
            rule.setWarnings(warnings);
        }
        Integer infos = ruleProblemsByLevel.get(Level.INFO);
        if (infos != null) {
            rule.setInfos(infos);
        }
        problemsObservableBySectionAndRuleId.put(new Pair<>(sectionId, ruleId), FXCollections.observableList(problems));
        //TODO: mozna prekreslit
        updateRuleObservable(rule);
    }

    public ObservableList<ValidationProblem> getProblemsObservable(Integer sectionId, Integer ruleId) {

        return problemsObservableBySectionAndRuleId.get(new Pair<>(sectionId, ruleId));
    }
}
