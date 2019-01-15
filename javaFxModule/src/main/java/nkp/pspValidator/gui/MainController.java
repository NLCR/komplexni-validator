package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import nkp.pspValidator.gui.validation.*;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.ValidationState;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Martin Řehánek on 9.12.16.
 */
public class MainController extends AbstractController implements ValidationState.ProgressListener, ValidationState.ProgressController {

    private static Logger LOG = Logger.getLogger(MainController.class.getSimpleName());

    //TODO: nahradit konkretni wiki strankou
    private static final String URL_ONLINE_HELP = "https://github.com/NLCR/komplexni-validator/wiki";

    @FXML
    BorderPane container;

    //menu
    @FXML
    MenuBar menuBar;
    @FXML
    Menu menuValidate;
    @FXML
    Menu menuValidation;
    @FXML
    Menu menuSettings;
    @FXML
    Menu menuShow;
    @FXML
    MenuItem showValidationResultSummaryDialogItem;
    @FXML
    MenuItem showLogTxtMenuItem;
    @FXML
    MenuItem showLogXmlMenuItem;

    //status bar
    @FXML
    Label statusText;
    @FXML
    ProgressIndicator statusProgressIndicator;
    @FXML
    ImageView statusImgFinished;
    @FXML
    ImageView statusImgError;
    @FXML
    ImageView statusImgStopped;

    //sections
    @FXML
    ListView<SectionWithState> sectionList;

    //rules of section
    @FXML
    Label rulesSectionNameLbl;
    @FXML
    Label rulesSectionDescriptionLbl;
    @FXML
    ListView<RuleWithState> ruleList;

    //problems of rule
    @FXML
    Node problemsContainer;
    @FXML
    Label problemsSectionNameLbl;
    @FXML
    Label problemsRuleNameLbl;
    @FXML
    Label problemsRuleDescriptionLbl;
    @FXML
    ProgressIndicator problemsProgressIndicator;
    @FXML
    ListView<ValidationProblem> problemList;

    //other validation data
    private Task validationTask;
    private ValidationStateManager validationStateManager = null;
    private SectionWithState selectedSection;
    private RuleWithState selectedRule;
    private File pspDir;
    private Dmf dmf;
    private File logTxtFile;
    private File logXmlFile;
    ValidationResultSummary validationResultSummary;
    private boolean cancelValidation = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
    }

    public void handleKeyInput(KeyEvent keyEvent) {
        //TODO: zpracovani menu
    }

    private Window getWindow() {
        return container.getScene().getWindow();
    }

    private void initBeforeValidation() {
        //data spojena s konkretnim behem validace
        validationStateManager = null;
        pspDir = null;
        dmf = null;
        validationResultSummary = null;
        //stav validace
        selectedSection = null;
        selectedRule = null;
        //VIEWS
        //status
        statusText.setText(null);
        //sections
        sectionList.setItems(null);
        //rules
        rulesSectionNameLbl.setText(null);
        rulesSectionDescriptionLbl.setText(null);
        ruleList.setItems(null);
        //problems of rule
        problemsContainer.setVisible(false);
        problemsProgressIndicator.setVisible(false);
        problemsSectionNameLbl.setText(null);
        problemsRuleNameLbl.setText(null);
        problemsRuleDescriptionLbl.setText(null);
        problemList.setItems(null);
        problemList.setVisible(false);
        updateStatus("", TotalState.IDLE);
    }

    /**
     * @param pspDir
     * @param preferedMonVersion can be null
     * @param preferedPerVersion can be null
     * @param forcedMonVersion   can be null
     * @param forcedPerVersion   can be null
     * @param createTxtLog
     * @param createXmlLog
     */
    public void runPspDirValidation(File pspDir,
                                    String preferedMonVersion, String preferedPerVersion, String forcedMonVersion, String forcedPerVersion,
                                    boolean createTxtLog, boolean createXmlLog, int verbosity) {
        initBeforeValidation();
        this.pspDir = pspDir;
        this.logTxtFile = createTxtLog ? buildTxtLogFile(pspDir) : null;
        this.logXmlFile = createXmlLog ? buildXmlLogFile(pspDir) : null;
        this.cancelValidation = false;

        validationTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                //System.out.println("validating " + pspDir.getAbsolutePath() + ", mon: " + focedMonographVersion + ", per: " + forcedPeriodicalVersion);
                PrintStream out = null;
                try {
                    updateStatusFromWorkerThread(String.format("Inicializuji balík %s.", pspDir.getAbsolutePath()), TotalState.RUNNING);
                    dmf = new DmfDetector().resolveDmf(pspDir, preferedMonVersion, preferedPerVersion, forcedMonVersion, forcedPerVersion);
                    //System.out.println(dmf);
                    Validator validator = Utils.buildValidator(main.getValidationDataManager(), dmf, pspDir);
                    //PrintStream out = textAreaPrintStream();//System.out;
                    out = buildTxtLogPrintstream();
                    //TODO: v produkci odstranit
                    Validator.DevParams devParams = null;
                    if (ConfigurationManager.DEV_MODE && ConfigurationManager.DEV_MODE_ONLY_SELECTED_SECTIONS) {
                        devParams = new Validator.DevParams();
                        //devParams.getSectionsToRun().add("Bibliografická metadata");
                        devParams.getSectionsToRun().add("Identifikátory");
                        devParams.getSectionsToRun().add("Soubor CHECKSUM");
                        devParams.getSectionsToRun().add("Soubor info");
                        devParams.getSectionsToRun().add("Struktura souborů");
                        devParams.getSectionsToRun().add("Primární METS filesec");
                        //devParams.getSectionsToRun().add("JPEG 2000");
                    }
                    //Thread.sleep(5000);
                    if (isCancelled()) {
                        return null;
                    }
                    validator.run(logXmlFile, out,
                            verbosity,
                            devParams,
                            MainController.this,
                            MainController.this);
                    //updateStatus(String.format("Validace balíku %s hotova.", pspDir.getAbsolutePath()));
                } /*catch (InterruptedException e) {
                    updateStatusFromWorkerThread(String.format("Validace balíku zrušena."), TotalState.STOPPED);
                } */ catch (Exception e) {
                    updateStatusFromWorkerThread(String.format("Chyba: %s.", e.getMessage()), TotalState.ERROR);
                    e.printStackTrace();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                    return null;
                }
            }

            private PrintStream buildTxtLogPrintstream() {
                if (logTxtFile == null) {
                    return null;
                } else {
                    try {
                        return new PrintStream(logTxtFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        };
        new Thread(validationTask).start();
    }

    private void updateStatusFromWorkerThread(String text, TotalState state) {
        Platform.runLater(() -> {
            updateStatus(text, state);
        });
    }

    private void updateStatus(String text, TotalState state) {
        statusText.setText(text);
        switch (state) {
            case IDLE:
                //menus
                menuValidate.setDisable(false);
                menuValidation.setDisable(true);
                menuSettings.setDisable(false);
                menuShow.setDisable(false);
                //status bar
                statusProgressIndicator.setVisible(false);
                statusImgFinished.setVisible(false);
                statusImgError.setVisible(false);
                statusImgStopped.setVisible(false);
                //logs through menu
                showValidationResultSummaryDialogItem.setDisable(true);
                showLogTxtMenuItem.setDisable(true);
                showLogXmlMenuItem.setDisable(true);
                break;
            case RUNNING:
                //menus
                menuValidate.setDisable(true);
                menuValidation.setDisable(false);
                menuSettings.setDisable(true);
                menuShow.setDisable(true);
                //status bar
                statusProgressIndicator.setVisible(true);
                statusImgFinished.setVisible(false);
                statusImgError.setVisible(false);
                statusImgStopped.setVisible(false);
                //logs through menu
                showValidationResultSummaryDialogItem.setDisable(true);
                showLogTxtMenuItem.setDisable(true);
                showLogXmlMenuItem.setDisable(true);
                break;
            case FINISHED:
                //menus
                menuValidate.setDisable(false);
                menuValidation.setDisable(true);
                menuSettings.setDisable(false);
                menuShow.setDisable(false);
                //status bar
                statusProgressIndicator.setVisible(false);
                statusImgFinished.setVisible(true);
                statusImgError.setVisible(false);
                statusImgStopped.setVisible(false);
                //logs through menu
                showValidationResultSummaryDialogItem.setDisable(false);
                showLogTxtMenuItem.setDisable(logTxtFile == null);
                showLogXmlMenuItem.setDisable(logXmlFile == null);
                break;
            case ERROR:
                //menus
                menuValidate.setDisable(false);
                menuValidation.setDisable(true);
                menuSettings.setDisable(false);
                menuShow.setDisable(false);
                //status bar
                statusProgressIndicator.setVisible(false);
                statusImgFinished.setVisible(false);
                statusImgError.setVisible(true);
                statusImgStopped.setVisible(false);
                //logs through menu
                showValidationResultSummaryDialogItem.setDisable(true);
                showLogTxtMenuItem.setDisable(true);
                showLogXmlMenuItem.setDisable(true);
                break;
            case STOPPED:
                //menus
                menuValidate.setDisable(false);
                menuValidation.setDisable(true);
                menuSettings.setDisable(false);
                menuShow.setDisable(false);
                //status bar
                statusProgressIndicator.setVisible(false);
                statusImgFinished.setVisible(false);
                statusImgError.setVisible(false);
                statusImgStopped.setVisible(true);
                //logs through menu
                showValidationResultSummaryDialogItem.setDisable(true);
                showLogTxtMenuItem.setDisable(true);
                showLogXmlMenuItem.setDisable(true);
                break;
        }
    }

    private String buildValidationLogName(File pspDir) {
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_");
        Date date = new Date();
        return dateFormat.format(date) + pspDir.getName();
    }

    private File getLogDir() {
        File logDir = main.getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_LOG_DIR);
        if (logDir != null) {
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            return logDir;
        } else {
            return null;
        }
    }

    private File buildXmlLogFile(File pspDir) {
        File logDir = getLogDir();
        if (logDir != null) {
            return new File(logDir, buildValidationLogName(pspDir) + ".xml");
        } else {
            return null;
        }
    }

    private File buildTxtLogFile(File pspDir) {
        File logDir = getLogDir();
        if (logDir != null) {
            return new File(logDir, buildValidationLogName(pspDir) + ".txt");
        } else {
            return null;
        }
    }


    @Override
    public void onValidationsFinish(int globalProblemsTotal, Map<Level, Integer> globalProblemsByLevel, boolean valid, long duration) {
        Platform.runLater(() -> {
            updateStatus(String.format("Validace balíku %s hotova.", pspDir.getAbsolutePath()), TotalState.FINISHED);
            //show dialog with summary
            validationResultSummary = new ValidationResultSummary();
            validationResultSummary.setPspDir(pspDir);
            validationResultSummary.setDmf(dmf);
            validationResultSummary.setGlobalProblemsTotal(globalProblemsTotal);
            validationResultSummary.setGlobalProblemsByLevel(globalProblemsByLevel);
            validationResultSummary.setValid(valid);
            validationResultSummary.setTotalTime(duration);
            main.showValidationResultSummaryDialog(validationResultSummary);
        });
    }

    @Override
    public void onValidationsCancel() {
        Platform.runLater(() -> {
            updateStatus(String.format("Validace balíku zrušena.", pspDir.getAbsolutePath()), TotalState.STOPPED);
        });
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
                    ListCell<ValidationProblem> cell = new ListCell<ValidationProblem>() {

                        @Override
                        protected void updateItem(ValidationProblem problem, boolean empty) {
                            super.updateItem(problem, empty);
                            if (empty || problem == null) {
                                setGraphic(null);
                            } else {
                                ProblemItem item = new ProblemItem();
                                item.populate(problem);
                                setGraphic(item.getContainer());
                                item.bindTextWidthToListWidth(list);
                            }
                        }
                    };
                    //http://stackoverflow.com/questions/29884894/how-to-make-listview-width-fit-width-of-its-cells#
                    //cell.prefWidthProperty().bind(list.widthProperty());
                    return cell;
                }


            });
        });
    }

    private void selectRule(RuleWithState rule) {
        if (rule != null && selectedSection != null) {
            selectedRule = rule;
            problemsContainer.setVisible(true);
            problemsSectionNameLbl.setText(selectedSection.getName() + ": ");
            problemsRuleNameLbl.setText(rule.getName());
            problemsRuleDescriptionLbl.setText(rule.getDescription());
            switch (rule.getState()) {
                case FINISHED:
                    problemsProgressIndicator.setVisible(false);
                    problemList.setVisible(true);
                    problemList.setItems(validationStateManager.getProblemsObservable(rule.getSectionId(), rule.getId()));
                    problemList.refresh();
                    break;
                case RUNNING:
                    problemsProgressIndicator.setVisible(true);
                    problemList.setVisible(false);
                    break;
                case WAITING:
                    problemsProgressIndicator.setVisible(false);
                    problemList.setVisible(false);
                    break;
            }
        } else { //rule je null
            selectedRule = null;
            //hide problems
            problemsContainer.setVisible(false);
            problemsSectionNameLbl.setText(null);
            problemsRuleNameLbl.setText(null);
            problemsRuleDescriptionLbl.setText(null);
            problemList.setVisible(false);
            problemsProgressIndicator.setVisible(false);
        }
    }


    private void selectSection(SectionWithState section) {
        if (section != null) {
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
        selectRule(null);
    }

    @Override
    public void onValidationsStart() {
        Platform.runLater(() -> {
            updateStatus(String.format("Validuji balík %s.", pspDir.getAbsolutePath()), TotalState.RUNNING);
        });
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
    public void onSectionCancel(int sectionId) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.CANCELED);
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
            //so that probles are reloaded
            selectRule(selectedRule);
        });
    }

    @Override
    public void onRuleCancel(int sectionId, int ruleId) {
        Platform.runLater(() -> {
            validationStateManager.updateRuleState(sectionId, ruleId, ProcessingState.CANCELED);
        });
    }

    @Override
    public boolean shouldCancel() {
        return cancelValidation;
    }

    public void stopValidation(ActionEvent actionEvent) {
        cancelValidation = true;
        if (validationTask != null) {
            validationTask.cancel();
        }
    }

    //DIALOGS

    public void showImageUtilsCheckDialog(ActionEvent actionEvent) {
        main.showImageUtilsCheckDialog(false, "OK");
    }

    public void showExclusionsConfigurationDialog(ActionEvent actionEvent) {
        main.showExclusionsConfigurationDialog();
    }

    public void showAboutAppDialog(ActionEvent actionEvent) {
        main.showAboutAppDialog();
    }

    public void showNewPspDirValidationConfigurationDialog(ActionEvent actionEvent) {
        main.showNewPspDirValidationConfigurationDialog();
    }

    public void showNewPspZipValidationConfigurationDialog(ActionEvent actionEvent) {
        main.showNewPspZipValidationConfigurationDialog();
    }

    public void showValidationResultSummaryDialog(ActionEvent actionEvent) {
        if (validationResultSummary != null) {
            main.showValidationResultSummaryDialog(validationResultSummary);
        }
    }

    public void showDictionariesConfigurationDialog(ActionEvent actionEvent) {
        main.showDictionariesConfigurationDialog();
    }

    //OPENING URLS

    public void showOnlineHelp(ActionEvent actionEvent) {
        if (ConfigurationManager.DEV_MODE) {
            main.showTestDialog();
        } else {
            openUrl(URL_ONLINE_HELP);
        }
    }

    public void showLogTxt(ActionEvent actionEvent) {
        openUrl("file:" + logTxtFile.getAbsolutePath());
    }

    public void showLogXml(ActionEvent actionEvent) {
        openUrl("file:" + logXmlFile.getAbsolutePath());
    }


    private enum TotalState {
        IDLE, RUNNING, FINISHED, ERROR, STOPPED;
    }
}
