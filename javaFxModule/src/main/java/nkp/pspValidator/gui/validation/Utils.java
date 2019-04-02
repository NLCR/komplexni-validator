package nkp.pspValidator.gui.validation;

import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;

import java.io.File;

/**
 * Created by Martin Řehánek on 10.4.18.
 */
public class Utils {

    public static Validator buildValidator(ValidationDataManager mgr, Dmf dmf, File pspDir) throws FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException {
        FdmfConfiguration fdmfConfig = mgr.getFdmfRegistry().getFdmfConfig(dmf);
        fdmfConfig.initBinaryFileProfiles(mgr.getExternalUtilManager());
        DictionaryManager dictionaryManager = mgr.getValidatorConfigMgr().getDictionaryManager();
        return ValidatorFactory.buildValidator(fdmfConfig, pspDir, dictionaryManager);
    }
}
