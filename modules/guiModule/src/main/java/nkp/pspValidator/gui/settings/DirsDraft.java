package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.newvalidation.OutputSettings;

import java.io.File;
import java.util.Objects;

/**
 * Záložka „Adresáře a protokoly“ v Nastavení: adresář pro protokoly a logy validace (klíč {@code validation.log_dir},
 * dřív se měnil v okně O aplikaci), adresář konfigurace validátoru (klíč {@code validatorConfig.dir}) a výchozí výstupy
 * validace (stejné klíče jako volby Výstupy v dialogu Nová validace, {@link OutputSettings}). Změny se drží v konceptu
 * a uloží až {@link #save()}; zapisují se jen změněné hodnoty.
 * <p>
 * Adresář konfigurace validátoru běžící aplikace se nemění: nový se použije až po restartu aplikace. Koncept proto
 * rozlišuje adresář zobrazený na kartě (koncept), uložený v nastavení a ten, který běžící aplikace používá
 * ({@link #getValidatorConfigState()}). Platnost nového adresáře ověřuje volající předem ({@link ValidatorConfigCheck}).
 */
public class DirsDraft {

    private final ConfigurationManager config;
    private final OutputSettings outputs;
    private final File initialLogDir;
    private final File savedValidatorConfigDir;
    /** adresář, jehož konfiguraci běžící aplikace načetla při startu */
    private final File runningValidatorConfigDir;
    private final boolean initialXml;
    private final boolean initialTxt;
    private final OutputSettings.Verbosity initialVerbosity;
    private File logDir;
    private File validatorConfigDir;
    private boolean xml;
    private boolean txt;
    private OutputSettings.Verbosity verbosity;

    /** Koncept, když běžící aplikace používá uložený adresář konfigurace validátoru. */
    public DirsDraft(ConfigurationManager config) {
        this(config, null);
    }

    /**
     * @param runningValidatorConfigDir adresář konfigurace validátoru, který běžící aplikace načetla při startu;
     *                                  null = uložený v nastavení
     */
    public DirsDraft(ConfigurationManager config, File runningValidatorConfigDir) {
        this.config = config;
        savedValidatorConfigDir = config.getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
        this.runningValidatorConfigDir = runningValidatorConfigDir != null ? runningValidatorConfigDir : savedValidatorConfigDir;
        validatorConfigDir = savedValidatorConfigDir;
        this.outputs = new OutputSettings(config);
        initialLogDir = config.getFileOrNull(ConfigurationManager.PROP_LOG_DIR);
        initialXml = outputs.isCreateXmlLog();
        initialTxt = outputs.isCreateTxtLog();
        initialVerbosity = outputs.getVerbosity();
        logDir = initialLogDir;
        xml = initialXml;
        txt = initialTxt;
        verbosity = initialVerbosity;
    }

    /** Adresář konfigurace validátoru (fDMF, slovníky) v konceptu: zobrazený na kartě, po uložení v nastavení. */
    public File getValidatorConfigDir() {
        return validatorConfigDir;
    }

    /** Adresář konfigurace validátoru, který běžící aplikace používá (do restartu). */
    public File getRunningValidatorConfigDir() {
        return runningValidatorConfigDir;
    }

    /** Nový adresář konfigurace validátoru (už ověřený, {@link ValidatorConfigCheck}); null se ignoruje. */
    public void setValidatorConfigDir(File dir) {
        if (dir != null) {
            this.validatorConfigDir = dir;
        }
    }

    /** Stav adresáře konfigurace validátoru vůči uloženému a používanému (poznámka na kartě). */
    public enum ValidatorConfigState {
        /** zobrazený adresář je uložený a aplikace ho používá */
        IN_USE,
        /** neuložená změna, použije se po uložení a restartu aplikace */
        UNSAVED,
        /** uložený, ale aplikace do restartu používá jiný */
        SAVED_PENDING_RESTART,
        /** neuložený návrat k adresáři, který aplikace používá (uložený je jiný) */
        UNSAVED_BACK_TO_RUNNING
    }

    public ValidatorConfigState getValidatorConfigState() {
        boolean unsaved = isValidatorConfigDirChanged();
        boolean running = ConfigurationManager.sameFile(validatorConfigDir, runningValidatorConfigDir);
        if (unsaved) {
            return running ? ValidatorConfigState.UNSAVED_BACK_TO_RUNNING : ValidatorConfigState.UNSAVED;
        }
        return running ? ValidatorConfigState.IN_USE : ValidatorConfigState.SAVED_PENDING_RESTART;
    }

    public boolean isValidatorConfigDirChanged() {
        return !ConfigurationManager.sameFile(validatorConfigDir, savedValidatorConfigDir);
    }

    /**
     * Po uložení konceptu bude uložený jiný adresář konfigurace validátoru, než jaký aplikace používá, a ten se uložením
     * nově mění (nabídnout restart). Volat před {@link #save()}.
     */
    public boolean isRestartNeededAfterSave() {
        return isValidatorConfigDirChanged() && !ConfigurationManager.sameFile(validatorConfigDir, runningValidatorConfigDir);
    }

    public File getLogDir() {
        return logDir;
    }

    public void setLogDir(File logDir) {
        if (logDir != null) {
            this.logDir = logDir;
        }
    }

    public boolean isCreateXmlLog() {
        return xml;
    }

    public void setCreateXmlLog(boolean xml) {
        this.xml = xml;
    }

    public boolean isCreateTxtLog() {
        return txt;
    }

    public void setCreateTxtLog(boolean txt) {
        this.txt = txt;
    }

    public OutputSettings.Verbosity getVerbosity() {
        return verbosity;
    }

    public void setVerbosity(OutputSettings.Verbosity verbosity) {
        if (verbosity != null) {
            this.verbosity = verbosity;
        }
    }

    public boolean isChanged() {
        return !Objects.equals(logDir, initialLogDir) || isValidatorConfigDirChanged() || xml != initialXml || txt != initialTxt || verbosity != initialVerbosity;
    }

    public void save() {
        if (!Objects.equals(logDir, initialLogDir)) {
            config.setFile(ConfigurationManager.PROP_LOG_DIR, logDir);
        }
        if (isValidatorConfigDirChanged()) {
            config.setValidatorConfigDirChosenByUser(validatorConfigDir);
        }
        if (xml != initialXml) {
            outputs.setCreateXmlLog(xml);
        }
        if (txt != initialTxt) {
            outputs.setCreateTxtLog(txt);
        }
        if (verbosity != initialVerbosity) {
            outputs.setVerbosity(verbosity);
        }
    }
}
