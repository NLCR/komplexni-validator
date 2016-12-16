package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.io.File;
import java.io.PrintStream;
import java.util.*;


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
     */
    @Deprecated
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
        ValidationState state = initState(progressListener);
        List<RulesSection> rulesSections = state.getSections();
        state.reportValidationsStarted();
        for (RulesSection section : rulesSections) {
            boolean runSection = runAllSections || devParams.getSectionsToRun().contains(section.getName());
            if (!runSection) {
                //TODO: ve vystupu a v logu zaznamenat, ze byla sekce ignorovana
            } else {
                state.reportSectionProcessingStarted(section);
                processSection(section, state, printSectionsWithoutProblems, printSectionsWithProblems, printRulesWithoutProblems, printRulesWithProblems, textLogger);
                state.reportSectionProcessingFinished(section);
            }
        }
        state.reportValidationsFinished();
        textLogger.logPackageSummary(state.getGlobalProblemsTotal(), state.getGlobalProblemsByLevel(), state.isValid());
        if (xmlOutputFile != null) {
            textLogger.logXmlExportStarted(xmlOutputFile);
            new ValidatorProtocolXmlBuilder().buildXmlOutput(xmlOutputFile, state);
            textLogger.logXmlExportCreated();
        }
    }

    private void processSection(RulesSection section, ValidationState state, boolean printSectionsWithoutProblems, boolean printSectionsWithProblems, boolean printRulesWithoutProblems, boolean printRulesWithProblems, ValidatorProtocolTextBuilder textLogger) {
        //vypocet pravidel
        for (Rule rule : state.getRules(section)) {
            state.reportRuleProcessingStarted(rule);
            //skutecny vypocet
            ValidationResult result = rule.getResult();
            //ulozeni vysledku
            state.reportRuleProcessingFinished(section, rule, result);
        }
        //vypis sekce, pokud potreba
        int sectionProblemsTotal = state.getSectionProblemsTotal(section);
        Map<Level, Integer> sectionProblemsByLevel = state.getSectionProblemsByLevel(section);
        boolean printSection = sectionProblemsTotal == 0 && printSectionsWithoutProblems || sectionProblemsTotal != 0 && printSectionsWithProblems;
        if (printSection) {
            textLogger.logSectionStart(section.getName(), sectionProblemsTotal, sectionProblemsByLevel);
        }
        //vypis pravidel, ktera jsou potreba
        for (Rule rule : state.getRules(section)) {
            ValidationResult result = state.getResult(rule);
            boolean printRule = printSection && (result.hasProblems() && printRulesWithProblems || !result.hasProblems() && printRulesWithoutProblems);
            if (printRule) {
                int ruleProblemsTotal = state.getRuleProblemsTotal(rule);
                Map<Level, Integer> ruleProblemsByLevel = state.getRuleProblemsByLevel(rule);
                textLogger.logRuleStart(rule.getName(), rule.getDescription(), ruleProblemsTotal, ruleProblemsByLevel);
                for (ValidationError error : result.getProblems()) {
                    textLogger.logRuleError(error.getLevel(), error.getMessage());
                }
            }
        }
    }

    private ValidationState initState(ValidationState.ProgressListener progressListener) {
        List<RulesSection> sections = engine.getRuleSections();
        Map<RulesSection, List<Rule>> rules = new HashMap<>();
        for (RulesSection section : sections) {
            rules.put(section, engine.getRules(section));
        }
        ValidationState state = new ValidationState(progressListener, sections, rules);
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

    public static interface ProgressListener {

        public void onValidationsStarted();

        public void onSectionStarted(RulesSection section);

        public void onSectionFinished(RulesSection section, long duration);

        public void onValidationsFinished();

        public void onInitialized(List<RulesSection> sections);
    }

}
