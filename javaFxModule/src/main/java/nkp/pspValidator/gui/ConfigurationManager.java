package nkp.pspValidator.gui;

import nkp.pspValidator.shared.Platform;

import java.io.*;
import java.util.Properties;

/**
 * Created by martin on 2.12.16.
 */
public class ConfigurationManager {


    private static File CONFIG_FILE = new File("../../resources/main/config.properties");

    //fdmf
    public static final String PROP_DMF_DIR = "fdmf.dir";

    //image tools
    public static final String PROP_JHOVE_DIR = "jhove.dir";
    public static final String PROP_JPYLYZER_DIR = "jpylyzer.dir";
    public static final String PROP_IMAGE_MAGICK_DIR = "imageMagick.dir";
    public static final String PROP_KAKADU_DIR = "kakadu.dir";

    //validation
    public static final String PROP_LAST_PSP_DIR = "last.psp.dir";
    public static final String PROP_FORCE_MON_VERSION = "force.monograph.version";
    public static final String PROP_FORCE_PER_VERSION = "force.periodical.version";

    private final Platform platform;
    private final File configFile;
    private final Properties properties = new Properties();

    public ConfigurationManager(Platform platform) throws IOException {
        this.platform = platform;
        this.configFile = CONFIG_FILE;
        loadProperties();
    }

    private void loadProperties() throws IOException {
        properties.load(new FileInputStream(configFile));
    }

    public File getFileOrNull(String propertyName) {
        String file = properties.getProperty(propertyName);
        return file == null ? null : new File(file);
    }

    public boolean getBooleanOrDefault(String propertyName, boolean defaultValue) {
        String stringVal = properties.getProperty(propertyName);
        if (stringVal == null) {
            return defaultValue;
        } else {
            return Boolean.valueOf(stringVal);
        }
    }

    public void setBoolean(String propertyName, Boolean value) {
        properties.setProperty(propertyName, value.toString());
        saveProperties();
    }


    public void setFile(String propertyName, File file) {
        try {
            properties.setProperty(propertyName, file.getCanonicalPath());
            saveProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveProperties() {
        try {
            OutputStream out = new FileOutputStream(configFile);
            properties.store(out, null);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("chyba při zápisu do souboru %s", configFile.getAbsolutePath()), e);
        }

    }

    public Platform getPlatform() {
        return platform;
    }


}
