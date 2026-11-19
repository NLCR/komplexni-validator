package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.newvalidation.OutputSettings;

import java.io.File;
import java.util.Objects;

/**
 * Záložka „Adresáře a protokoly“ v Nastavení: adresář pro protokoly a logy validace (klíč {@code validation.log_dir},
 * dřív se měnil v okně O aplikaci) a výchozí výstupy validace (stejné klíče jako volby Výstupy v dialogu Nová
 * validace, {@link OutputSettings}). Změny se drží v konceptu a uloží až {@link #save()}; zapisují se jen změněné
 * hodnoty.
 */
public class DirsDraft {

    private final ConfigurationManager config;
    private final OutputSettings outputs;
    private final File initialLogDir;
    private final boolean initialXml;
    private final boolean initialTxt;
    private final OutputSettings.Verbosity initialVerbosity;
    private File logDir;
    private boolean xml;
    private boolean txt;
    private OutputSettings.Verbosity verbosity;

    public DirsDraft(ConfigurationManager config) {
        this.config = config;
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

    /** Adresář konfigurace validátoru (fDMF, slovníky); jen ke zobrazení a otevření. */
    public File getValidatorConfigDir() {
        return config.getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
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
        return !Objects.equals(logDir, initialLogDir) || xml != initialXml || txt != initialTxt || verbosity != initialVerbosity;
    }

    public void save() {
        if (!Objects.equals(logDir, initialLogDir)) {
            config.setFile(ConfigurationManager.PROP_LOG_DIR, logDir);
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
