package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.io.File;
import java.io.PrintStream;
import java.util.*;


/**
 * Created by Martin Řehánek on 2.11.16.
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

    public void run(File xmlProtocolFile,
                    PrintStream out,
                    int verbosity,
                    DevParams devParams,
                    Set<String> skippedSections,
                    ValidationState.ProgressListener progressListener,
                    ValidationState.ProgressController progressController) {
        switch (verbosity) {
            case 3:
                //vsechno, vcetne sekci a pravidel bez chyb
                run(xmlProtocolFile, out,
                        true, true, true, true,
                        devParams,
                        skippedSections,
                        progressListener, progressController);
                break;
            case 2:
                //jen sekce a pravidla s chybami a popisy jednotlivych chyb (default)
                run(xmlProtocolFile, out,
                        true, false, true, false,
                        devParams,
                        skippedSections,
                        progressListener, progressController);
                break;
            case 1:
                //jen pocty chyb v sekcich s chybami, bez popisu jednotlivych chyb
                run(xmlProtocolFile, out,
                        true, false, false, false,
                        devParams,
                        skippedSections,
                        progressListener, progressController);
                break;
            case 0:
                //jen valid/not valid
                run(xmlProtocolFile, out,
                        false, false, false, false,
                        devParams,
                        skippedSections,
                        progressListener, progressController);
                break;
            default:
                throw new IllegalStateException(String.format("Nepovolená hodnota verbosity: %d. Hodnota musí být v intervalu [0-3]", verbosity));
        }
    }

    private void run(File xmlOutputFile,
                     PrintStream out,
                     boolean printSectionsWithProblems, boolean printSectionsWithoutProblems,
                     boolean printRulesWithProblems, boolean printRulesWithoutProblems,
                     DevParams devParams,
                     Set<String> skippedSections,
                     ValidationState.ProgressListener progressListener,
                     ValidationState.ProgressController progressController
    ) {
        ValidatorProtocolTextBuilder textLogger = new ValidatorProtocolTextBuilder(out);
        boolean sectionsUnlimitted = devParams == null || devParams.getSectionsToRun() == null || devParams.getSectionsToRun().isEmpty();
        boolean noSectionsSkipped = skippedSections == null || skippedSections.isEmpty();

        ValidationState state = initState(progressListener);
        List<RulesSection> rulesSections = state.getSections();
        state.reportValidationsStarted();
        if (progressController == null || !progressController.shouldCancel()) {
            for (RulesSection section : rulesSections) {
                if (progressController != null && progressController.shouldCancel()) {
                    break;
                }
                boolean runSection = true;
                runSection &= noSectionsSkipped || !skippedSections.contains(section.getName());
                runSection &= sectionsUnlimitted || devParams.getSectionsToRun().contains(section.getName());

                if (!runSection) {
                    state.reportSectionSkipped(section);
                    if (printSectionsWithoutProblems) {
                        textLogger.logSectionSkipped(section.getName());
                    }
                } else {
                    state.reportSectionProcessingStarted(section);
                    processSection(section, state, printSectionsWithoutProblems, printSectionsWithProblems, printRulesWithoutProblems, printRulesWithProblems, textLogger, progressController);
                    if (progressController == null || !progressController.shouldCancel()) {
                        state.reportSectionProcessingFinished(section);
                    } else {
                        state.reportSectionProcessingCanceled(section);
                    }
                }
            }
            if (progressController == null || !progressController.shouldCancel()) {
                state.reportValidationsFinished();
                textLogger.logPackageSummary(state.getGlobalProblemsTotal(), state.getGlobalProblemsByLevel(), state.isValid());
                if (xmlOutputFile != null) {
                    textLogger.logXmlExportStarted(xmlOutputFile);
                    new ValidatorProtocolXmlBuilder().buildXmlOutput(xmlOutputFile, state);
                    //TODO: tohle nepatri do textoveho logu
                    textLogger.logXmlExportCreated();
                }
            } else {
                state.reportValidationsCanceled();
            }
        } else {
            state.reportValidationsCanceled();
        }
    }

    private void processSection(RulesSection section, ValidationState state,
                                boolean printSectionsWithoutProblems, boolean printSectionsWithProblems,
                                boolean printRulesWithoutProblems, boolean printRulesWithProblems,
                                ValidatorProtocolTextBuilder textLogger,
                                ValidationState.ProgressController progressController) {
        //vypocet pravidel
        for (Rule rule : state.getRules(section)) {
            if (progressController == null || !progressController.shouldCancel()) {
                state.reportRuleProcessingStarted(rule);
                if (progressController == null || !progressController.shouldCancel()) {
                    //skutecny vypocet
                    ValidationResult result = rule.getResult();
                    //ulozeni vysledku
                    state.reportRuleProcessingFinished(section, rule, result);
                } else {
                    state.reportRuleProcessingCanceled(section, rule);
                }
            } else {
                break;
            }
        }

        //log if not canceled
        if (progressController == null || !progressController.shouldCancel()) {
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
                    for (ValidationProblem error : result.getProblems()) {
                        textLogger.logRuleError(error.getLevel(), error.getMessage());
                    }
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

}
