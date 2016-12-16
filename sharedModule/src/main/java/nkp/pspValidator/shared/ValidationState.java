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
public class ValidationState {

    //TODO: handle synchronization properly!!! Will definitely be accessed from multiple threads.

    @Deprecated
    private final Engine engine; //neni potreba pristupovat k enginu, seznam pravidel si vytahnu i odsud
    private final ProgressListener progressListener;

    private long startTime;
    private long finishTime;

    //total
    private int totalProblems;
    private Map<Level, Integer> totalProblemsByLevel;
    private boolean valid;
    //sectionsBak
    private final List<RulesSection> sections;
    private final List<RulesSection> sectionsBak = new ArrayList<>();
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
    //progress
    private RulesSection sectionBeingProcessed = null;

    public ValidationState(Engine engine, ProgressListener progressListener, List<RulesSection> sections) {
        this.engine = engine;
        this.progressListener = progressListener;
        this.sections = sections;
        if (progressListener != null) {
            progressListener.onInitialized(sections);
        }
    }

    public void addSection(RulesSection section) {
        sectionsBak.add(section);
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
        sectionBeingProcessed = section;
        startTimeBySection.put(section, System.currentTimeMillis());
        if (progressListener != null) {
            progressListener.onSectionStarted(section);
            System.out.println("progressListener.onSectionStarted(section);");
        }
    }

    public void reportSectionProcessingFinished(RulesSection section) {
        finishTimeBySection.put(section, System.currentTimeMillis());
        sectionBeingProcessed = null;
        if (progressListener != null) {
            progressListener.onSectionFinished(section, getSectionProcessingDuration(section));
            System.out.println("progressListener.onSectionFinished(section);");
        }
    }

    public void reportRuleProcessingStarted(Rule rule) {
        startTimeByRule.put(rule, System.currentTimeMillis());
    }

    public void reportRuleProcessingFinished(Rule rule) {
        finishTimeByRule.put(rule, System.currentTimeMillis());
    }

    public void reportValidationsStart() {
        startTime = System.currentTimeMillis();
        if (progressListener != null) {
            progressListener.onValidationsStarted();
        }
    }

    public void reportValidationsEnd() {
        finishTime = System.currentTimeMillis();
        totalProblemsByLevel = computeTotalProblemsByLevel(sectionsBak);
        totalProblems = computeTotalProblems(totalProblemsByLevel);
        Integer errors = totalProblemsByLevel.get(Level.ERROR);
        valid = errors == 0;
        if (progressListener != null) {
            progressListener.onValidationsFinished();
        }
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

    public List<RulesSection> getSectionsBak() {
        return sectionsBak;
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

    /*public RulesSection getSectionBeingProcessed() {
        return sectionBeingProcessed;
    }*/

    public static interface ProgressListener {

        public void onValidationsStarted();

        public void onSectionStarted(RulesSection section);

        public void onSectionFinished(RulesSection section, long duration);

        public void onValidationsFinished();

        public void onInitialized(List<RulesSection> sections);
    }
}
