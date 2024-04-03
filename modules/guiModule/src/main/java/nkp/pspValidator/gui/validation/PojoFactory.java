package nkp.pspValidator.gui.validation;

import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 16.12.16.
 */
public class PojoFactory {

    public static List<SectionWithState> buildSections(List<RulesSection> sections) {
        List<SectionWithState> result = new ArrayList<>(sections.size());
        for (RulesSection section : sections) {
            SectionWithState withState = new SectionWithState(section);
            result.add(withState);
        }
        return result;
    }

    public static Map<Integer, List<RuleWithState>> buildRules(Map<RulesSection, List<Rule>> original) {
        Map<Integer, List<RuleWithState>> result = new HashMap<>();
        for (RulesSection section : original.keySet()) {
            List<Rule> originalList = original.get(section);
            List<RuleWithState> newList = toRulesWithState(originalList);
            result.put(section.getId(), newList);
        }
        return result;
    }

    private static List<RuleWithState> toRulesWithState(List<Rule> originalList) {
        List<RuleWithState> result = new ArrayList<>(originalList.size());
        for (Rule rule : originalList) {
            result.add(new RuleWithState(rule));
        }
        return result;
    }
}
