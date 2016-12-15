package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 15.12.16.
 */
public class ValidationProtocol {

    private final Engine engine;

    private long startTime;
    private long finishTime;

    //total
    private int totalProblems;
    private Map<Level, Integer> totalProblemsByLevel;
    private boolean valid;
    //sections
    private final List<RulesSection> sections = new ArrayList<>();
    private final Map<RulesSection, Map<Level, Integer>> sectionProblemsByLevel = new HashMap<>();
    private final Map<RulesSection, Integer> sectionProblemsTotal = new HashMap<>();
    private final Map<RulesSection, Long> startTimeBySection = new HashMap<>();
    private final Map<RulesSection, Long> finishTimeBySection = new HashMap<>();
    //rules
    private Map<RulesSection, List<Rule>> rulesBySection = new HashMap<>();
    private final Map<Rule, Map<Level, Integer>> ruleProblemsByLevel = new HashMap<>();
    private final Map<Rule, Integer> ruleProblemsTotal = new HashMap<>();
    private final Map<Rule, Long> startTimeByRule = new HashMap<>();
    private final Map<Rule, Long> finishTimeByRule = new HashMap<>();

    public ValidationProtocol(Engine engine) {
        this.engine = engine;
    }

    public void addSection(RulesSection section) {
        sections.add(section);
        Map<Level, Integer> problemsByLevel = computeTotalProblemsByLevel(section);
        sectionProblemsByLevel.put(section, problemsByLevel);
        int totalProblems = computeTotalProblems(problemsByLevel);
        sectionProblemsTotal.put(section, totalProblems);
    }

    public void addRule(RulesSection section, Rule rule) {
        List<Rule> rules = rulesBySection.get(section);
        if (rules == null) {
            rules = new ArrayList<>();
            rulesBySection.put(section, rules);
        }
        rules.add(rule);
        Map<Level, Integer> problemsByLevel = computeTotalProblemsByLevel(rule);
        int totalProblems = computeTotalProblems(problemsByLevel);
        ruleProblemsByLevel.put(rule, problemsByLevel);
        ruleProblemsTotal.put(rule, totalProblems);
    }

    public void reportSectionProcessingStarted(RulesSection section) {
        startTimeBySection.put(section, System.currentTimeMillis());
    }

    public void reportSectionProcessingFinished(RulesSection section) {
        finishTimeBySection.put(section, System.currentTimeMillis());
    }

    public void reportRuleProcessingStarted(Rule rule) {
        startTimeByRule.put(rule, System.currentTimeMillis());
    }

    public void reportRuleProcessingFinished(Rule rule) {
        finishTimeByRule.put(rule, System.currentTimeMillis());
    }

    public void reportValidationsStart() {
        startTime = System.currentTimeMillis();
    }

    public void reportValidationsEnd() {
        finishTime = System.currentTimeMillis();
        totalProblemsByLevel = computeTotalProblemsByLevel(sections);
        totalProblems = computeTotalProblems(totalProblemsByLevel);
        Integer errors = totalProblemsByLevel.get(Level.ERROR);
        valid = errors == 0;
    }

    private Map<Level, Integer> computeTotalProblemsByLevel(Rule rule) {
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

    private Map<Level, Integer> computeTotalProblemsByLevel(RulesSection section) {
        Map<Level, Integer> problemsByLevel = new HashMap<>();
        for (Level level : Level.values()) {
            problemsByLevel.put(level, 0);
        }
        for (Rule rule : engine.getRules(section)) {
            for (ValidationError error : rule.getResult().getProblems()) {
                Integer count = problemsByLevel.get(error.getLevel());
                problemsByLevel.put(error.getLevel(), ++count);
            }
        }
        return problemsByLevel;
    }

    private int computeTotalProblems(Map<Level, Integer> problemsByLevel) {
        int counter = 0;
        for (Integer counterByLevel : problemsByLevel.values()) {
            counter += counterByLevel;
        }
        return counter;
    }


    private Map<Level, Integer> computeTotalProblemsByLevel(List<RulesSection> rulesSections) {
        Map<Level, Integer> problemsByLevel = new HashMap<>();
        for (Level level : Level.values()) {
            problemsByLevel.put(level, 0);
        }
        for (RulesSection section : rulesSections) {
            for (Rule rule : engine.getRules(section)) {
                for (ValidationError error : rule.getResult().getProblems()) {
                    Integer count = problemsByLevel.get(error.getLevel());
                    problemsByLevel.put(error.getLevel(), ++count);
                }
            }
        }
        return problemsByLevel;
    }

    public int getSectionProblemsSum(RulesSection section) {
        return sectionProblemsTotal.get(section);
    }

    public Map<Level, Integer> getSectionProblemsByLevel(RulesSection section) {
        return sectionProblemsByLevel.get(section);
    }

    public boolean isValid() {
        return valid;
    }

    public Integer getTotalProblemsSum() {
        return totalProblems;
    }

    public Map<Level, Integer> getTotalProblemsByLevel() {
        return totalProblemsByLevel;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public List<RulesSection> getSections() {
        return sections;
    }

    public List<Rule> getRules(RulesSection section) {
        return rulesBySection.get(section);
    }

    public int getRuleProblemsSum(Rule rule) {
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
}
