package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.util.*;

/**
 * Created by martin on 15.12.16.
 */
public class ValidationState {

    //TODO: handle synchronization properly!!! Will definitely be accessed from multiple threads.

    private final ProgressListener progressListener;

    //times for durations
    private long globalStartTime;
    private long globalFinishTime;
    private final Map<RulesSection, Long> startTimeBySection = new HashMap<>();
    private final Map<RulesSection, Long> finishTimeBySection = new HashMap<>();
    private final Map<Rule, Long> startTimeByRule = new HashMap<>();
    private final Map<Rule, Long> finishTimeByRule = new HashMap<>();
    //rules and sections
    private final List<RulesSection> sections;
    private final Map<RulesSection, List<Rule>> rulesBySection;
    //results
    private final Map<Rule, ValidationResult> validationResults = new HashMap<>();
    private final Map<Level, Integer> globalProblemsByLevel = new HashMap<>();
    private int globalProblemsTotal;
    private final Map<RulesSection, Map<Level, Integer>> sectionProblemsByLevel = new HashMap<>();
    private final Map<RulesSection, Integer> sectionProblemsTotal = new HashMap<>();
    private final Map<Rule, Map<Level, Integer>> ruleProblemsByLevel = new HashMap<>();
    private final Map<Rule, Integer> ruleProblemsTotal = new HashMap<>();
    private boolean valid;


    public ValidationState(ProgressListener progressListener, List<RulesSection> sections, Map<RulesSection, List<Rule>> rules) {
        this.progressListener = progressListener;
        this.sections = sections;
        this.rulesBySection = rules;
        if (progressListener != null) {
            progressListener.onInitialization(copy(sections), copy(rules));
        }
    }

    private List<RulesSection> copy(List<RulesSection> original) {
        if (original == null) {
            return null;
        } else {
            List<RulesSection> result = new ArrayList<>();
            for (RulesSection rule : original) {
                result.add(rule.copy());
            }
            return result;
        }
    }

    private Map<RulesSection, List<Rule>> copy(Map<RulesSection, List<Rule>> original) {
        if (original == null) {
            return null;
        } else {
            Map<RulesSection, List<Rule>> result = new HashMap<>();
            for (RulesSection section : original.keySet()) {
                List<Rule> rulesOriginal = original.get(section);
                if (rulesOriginal != null) {
                    List<Rule> rulesNew = null;
                    rulesNew = new ArrayList<>(original.size());
                    for (Rule rule : rulesOriginal) {
                        rulesNew.add(rule.copyWithoutValidationFunction());
                    }
                    result.put(section.copy(), rulesNew);
                }
            }
            return result;
        }
    }

    public ValidationResult getResult(Rule rule) {
        return validationResults.get(rule);
    }

    public void reportSectionProcessingStarted(RulesSection section) {
        startTimeBySection.put(section, System.currentTimeMillis());
        if (progressListener != null) {
            progressListener.onSectionStart(section.getId());
        }
    }

    public void reportSectionProcessingFinished(RulesSection section) {
        finishTimeBySection.put(section, System.currentTimeMillis());
        if (progressListener != null) {
            progressListener.onSectionFinish(section.getId(), getSectionProcessingDuration(section));
        }
    }

    public void reportRuleProcessingStarted(Rule rule) {
        startTimeByRule.put(rule, System.currentTimeMillis());
        if (progressListener != null) {
            progressListener.onRuleStart(rule.getSectionId(), rule.getId());
        }
    }

    public void reportRuleProcessingFinished(RulesSection section, Rule rule, ValidationResult result) {
        finishTimeByRule.put(rule, System.currentTimeMillis());
        validationResults.put(rule, result);
        //rule problems
        Map<Level, Integer> problemsByLevel = computeProblemsByLevel(rule);
        ruleProblemsByLevel.put(rule, problemsByLevel);
        //rule problems total
        int problemsTotal = sum(problemsByLevel.values());
        ruleProblemsTotal.put(rule, problemsTotal);
        updateRuleSectionProblems(section, problemsByLevel, problemsTotal);
        updateTotalProblems(problemsByLevel, problemsTotal);
        if (progressListener != null) {
            progressListener.onRuleFinish(
                    rule.getSectionId(), copyProblems(sectionProblemsByLevel.get(section)), sectionProblemsTotal.get(section).intValue(),
                    rule.getId(), copyProblems(ruleProblemsByLevel.get(rule)), ruleProblemsTotal.get(rule).intValue(),
                    copyErrors(result.getProblems())
            );
        }
    }

    private List<ValidationProblem> copyErrors(List<ValidationProblem> original) {
        List<ValidationProblem> result = null;
        if (original != null) {
            result = new ArrayList<>(original.size());
            for (ValidationProblem error : original) {
                result.add(error);
            }
        }
        return result;
    }

    private Map<Level, Integer> copyProblems(Map<Level, Integer> original) {
        if (original == null) {
            return null;
        } else {
            Map<Level, Integer> result = new HashMap<>();
            for (Level level : original.keySet()) {
                Integer value = original.get(level);
                if (value != null) {
                    result.put(level, value.intValue());
                }
            }
            return result;
        }
    }


    private Map<Level, Integer> computeProblemsByLevel(Rule rule) {
        Map<Level, Integer> problemsByLevel = new HashMap<>();
        for (Level level : Level.values()) {
            problemsByLevel.put(level, 0);
        }
        if (rule.getResult().hasProblems()) {
            for (ValidationProblem error : rule.getResult().getProblems()) {
                Integer counter = problemsByLevel.get(error.getLevel());
                problemsByLevel.put(error.getLevel(), ++counter);
            }
        }
        return problemsByLevel;
    }

    private void updateRuleSectionProblems(RulesSection section, Map<Level, Integer> problemsByLevelNow, int problemsTotalNow) {
        //total
        Integer total = sectionProblemsTotal.get(section);
        if (total == null) {
            total = 0;
        }
        total += problemsTotalNow;
        sectionProblemsTotal.put(section, total);
        //by level
        Map<Level, Integer> byLevel = sectionProblemsByLevel.get(section);
        if (byLevel == null) {
            byLevel = new HashMap<>();
        }
        for (Level level : Level.values()) {
            Integer ofLevel = byLevel.get(level);
            if (ofLevel == null) {
                ofLevel = 0;
            }
            Integer ofLevelNew = problemsByLevelNow.get(level);
            if (ofLevelNew != null) {
                ofLevel += ofLevelNew;
            }
            byLevel.put(level, ofLevel);
        }
        sectionProblemsByLevel.put(section, byLevel);
    }

    private void updateTotalProblems(Map<Level, Integer> problemsByLevelNow, int problemsTotalNow) {
        globalProblemsTotal += problemsTotalNow;
        for (Level level : Level.values()) {
            Integer ofLevelGlobal = globalProblemsByLevel.get(level);
            if (ofLevelGlobal == null) {
                ofLevelGlobal = 0;
            }
            Integer problemsOfLevelNow = problemsByLevelNow.get(level);
            if (problemsOfLevelNow == null) {
                ofLevelGlobal += problemsOfLevelNow;
            }
            globalProblemsByLevel.put(level, ofLevelGlobal);
        }
    }

    private int sum(Collection<Integer> values) {
        int result = 0;
        for (Integer value : values) {
            result += value;
        }
        return result;
    }

    public void reportValidationsStarted() {
        globalStartTime = System.currentTimeMillis();
        if (progressListener != null) {
            progressListener.onValidationsStart();
        }
    }

    public void reportValidationsFinished() {
        globalFinishTime = System.currentTimeMillis();
        Integer errors = globalProblemsByLevel.get(Level.ERROR);
        valid = errors == null || errors == 0;
        if (progressListener != null) {
            progressListener.onValidationsFinish();
        }
    }

    public int getSectionProblemsTotal(RulesSection section) {
        return sectionProblemsTotal.get(section);
    }

    public Map<Level, Integer> getSectionProblemsByLevel(RulesSection section) {
        return sectionProblemsByLevel.get(section);
    }

    public boolean isValid() {
        return valid;
    }

    public Integer getGlobalProblemsTotal() {
        return globalProblemsTotal;
    }

    public Map<Level, Integer> getGlobalProblemsByLevel() {
        return globalProblemsByLevel;
    }

    public long getGlobalStartTime() {
        return globalStartTime;
    }

    public long getGlobalFinishTime() {
        return globalFinishTime;
    }


    public List<Rule> getRules(RulesSection section) {
        return rulesBySection.get(section);
    }

    public int getRuleProblemsTotal(Rule rule) {
        return ruleProblemsTotal.get(rule);
    }

    public Map<Level, Integer> getRuleProblemsByLevel(Rule rule) {
        return ruleProblemsByLevel.get(rule);
    }

    public long getSectionProcessingDuration(RulesSection section) {
        return finishTimeBySection.get(section) - startTimeBySection.get(section);
    }

    public long getRuleProcessingDuration(Rule rule) {
        return finishTimeByRule.get(rule) - startTimeByRule.get(rule);
    }

    public List<RulesSection> getSections() {
        return sections;
    }

    public static interface ProgressListener {

        public void onInitialization(List<RulesSection> sections, Map<RulesSection, List<Rule>> rules);

        public void onValidationsStart();

        public void onSectionStart(int sectionId);

        public void onSectionFinish(int sectionId, long duration);

        public void onRuleStart(int sectionId, int ruleId);

        public void onRuleFinish(int sectionId, Map<Level, Integer> sectionProblemsByLevel, int sectionProblemsTotal, int ruleId, Map<Level, Integer> ruleProblemsByLevel, int ruleProblemsTotal, List<ValidationProblem> errors);

        public void onValidationsFinish();

    }
}
