package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class Validation {
    private final String name;
    private final DataExtraction dataExtraction;
    private final List<DataRule> dataRules;

    public Validation(String name, DataExtraction dataExtraction, List<DataRule> dataRules) {
        this.name = name;
        this.dataExtraction = dataExtraction;
        this.dataRules = dataRules;
    }

    public List<Problem> validate(Object processedOutput) throws DataExtraction.ExtractionException {
        Object data = dataExtraction.extract(processedOutput);
        for (DataRule rule : dataRules) {
            List<String> ruleErros = rule.validate(data);
            //first rule with some errors, other rules will be ignored
            if (ruleErros != null && !ruleErros.isEmpty()) {
                List<Problem> problems = new ArrayList<>(ruleErros.size());
                for (String message : ruleErros) {
                    problems.add(new Problem(rule.getLevel(), message));
                }
                return problems;
            }
        }
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    /**
     * Problém nalezený profilem binárního souboru. Úroveň null znamená úroveň z rules.xml
     * (parametr validation_problem_level), jinak úroveň pravidla z profilu (atribut level) nebo ERROR u neočekávané chyby.
     */
    public static class Problem {
        private final Level level;
        private final String message;

        public Problem(Level level, String message) {
            this.level = level;
            this.message = message;
        }

        public Level getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return level == null ? message : level + ": " + message;
        }
    }
}
