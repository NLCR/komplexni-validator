package rzehan.shared;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Rule;
import rzehan.shared.engine.RulesSection;
import rzehan.shared.engine.validationFunctions.ValidationResult;

import java.util.List;

/**
 * Created by martin on 2.11.16.
 */
public class Validator {

    private final Engine engine;


    public Validator(Engine engine) {
        this.engine = engine;
    }

    public void run(boolean printValid) {
        List<RulesSection> rulesSections = engine.getRuleSections();
        for (RulesSection section : rulesSections) {
            System.out.println("Running section " + section.getName());
            List<Rule> rules = engine.getRules(section);
            for (Rule rule : rules) {
                //System.out.println("Running rule " + rule.getName());
                ValidationResult result = rule.getResult();
                if (result.isValid()) {
                    if (printValid) {
                        System.out.println(String.format("rule %s: OK", rule.getName()));
                    }
                } else {
                    System.out.println(String.format("rule %s: %s: %s", rule.getName(), rule.getLevel(), result.getMessage()));
                    System.out.println(String.format("\t%s", rule.getDescription()));
                }
            }
        }
    }
}
