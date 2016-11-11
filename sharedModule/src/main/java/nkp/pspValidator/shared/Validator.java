package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 2.11.16.
 */
public class Validator {

    private final Engine engine;


    public Validator(Engine engine) {
        this.engine = engine;
    }

    public void run(boolean printValid, boolean printRuleDescription, boolean printErrors) {
        List<RulesSection> rulesSections = engine.getRuleSections();
        for (RulesSection section : rulesSections) {
            System.out.println("Provádím sekci " + section.getName());
            List<Rule> rules = engine.getRules(section);
            for (Rule rule : rules) {
                //System.out.println("Running rule " + rule.getName());
                ValidationResult result = rule.getResult();
                if (result.isValid()) {
                    if (printValid) {
                        System.out.println(String.format("Pravidlo %s: OK", rule.getName()));
                    }
                } else {
                    String errorsSummary = buildErrorsSummary(result.getErrors());
                    System.out.println(String.format("Pravidlo %s: %s", rule.getName(), errorsSummary));
                    if (printRuleDescription) {
                        System.out.println(String.format("\t%s", rule.getDescription()));
                    }
                    if (printErrors) {
                        for (ValidationError error : result.getErrors()) {
                            System.out.println(String.format("\t%s: %s", error.getLevel(), error.getMessage()));
                        }
                    }
                }
            }
        }
    }

    private String buildErrorsSummary(List<ValidationError> errors) {
        Map<Level, Integer> map = new HashMap<>();
        for (Level level : Level.values()) {
            map.put(level, 0);
        }
        for (ValidationError error : errors) {
            Integer counter = map.get(error.getLevel());
            map.put(error.getLevel(), ++counter);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.declineErrorNumber(errors.size()));
        builder.append(" (");
        boolean first = true;
        for (int i = 0; i < Level.values().length; i++) {
            Level level = Level.values()[i];
            int count = map.get(level);
            if (count != 0) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(String.format("%dx %s", count, level));
                first = false;
            }
        }
        builder.append(")");
        return builder.toString();
    }
}
