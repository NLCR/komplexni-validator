package nkp.pspValidator.gui.pojo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    public ValidationStateManager(List<RulesSection> sections) {
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

    public void updateSectionStatus(Integer sectionId, ProcessingState state) {
        RulesSectionWithState section = sectionsById.get(sectionId);
        section.setState(state);
        for (int i = 0; i < sectionsObservable.size(); i++) {
            RulesSectionWithState rulesSectionWithState = sectionsObservable.get(i);
            if (rulesSectionWithState.getSection().getId() == sectionId) {
                sectionsObservable.set(i, section);
            }
        }
    }

    public ObservableList<RulesSectionWithState> getSectionsObservable() {
        return sectionsObservable;
    }
}
