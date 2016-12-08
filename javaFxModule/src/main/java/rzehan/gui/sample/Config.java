package rzehan.gui.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by martin on 2.12.16.
 */
public class Config {

    private static final String PROP_DMF_DIR = "fdmf.dir";

    private final File configFile;
    private final Properties properties = new Properties();

    public Config(File configFile) throws FileNotFoundException, IOException {
        this.configFile = configFile;
        loadProperties();
    }

    private void loadProperties() throws FileNotFoundException, IOException {
        properties.load(new FileInputStream(configFile));
    }

    public File getFdmfDir() {
        return getFile(PROP_DMF_DIR);
    }

    private File getFile(String property) {
        String file = properties.getProperty(property);
        return new File(file);
    }


}
