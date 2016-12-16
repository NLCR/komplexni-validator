package nkp.pspValidator.gui.pojo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 16.12.16.
 */
public class ValidationStateManager {

    private final ObservableList<RulesSectionWithState> sectionsObservable;
    private final Map<Integer, RulesSectionWithState> sectionsById;

    public ValidationStateManager(List<RulesSection> sections, Map<RulesSection, List<Rule>> rules) {
        List<RulesSectionWithState> sectionsWithState = PojoFactory.buildRulesSections(sections);
        this.sectionsObservable = FXCollections.observableList(sectionsWithState);
        this.sectionsById = initSectionsById(sectionsWithState);
    }

    private Map<Integer, RulesSectionWithState> initSectionsById(List<RulesSectionWithState> sectionsWithState) {
        Map<Integer, RulesSectionWithState> result = new HashMap<>();
        for (RulesSectionWithState section : sectionsWithState) {
            result.put(section.getSection().getId(), section);
        }
        return result;
    }

    public ObservableList<RulesSectionWithState> getSectionsObservable() {
        return sectionsObservable;
    }

    public void updateSectionStatus(Integer sectionId, ProcessingState state) {
        RulesSectionWithState section = sectionsById.get(sectionId);
        section.setState(state);
        updateSectionObservable(section);

    }

    private void updateSectionObservable(RulesSectionWithState section) {
        for (int i = 0; i < sectionsObservable.size(); i++) {
            RulesSectionWithState rulesSectionWithState = sectionsObservable.get(i);
            if (rulesSectionWithState.getSection().getId() == section.getSection().getId()) {
                sectionsObservable.set(i, section);
            }
        }
    }


    public void updateSectionProblems(int sectionId, Map<Level, Integer> sectionProblemsByLevel) {
        RulesSectionWithState section = sectionsById.get(sectionId);
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
}
