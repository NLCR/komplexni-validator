package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import nkp.pspValidator.gui.pojo.*;
import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by martin on 9.12.16.
 */
public class MainController extends AbstractController implements ValidationState.ProgressListener {

    private static Logger LOG = Logger.getLogger(MainController.class.getSimpleName());

    //TODO: nahradit konkretni wiki strankou
    private static final String URL_ONLINE_HELP = "https://github.com/rzeh4n/psp-validator/wiki";

    @FXML
    BorderPane container;


    //menu
    @FXML
    MenuBar menuBar;

    //status bar
    @FXML
    Label statusText;

    @FXML
    ProgressIndicator statusProgressIndicator;

    @FXML
    ImageView statusImgFinished;

    @FXML
    ImageView statusImgError;

    //sections

    @FXML
    ListView<SectionWithState> sectionList;

    //rules for section

    @FXML
    Label rulesSectionNameLbl;

    @FXML
    Label rulesSectionDescriptionLbl;

    @FXML
    ListView<RuleWithState> ruleList;

    //problems of rule

    @FXML
    Label problemsSectionNameLbl;
    @FXML
    Label problemsRuleNameLbl;
    @FXML
    Label problemsRuleDescriptionLbl;

    @FXML
    ListView<ValidationProblem> problemList;

    private ValidationStateManager validationStateManager = null;
    private SectionWithState selectedSection;
    private RuleWithState selectedRule;
    private File pspDir;

    @Override
    public void start(Stage primaryStage) throws Exception {

    }

    public void handleKeyInput(KeyEvent keyEvent) {
        //TODO: zpracovani menu
    }

    public void showOnlineHelp(ActionEvent actionEvent) {
        openUrl(URL_ONLINE_HELP);
    }

    public void showAboutApp(ActionEvent actionEvent) {
        //TODO: dialog
    }

    public void openNewValidationDialog(ActionEvent actionEvent) {
        main.showNewValidationConfigurationDialog();
    }

    private Window getWindow() {
        return container.getScene().getWindow();
    }

    /**
     * @param pspDir
     * @param focedMonographVersion   can be null
     * @param forcedPeriodicalVersion can be null
     */
    public void runPspValidation(File pspDir, String focedMonographVersion, String forcedPeriodicalVersion) {
        this.pspDir = pspDir;
        sectionList.setItems(null);
        ruleList.setItems(null);
        problemList.setItems(null);
        Task task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                //System.out.println("validating " + pspDir.getAbsolutePath() + ", mon: " + focedMonographVersion + ", per: " + forcedPeriodicalVersion);
                try {
                    updateStatus(String.format("Inicializuji balík %s.", pspDir.getAbsolutePath()), TotalState.RUNNING);
                    Dmf dmf = selectDmf(pspDir, focedMonographVersion, forcedPeriodicalVersion);
                    System.out.println(dmf);

                    FdmfConfiguration fdmfConfig = main.getValidationDataManager().getFdmfRegistry().getFdmfConfig(dmf);
                    Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspDir, main.getValidationDataManager().getImageUtilManager());
                    //PrintStream out = textAreaPrintStream();//System.out;
                    PrintStream out = null;
                    //TODO: v produkci odstranit
                    Validator.DevParams devParams = null;
                    devParams = new Validator.DevParams();
                    //devParams.getSectionsToRun().add("Bibliografická metadata");
                    devParams.getSectionsToRun().add("Identifikátory");
                    devParams.getSectionsToRun().add("Soubor CHECKSUM");
                    devParams.getSectionsToRun().add("Soubor info");
                    devParams.getSectionsToRun().add("Struktura souborů");
                    devParams.getSectionsToRun().add("Primary METS filesec");
                    //devParams.getSectionsToRun().add("JPEG 2000");

                    validator.run(null, out, true, true, true, true, devParams, MainController.this);
                    //updateStatus(String.format("Validace balíku %s hotova.", pspDir.getAbsolutePath()));
                } catch (Exception e) {
                    //TODO: handle in UI
                    e.printStackTrace();
                    updateStatus(String.format("Chyba: %s.", e.getMessage()), TotalState.ERROR);
                } finally {
                    return null;
                }
            }

            private Dmf selectDmf(File pspDir, String focedMonographVersion, String forcedPeriodicalVersion) throws Exception {
                DmfDetector dmfDetector = new DmfDetector();
                Dmf.Type type = dmfDetector.detectDmfType(pspDir);
                switch (type) {
                    case MONOGRAPH:
                        if (focedMonographVersion != null) {
                            return new Dmf(Dmf.Type.MONOGRAPH, focedMonographVersion);
                        } else {
                            String version = dmfDetector.detectDmfVersionFromInfoOrDefault(Dmf.Type.MONOGRAPH, pspDir);
                            return new Dmf(Dmf.Type.MONOGRAPH, version);
                        }
                    case PERIODICAL:
                        if (focedMonographVersion != null) {
                            return new Dmf(Dmf.Type.PERIODICAL, forcedPeriodicalVersion);
                        } else {
                            String version = dmfDetector.detectDmfVersionFromInfoOrDefault(Dmf.Type.PERIODICAL, pspDir);
                            return new Dmf(Dmf.Type.PERIODICAL, version);
                        }
                    default:
                        throw new Exception("nepodporovaný typ " + type);
                }
            }
        };
        new Thread(task).start();
    }

    private void updateStatus(String text, TotalState state) {
        Platform.runLater(() -> {
            statusText.setText(text);
            switch (state) {
                case IDLE:
                    statusProgressIndicator.setVisible(false);
                    statusImgFinished.setVisible(false);
                    statusImgError.setVisible(false);
                    break;
                case RUNNING:
                    statusProgressIndicator.setVisible(true);
                    statusImgFinished.setVisible(false);
                    statusImgError.setVisible(false);
                    break;
                case FINISHED:
                    statusProgressIndicator.setVisible(false);
                    statusImgFinished.setVisible(true);
                    statusImgError.setVisible(false);
                    break;
                case ERROR:
                    statusProgressIndicator.setVisible(false);
                    statusImgFinished.setVisible(false);
                    statusImgError.setVisible(true);
                    break;
            }
        });
    }

    @Override
    public void onValidationsFinish() {
        updateStatus(String.format("Validace balíku %s hotova.", pspDir.getAbsolutePath()), TotalState.FINISHED);
        //TODO: reenable menu
    }

    @Override
    public void onInitialization(List<RulesSection> sections, Map<RulesSection, List<Rule>> rules) {
        validationStateManager = new ValidationStateManager(sections, rules);
        Platform.runLater(() -> {
            //sections
            sectionList.setCellFactory(new Callback<ListView<SectionWithState>, ListCell<SectionWithState>>() {

                @Override
                public ListCell<SectionWithState> call(ListView<SectionWithState> list) {
                    return new ListCell<SectionWithState>() {

                        @Override
                        protected void updateItem(SectionWithState section, boolean empty) {
                            super.updateItem(section, empty);
                            if (empty || section == null) {
                                setGraphic(null);
                            } else {
                                SectionItem item = new SectionItem();
                                item.populate(section);
                                setGraphic(item.getContainer());
                            }
                        }
                    };
                }
            });
            sectionList.setItems(validationStateManager.getSectionsObservable());
            sectionList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newSection) -> {
                if (newSection == null) {
                    selectSection(null);
                } else {
                    if (!newSection.equals(selectedSection)) {
                        selectSection(newSection);
                    }
                }
            });
            //rules
            //TODO: tahle inicializace je na dvou mistech, sjednotit
            rulesSectionNameLbl.setText(null);
            rulesSectionDescriptionLbl.setText(null);
            ruleList.setItems(null);
            ruleList.setCellFactory(new Callback<ListView<RuleWithState>, ListCell<RuleWithState>>() {

                @Override
                public ListCell<RuleWithState> call(ListView<RuleWithState> list) {
                    return new ListCell<RuleWithState>() {

                        @Override
                        protected void updateItem(RuleWithState rule, boolean empty) {
                            super.updateItem(rule, empty);
                            if (empty || rule == null) {
                                setGraphic(null);
                            } else {
                                RuleItem item = new RuleItem();
                                item.populate(rule);
                                setGraphic(item.getContainer());
                            }
                        }
                    };
                }
            });
            ruleList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newRule) -> {
                if (newRule == null) {
                    selectSection(null);
                } else {
                    if (!newRule.equals(selectedRule)) {
                        selectRule(newRule);
                    }
                }
            });
            //problems
            problemList.setItems(null);
            problemList.setCellFactory(new Callback<ListView<ValidationProblem>, ListCell<ValidationProblem>>() {

                @Override
                public ListCell<ValidationProblem> call(ListView<ValidationProblem> list) {
                    return new ListCell<ValidationProblem>() {

                        @Override
                        protected void updateItem(ValidationProblem problem, boolean empty) {
                            super.updateItem(problem, empty);
                            if (empty || problem == null) {
                                setGraphic(null);
                            } else {
                                ProblemItem item = new ProblemItem();
                                item.populate(problem);
                                setGraphic(item.getContainer());
                            }
                        }
                    };
                }
            });
        });
    }

    private void selectRule(RuleWithState rule) {
        if (rule != null) {
            selectedRule = rule;
            if (selectedSection != null) {
                problemsSectionNameLbl.setText(selectedSection.getName() + ": ");
                problemsRuleNameLbl.setText(rule.getName());
                problemsRuleDescriptionLbl.setText(rule.getDescription());
                problemList.setItems(validationStateManager.getProblemsObservable(rule.getSectionId(), rule.getId()));
                problemList.refresh();
            }
        } else {
            selectedRule = null;
            problemsSectionNameLbl.setText(null);
            problemsRuleNameLbl.setText(null);
            problemsRuleDescriptionLbl.setText(null);
        }
    }

    private void selectSection(SectionWithState section) {
        if (section != null) {
            //TODO: jeste aktualizovat hlavicku u seznamu pravidel
            //TODO: bug, seznam se chova divne, pokud ma vice polozek. Treba u pravidla "Struktura souboru"
            //System.out.println("Selected section: " + section.getName());
            selectedSection = section;
            rulesSectionNameLbl.setText(section.getName());
            rulesSectionDescriptionLbl.setText(section.getDescription());
            ruleList.setItems(validationStateManager.getRulesObervable(selectedSection.getId()));
            ruleList.refresh();
        } else {
            selectedSection = null;
            rulesSectionNameLbl.setText(null);
            rulesSectionDescriptionLbl.setText(null);
        }
    }

    @Override
    public void onValidationsStart() {
        //TODO: disable menu
        updateStatus(String.format("Validuji balík %s.", pspDir.getAbsolutePath()), TotalState.RUNNING);
    }

    @Override
    public void onSectionStart(int sectionId) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.RUNNING);
        });
    }

    @Override
    public void onSectionFinish(int sectionId, long duration) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.FINISHED);
        });
    }

    @Override
    public void onRuleStart(int sectionId, int ruleId) {
        Platform.runLater(() -> {
            validationStateManager.updateRuleState(sectionId, ruleId, ProcessingState.RUNNING);
        });
    }

    @Override
    public void onRuleFinish(int sectionId, Map<Level, Integer> sectionProblemsByLevel, int sectionProblemsTotal, int ruleId, Map<Level, Integer> ruleProblemsByLevel, int ruleProblemsTotal, List<ValidationProblem> problems) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionProblems(sectionId, sectionProblemsByLevel);
            validationStateManager.updateRuleState(sectionId, ruleId, ProcessingState.FINISHED);
            validationStateManager.setRuleResults(sectionId, ruleId, ruleProblemsByLevel, problems);
        });
    }

    private enum TotalState {
        IDLE, RUNNING, FINISHED, ERROR;
    }
}
