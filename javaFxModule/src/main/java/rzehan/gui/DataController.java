package rzehan.gui;

import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by martin on 2.12.16.
 */
public class DataController {

    private static File CONFIG_FILE = new File("../../resources/main/config.properties");
    private static DataController instance;
    private final Config config;
    private ImageUtilManager imageUtilManager;
    private Platform platform;

    public DataController(Platform platform) throws IOException {
        this.platform = platform;
        config = new Config(CONFIG_FILE);
    }

    public ImageUtilManager getImageUtilManager() {
        return imageUtilManager;
    }

    public void setImageUtilManager(ImageUtilManager imageUtilManager) {
        this.imageUtilManager = imageUtilManager;
        imageUtilManager.setPath(ImageUtil.JHOVE, config.getFileOrNull(Config.PROP_JHOVE_DIR));
        imageUtilManager.setPath(ImageUtil.JPYLYZER, config.getFileOrNull(Config.PROP_JPYLYZER_DIR));
        imageUtilManager.setPath(ImageUtil.IMAGE_MAGICK, config.getFileOrNull(Config.PROP_IMAGE_MAGICK_DIR));
        imageUtilManager.setPath(ImageUtil.KAKADU, config.getFileOrNull(Config.PROP_KAKADU_DIR));
    }

    public Config getConfig() {
        return config;
    }

    public Platform getPlatform() {
        return platform;
    }
}
