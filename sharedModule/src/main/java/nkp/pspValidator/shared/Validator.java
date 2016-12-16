package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by martin on 2.11.16.
 */
public class Validator {

    private final Engine engine;

    public Validator(Engine engine) {
        this.engine = engine;
    }

    /**
     * only for tests
     *
     * @return
     */
    public Engine getEngine() {
        return engine;
    }

    public void run(File xmlOutputFile,
                    PrintStream out,
                    boolean printSectionsWithProblems, boolean printSectionsWithoutProblems,
                    boolean printRulesWithProblems, boolean printRulesWithoutProblems,
                    DevParams devParams,
                    ValidationState.ProgressListener progressListener
    ) {
        ValidatorProtocolTextBuilder textLogger = new ValidatorProtocolTextBuilder(out);
        boolean runAllSections = devParams == null || devParams.getSectionsToRun() == null || devParams.getSectionsToRun().isEmpty();
        //ValidationState state = new ValidationState(engine, progressListener);
        ValidationState state = initState(engine, progressListener);

        List<RulesSection> rulesSections = engine.getRuleSections();
        state.reportValidationsStart();
        for (RulesSection section : rulesSections) {
            boolean runSection = runAllSections || devParams.getSectionsToRun().contains(section.getName());
            if (!runSection) {
                //TODO: ve vystupu a v logu zaznamenat, ze byla sekce ignorovana
            } else {
                state.reportSectionProcessingStarted(section);
                state.addSection(section);
                //TODO: tohle se pocita
                int sectionProblemsTotal = state.getSectionProblemsSum(section);
                Map<Level, Integer> sectionProblemsByLevel = state.getSectionProblemsByLevel(section);
                boolean printSection = sectionProblemsTotal == 0 && printSectionsWithoutProblems || sectionProblemsTotal != 0 && printSectionsWithProblems;
                if (printSection) {
                    textLogger.logSectionStart(section.getName(), sectionProblemsTotal, sectionProblemsByLevel);
                }
                List<Rule> rules = engine.getRules(section);
                for (Rule rule : rules) {
                    state.reportRuleProcessingStarted(rule);
                    state.addRule(section, rule);
                    ValidationResult result = rule.getResult();
                    boolean printRule = printSection && (result.hasProblems() && printRulesWithProblems || !result.hasProblems() && printRulesWithoutProblems);
                    if (printRule) {
                        int ruleProblemsTotal = state.getRuleProblemsSum(rule);
                        Map<Level, Integer> ruleProblemsByLevel = state.getRuleProblemsByLevel(rule);
                        textLogger.logRuleStart(rule.getName(), rule.getDescription(), ruleProblemsTotal, ruleProblemsByLevel);
                        for (ValidationError error : result.getProblems()) {
                            textLogger.logRuleError(error.getLevel(), error.getMessage());
                        }
                    }
                    state.reportRuleProcessingFinished(rule);
                }
                state.reportSectionProcessingFinished(section);
            }
        }
        state.reportValidationsEnd();
        textLogger.logPackageSummary(state.getTotalProblemsSum(), state.getTotalProblemsByLevel(), state.isValid());
        if (xmlOutputFile != null) {
            textLogger.logXmlExportStarted(xmlOutputFile);
            new ValidatorProtocolXmlBuilder().buildXmlOutput(xmlOutputFile, state);
            textLogger.logXmlExportCreated();
        }
    }

    private ValidationState initState(Engine engine, ValidationState.ProgressListener progressListener) {
        List<RulesSection> sections = engine.getRuleSections();
        ValidationState state = new ValidationState(engine, progressListener, sections);
        return state;
    }

    public static class DevParams {
        private Set<String> sectionsToRun = new HashSet<>();

        public Set<String> getSectionsToRun() {
            return sectionsToRun;
        }

        public void setSectionsToRun(Set<String> sectionsToRun) {
            this.sectionsToRun = sectionsToRun;
        }
    }

}
