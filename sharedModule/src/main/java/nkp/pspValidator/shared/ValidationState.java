package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private int globalProblemsTotal;
    private final Map<Level, Integer> globalProblemsByLevel = new HashMap<>();
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
            progressListener.onInitialized(sections);
        }
    }

    public ValidationResult getResult(Rule rule) {
        return validationResults.get(rule);
    }

    public void reportSectionProcessingStarted(RulesSection section) {
        startTimeBySection.put(section, System.currentTimeMillis());
        if (progressListener != null) {
            progressListener.onSectionStarted(section);
        }
    }

    public void reportSectionProcessingFinished(RulesSection section) {
        finishTimeBySection.put(section, System.currentTimeMillis());
        if (progressListener != null) {
            progressListener.onSectionFinished(section, getSectionProcessingDuration(section));
        }
    }

    public void reportRuleProcessingStarted(Rule rule) {
        startTimeByRule.put(rule, System.currentTimeMillis());
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
    }


    private Map<Level, Integer> computeProblemsByLevel(Rule rule) {
        Map<Level, Integer> problemsByLevel = new HashMap<>();
        for (Level level : Level.values()) {
            problemsByLevel.put(level, 0);
        }
        if (rule.getResult().hasProblems()) {
            for (ValidationError error : rule.getResult().getProblems()) {
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

    public void reportValidationsStart() {
        globalStartTime = System.currentTimeMillis();
        if (progressListener != null) {
            progressListener.onValidationsStarted();
        }
    }

    public void reportValidationsEnd() {
        globalFinishTime = System.currentTimeMillis();
        Integer errors = globalProblemsByLevel.get(Level.ERROR);
        valid = errors == null || errors == 0;
        if (progressListener != null) {
            progressListener.onValidationsFinished();
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

        public void onValidationsStarted();

        public void onSectionStarted(RulesSection section);

        public void onSectionFinished(RulesSection section, long duration);

        public void onValidationsFinished();

        public void onInitialized(List<RulesSection> sections);
    }
}
