package nkp.pspValidator.gui;

import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

/**
 * Created by martin on 2.12.16.
 */
public class ValidationDataManager {


    private static ValidationDataManager instance;
    private ImageUtilManager imageUtilManager;
    private ConfigurationManager configurationManager;
    private FdmfRegistry fdmfRegistry;

    public ValidationDataManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public ImageUtilManager getImageUtilManager() {
        return imageUtilManager;
    }

    public void setImageUtilManager(ImageUtilManager imageUtilManager) {
        this.imageUtilManager = imageUtilManager;
        imageUtilManager.setPath(ImageUtil.JHOVE, configurationManager.getFileOrNull(ConfigurationManager.PROP_JHOVE_DIR));
        imageUtilManager.setPath(ImageUtil.JPYLYZER, configurationManager.getFileOrNull(ConfigurationManager.PROP_JPYLYZER_DIR));
        imageUtilManager.setPath(ImageUtil.IMAGE_MAGICK, configurationManager.getFileOrNull(ConfigurationManager.PROP_IMAGE_MAGICK_DIR));
        imageUtilManager.setPath(ImageUtil.KAKADU, configurationManager.getFileOrNull(ConfigurationManager.PROP_KAKADU_DIR));
    }

    public void setFdmfRegistry(FdmfRegistry fdmfRegistry) {
        this.fdmfRegistry = fdmfRegistry;
    }

    public FdmfRegistry getFdmfRegistry() {
        return fdmfRegistry;
    }
}
