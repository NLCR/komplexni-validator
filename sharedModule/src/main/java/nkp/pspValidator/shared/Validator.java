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
        Map<Level, Integer> errorsByType = computeTotalErrorsByType(rulesSections);
        int totalErrors = computeTotalErrors(errorsByType);
        System.out.println(String.format("\nCelkem: %s", buildSummary(totalErrors, errorsByType)));
    }

    private int computeTotalErrors(Map<Level, Integer> errorByType) {
        int counter = 0;
        for (Integer counterByType : errorByType.values()) {
            counter += counterByType;
        }
        return counter;
    }

    private Map<Level, Integer> computeTotalErrorsByType(List<RulesSection> rulesSections) {
        Map<Level, Integer> errorsByType = new HashMap<>();
        for (Level level : Level.values()) {
            errorsByType.put(level, 0);
        }
        for (RulesSection section : rulesSections) {
            for (Rule rule : engine.getRules(section)) {
                for (ValidationError error : rule.getResult().getErrors()) {
                    Integer count = errorsByType.get(error.getLevel());
                    errorsByType.put(error.getLevel(), ++count);
                }
            }
        }
        return errorsByType;
    }

    private String buildErrorsSummary(List<ValidationError> errors) {
        Map<Level, Integer> errorsByType = new HashMap<>();
        for (Level level : Level.values()) {
            errorsByType.put(level, 0);
        }
        for (ValidationError error : errors) {
            Integer counter = errorsByType.get(error.getLevel());
            errorsByType.put(error.getLevel(), ++counter);
        }
        return buildSummary(errors.size(), errorsByType);
    }

    private String buildSummary(int totalErrors, Map<Level, Integer> errorsByType) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.declineErrorNumber(totalErrors));
        if (totalErrors != 0) {
            builder.append(" (");
            boolean first = true;
            for (int i = 0; i < Level.values().length; i++) {
                Level level = Level.values()[i];
                int count = errorsByType.get(level);
                if (count != 0) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append(String.format("%dx %s", count, level));
                    first = false;
                }
            }
            builder.append(")");
        }
        return builder.toString();

    }
}
