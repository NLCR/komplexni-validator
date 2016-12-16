package nkp.pspValidator.gui.pojo;

import nkp.pspValidator.shared.engine.RulesSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 16.12.16.
 */
public class PojoFactory {

    public static List<RulesSectionWithState> buildRulesSections(List<RulesSection> sections) {
        List<RulesSectionWithState> result = new ArrayList<>(sections.size());
        for (RulesSection section : sections) {
            RulesSectionWithState withState = new RulesSectionWithState(section);
            result.add(withState);
        }
        return result;
    }
}
