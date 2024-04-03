package nkp.pspValidator.gui;

import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

/**
 * Created by Martin Řehánek on 2.12.16.
 */
public class ValidationDataManager {


    private static ValidationDataManager instance;
    private ExternalUtilManager externalUtilManager;
    private ConfigurationManager configurationManager;
    private FdmfRegistry fdmfRegistry;
    private ValidatorConfigurationManager validatorConfigMgr;

    public ValidationDataManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public ExternalUtilManager getExternalUtilManager() {
        return externalUtilManager;
    }

    public void setExternalUtilManager(ExternalUtilManager externalUtilManager) {
        this.externalUtilManager = externalUtilManager;
        externalUtilManager.setPath(ExternalUtil.JHOVE, configurationManager.getFileOrNull(ConfigurationManager.PROP_JHOVE_DIR));
        externalUtilManager.setPath(ExternalUtil.JPYLYZER, configurationManager.getFileOrNull(ConfigurationManager.PROP_JPYLYZER_DIR));
        externalUtilManager.setPath(ExternalUtil.IMAGE_MAGICK, configurationManager.getFileOrNull(ConfigurationManager.PROP_IMAGE_MAGICK_DIR));
        externalUtilManager.setPath(ExternalUtil.KAKADU, configurationManager.getFileOrNull(ConfigurationManager.PROP_KAKADU_DIR));
        externalUtilManager.setPath(ExternalUtil.MP3VAL, configurationManager.getFileOrNull(ConfigurationManager.PROP_MP3VAL_DIR));
        externalUtilManager.setPath(ExternalUtil.SHNTOOL, configurationManager.getFileOrNull(ConfigurationManager.PROP_SHNTOOL_DIR));
        externalUtilManager.setPath(ExternalUtil.CHECKMATE, configurationManager.getFileOrNull(ConfigurationManager.PROP_CHECKMATE_DIR));
    }

    public void setFdmfRegistry(FdmfRegistry fdmfRegistry) {
        this.fdmfRegistry = fdmfRegistry;
    }

    public FdmfRegistry getFdmfRegistry() {
        return fdmfRegistry;
    }


    public ValidatorConfigurationManager getValidatorConfigMgr() {
        return validatorConfigMgr;
    }

    public void setValidatorConfigMgr(ValidatorConfigurationManager validatorConfigMgr) {
        this.validatorConfigMgr = validatorConfigMgr;
    }
}
