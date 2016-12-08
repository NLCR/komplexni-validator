package rzehan.gui;

import java.io.*;
import java.util.Properties;

/**
 * Created by martin on 2.12.16.
 */
public class Config {

    private static final String PROP_DMF_DIR = "fdmf.dir";

    public static final String PROP_JHOVE_DIR = "jhove.dir";
    public static final String PROP_JPYLYZER_DIR = "jpylyzer.dir";
    public static final String PROP_IMAGE_MAGICK_DIR = "imageMagick.dir";
    public static final String PROP_KAKADU_DIR = "kakadu.dir";


    private final File configFile;
    private final Properties properties = new Properties();

    public Config(File configFile) throws IOException {
        this.configFile = configFile;
        loadProperties();
    }

    private void loadProperties() throws IOException {
        properties.load(new FileInputStream(configFile));
    }

    public File getFileOrNull(String propertyName) {
        String file = properties.getProperty(propertyName);
        return file == null ? null : new File(file);
    }

    public void setFile(String propertyName, File file) {
        try {
            properties.setProperty(propertyName, file.getCanonicalPath());
            saveProperties();
        } catch (IOException e) {
            throw new RuntimeException(String.format("chyba ukládání souboru %s", file.getAbsolutePath()), e);
        }
    }

    private void saveProperties() throws IOException {
        OutputStream out = new FileOutputStream(configFile);
        properties.store(out, null);
        out.close();
    }

}
