package nkp.pspValidator.gui;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;

import java.io.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by Martin Řehánek on 2.12.16.
 */
public class ConfigurationManager {

    public static boolean DEV_MODE = false;
    public static boolean DEV_MODE_ONLY_SELECTED_SECTIONS = false;

    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String VALIDATOR_CONFIG_DIR_NAME = "validatorConfig";
    private static final String USER_DATA_DIR_NAME = "KomplexniValidator";

    private static File CONFIG_FILE_PRODUCTION = new File("config.properties");
    private static File CONFIG_FILE_DEV_WIN = new File("../../resources/main/dev/config-win.properties");
    private static File CONFIG_FILE_DEV_MAC = new File("../../resources/main/dev/config-mac.properties");
    private static File CONFIG_FILE_DEV_LINUX = new File("../../resources/main/dev/config-linux.properties");

    //fdmf
    public static final String PROP_VALIDATOR_CONFIG_DIR = "validatorConfig.dir";

    //external tools
    public static final String PROP_EXTERNAL_TOOLS_CHECK_SHOWN = "external_tools_check.shown";
    public static final String PROP_JHOVE_DIR = "jhove.dir";
    public static final String PROP_JPYLYZER_DIR = "jpylyzer.dir";
    public static final String PROP_IMAGE_MAGICK_DIR = "imageMagick.dir";
    public static final String PROP_KAKADU_DIR = "kakadu.dir";
    public static final String PROP_MP3VAL_DIR = "mp3val.dir";
    public static final String PROP_SHNTOOL_DIR = "shntool.dir";
    public static final String PROP_CHECKMATE_DIR = "checkmate.dir";

    //validation
    public static final String PROP_LAST_PSP_DIR = "last.psp.dir";
    public static final String PROP_LAST_PSP_ZIP = "last.psp.zip";
    public static final String PROP_FORCE_MON_VERSION_ENABLED = "force.monograph.version.enabled";
    public static final String PROP_FORCE_MON_VERSION_CODE = "force.monograph.version.code";
    public static final String PROP_FORCE_PER_VERSION_ENABLED = "force.periodical.version.enabled";
    public static final String PROP_FORCE_PER_VERSION_CODE = "force.periodical.version.code";
    public static final String PROP_FORCE_AUDIO_GRAM_VERSION_ENABLED = "force.audio_gram.version.enabled";
    public static final String PROP_FORCE_AUDIO_GRAM_VERSION_CODE = "force.audio_gram.version.code";
    public static final String PROP_FORCE_AUDIO_FONO_VERSION_ENABLED = "force.audio_fono.version.enabled";
    public static final String PROP_FORCE_AUDIO_FONO_VERSION_CODE = "force.audio_fono.version.code";
    public static final String PROP_FORCE_AUDIO_DISC_VERSION_ENABLED = "force.audio_disc.version.enabled";
    public static final String PROP_FORCE_AUDIO_DISC_VERSION_CODE = "force.audio_disc.version.code";
    public static final String PROP_FORCE_AUDIO_NO_CARRIER_VERSION_ENABLED = "force.audio_no_carrier.version.enabled";
    public static final String PROP_FORCE_AUDIO_NO_CARRIER_VERSION_CODE = "force.audio_no_carrier.version.code";
    public static final String PROP_FORCE_DIGITAL_DISC_VERSION_ENABLED = "force.digital_disc.version.enabled";
    public static final String PROP_FORCE_DIGITAL_DISC_VERSION_CODE = "force.digital_disc.version.code";
    public static final String PROP_FORCE_FUND_UNIT_VERSION_ENABLED = "force.fund_unit.version.enabled";
    public static final String PROP_FORCE_FUND_UNIT_VERSION_CODE = "force.fund_unit.version.code";
    public static final String PROP_PREFER_MON_VERSION_ENABLED = "prefer.monograph.version.enabled";
    public static final String PROP_PREFER_MON_VERSION_CODE = "prefer.monograph.version.code";
    public static final String PROP_PREFER_PER_VERSION_ENABLED = "prefer.periodical.version.enabled";
    public static final String PROP_PREFER_PER_VERSION_CODE = "prefer.periodical.version.code";
    public static final String PROP_PREFER_AUDIO_GRAM_VERSION_ENABLED = "prefer.audio_gram.version.enabled";
    public static final String PROP_PREFER_AUDIO_GRAM_VERSION_CODE = "prefer.audio_gram.version.code";
    public static final String PROP_PREFER_AUDIO_FONO_VERSION_ENABLED = "prefer.audio_fono.version.enabled";
    public static final String PROP_PREFER_AUDIO_FONO_VERSION_CODE = "prefer.audio_fono.version.code";
    public static final String PROP_PREFER_AUDIO_DISC_VERSION_ENABLED = "prefer.audio_disc.version.enabled";
    public static final String PROP_PREFER_AUDIO_DISC_VERSION_CODE = "prefer.audio_disc.version.code";
    public static final String PROP_PREFER_AUDIO_NO_CARRIER_VERSION_ENABLED = "prefer.audio_no_carrier.version.enabled";
    public static final String PROP_PREFER_AUDIO_NO_CARRIER_VERSION_CODE = "prefer.audio_no_carrier.version.code";
    public static final String PROP_PREFER_DIGITAL_DISC_VERSION_ENABLED = "prefer.digital_disc.version.enabled";
    public static final String PROP_PREFER_DIGITAL_DISC_VERSION_CODE = "prefer.digital_disc.version.code";
    public static final String PROP_PREFER_FUND_UNIT_VERSION_ENABLED = "prefer.fund_unit.version.enabled";
    public static final String PROP_PREFER_FUND_UNIT_VERSION_CODE = "prefer.fund_unit.version.code";
    public static final String PROP_PSP_VALIDATION_CREATE_TXT_LOG = "psp_validation.create_txt_log";
    public static final String PROP_PSP_VALIDATION_CREATE_XML_LOG = "psp_validation.create_xml_log";
    public static final String PROP_LOG_DIR = "validation.log_dir";

    //dictionaries
    public static final String propDictionarySpecUrl(String dictionary) {
        return "dictionary." + dictionary + ".specUrl";
    }

    public static final String propDictionaryDescription(String dictionary) {
        return "dictionary." + dictionary + ".description";
    }

    public static final String propDictionarySyncDate(String dictionary) {
        return "dictionary." + dictionary + ".syncDate";
    }

    public static final String propDictionarySyncUrl(String dictionary) {
        return "dictionary." + dictionary + ".syncUrl";
    }

    //skipped sections
    public static final String propSkippedValidationSections(Dmf dmf) {
        String dmfCode = dmf.getType().name() + "-" + dmf.getVersion();
        return "skipped.validation.sections." + dmfCode;
    }

    //text log verbosity
    public static final String PROP_TEXT_LOG_VERBOSITY = "text_log.verbosity";

    private final Platform platform;
    private final Locations locations;
    private final File configFile;
    private final Properties properties = new Properties();

    public ConfigurationManager(Platform platform) throws IOException {
        this(platform, Locations.detect(platform));
    }

    ConfigurationManager(Platform platform, Locations locations) throws IOException {
        try {
            this.platform = platform;
            this.locations = locations;
            this.configFile = selectConfigFile();
            loadProperties();
            initDefaultProperties();
        } catch (IOException e) {
            throw new IOException(new File(".").getAbsolutePath(), e);
        }
    }

    private void initDefaultProperties() {
        //validator config dir
        File validatorConfigDir = getFileOrNull(PROP_VALIDATOR_CONFIG_DIR);
        // konfigurace z instalace se bere vždy z právě spuštěné aplikace (jinak by nová verze nebo přesunutá aplikace
        // použila pravidla ze staré instalace); vlastní existující adresář mimo instalaci zvolený uživatelem zůstává
        if (validatorConfigDir != null && !DEV_MODE && locations.isStaleValidatorConfigDir(validatorConfigDir)) {
            validatorConfigDir = null;
        }
        if (validatorConfigDir == null) {
            validatorConfigDir = locations.defaultValidatorConfigDir;
            setFile(PROP_VALIDATOR_CONFIG_DIR, validatorConfigDir);
        }
        //log dir
        File logDir = getFileOrNull(PROP_LOG_DIR);
        if (logDir == null) {
            logDir = locations.defaultLogDir;
            setFile(PROP_LOG_DIR, logDir);
        }
        logDir.mkdirs();
        //dictionaries
        initDictionary("siglaInstitutionCodes",
                "kódy institucí (Sigla)",
                null,
                "https://raw.githubusercontent.com/NLCR/komplexni-validator/master/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/dictionaries/siglaInstitutionCodes.dict");
        initDictionary("iso31661Alpha2languageCodes",
                "kódy zemí ISO3166-1 alpha-2",
                "https://www.iso.org/iso-3166-country-codes.html",
                null);
        initDictionary("iso6392languageCodes",
                "kódy jazyků podle ISO 639-2",
                "http://www.loc.gov/standards/iso639-2/php/code_list.php",
                null);
        initDictionary("marcRelatorCodes",
                "kódy rolí podle MARC",
                "https://www.loc.gov/marc/relators/relaterm.html",
                null);
    }

    private void initDictionary(String name, String description, String specUrl, String syncUrl) {
        updateStringPropertyIfOldValueEmptyAndNewValueNot(propDictionaryDescription(name), description);
        updateStringPropertyIfOldValueEmptyAndNewValueNot(propDictionarySpecUrl(name), specUrl);
        updateStringPropertyIfOldValueEmptyAndNewValueNot(propDictionarySyncUrl(name), syncUrl);
    }

    private void updateStringPropertyIfOldValueEmptyAndNewValueNot(String key, String newValue) {
        String oldValue = getStringOrDefault(key, null);
        if (oldValue == null || oldValue.isEmpty()) {
            if (newValue != null && !newValue.isEmpty()) {
                setString(key, newValue);
            }
        }
    }

    private File selectConfigFile() {
        if (DEV_MODE) {
            OperatingSystem os = platform.getOperatingSystem();
            if (os == null) {
                return CONFIG_FILE_PRODUCTION;
            }
            switch (os) {
                case LINUX:
                    return CONFIG_FILE_DEV_LINUX;
                case WINDOWS:
                    return CONFIG_FILE_DEV_WIN;
                case MAC:
                    return CONFIG_FILE_DEV_MAC;
                default:
                    return CONFIG_FILE_PRODUCTION;
            }
        }
        if (locations.userDataDir != null) {
            locations.userDataDir.mkdirs();
            File configFile = new File(locations.userDataDir, CONFIG_FILE_PRODUCTION.getName());
            if (!configFile.exists()) {
                // jednorázová migrace nastavení z verze 2.8 (cesty k nástrojům, přeskočené sekce, ...), která
                // config.properties ukládala do pracovního adresáře; zastaralá cesta ke konfiguraci se opraví níže
                File legacy = locations.findLegacyConfigFile(CONFIG_FILE_PRODUCTION.getName());
                if (legacy != null) {
                    try {
                        java.nio.file.Files.copy(legacy.toPath(), configFile.toPath());
                    } catch (IOException e) {
                        // bez migrace, aplikace začne s výchozím nastavením
                    }
                }
            }
            return configFile;
        }
        return CONFIG_FILE_PRODUCTION;
    }

    /**
     * Umístění souborů aplikace podle způsobu spuštění. Jen výpočet cest, nic nezapisuje (testovatelné bez JavaFX).
     * <p>
     * Aplikace z jpackage (instalátor, portable, macOS .app; launcher nastavuje systémovou vlastnost
     * {@code jpackage.app-path}): konfigurace validace je vedle hlavního jaru ({@code app/validatorConfig} na Windows,
     * {@code lib/app/validatorConfig} na Linuxu, {@code Contents/app/validatorConfig} na macOS), vždy absolutně, protože
     * pracovní adresář je libovolný (z Finderu "/", ze zástupce ve Windows různě). Zapisovatelné soubory
     * ({@code config.properties}, výchozí adresář logů) jsou mimo instalaci, ta nemusí být zapisovatelná a při
     * odinstalaci nebo aktualizaci se maže: Windows {@code %APPDATA%\KomplexniValidator}, Linux
     * {@code $XDG_CONFIG_HOME/KomplexniValidator} (výchozí {@code ~/.config}), macOS
     * {@code ~/Library/Application Support/KomplexniValidator}.
     * <p>
     * Mimo jpackage (java -jar z GUI zipu, IDE) a na neznámém OS: {@code validatorConfig} vedle jaru, pokud existuje,
     * jinak {@code validatorConfig} v pracovním adresáři; {@code config.properties} a {@code logs} v pracovním adresáři
     * (chování verze 2.8).
     */
    static final class Locations {
        /** spuštěno z aplikace vytvořené přes jpackage na známém OS */
        final boolean jpackage;
        /** adresář hlavního jaru, null pokud ho nelze zjistit */
        final File appJarDir;
        final File defaultValidatorConfigDir;
        final File defaultLogDir;
        /** adresář pro config.properties, null = pracovní adresář */
        final File userDataDir;
        /** pracovní adresář (kvůli migraci config.properties z verze 2.8; v testech podstrčený) */
        final File workingDir;

        private Locations(boolean jpackage, File appJarDir, File defaultValidatorConfigDir, File defaultLogDir, File userDataDir, File workingDir) {
            this.jpackage = jpackage;
            this.appJarDir = appJarDir;
            this.defaultValidatorConfigDir = defaultValidatorConfigDir;
            this.defaultLogDir = defaultLogDir;
            this.userDataDir = userDataDir;
            this.workingDir = workingDir;
        }

        static Locations detect(Platform platform) {
            return resolve(platform == null ? null : platform.getOperatingSystem(),
                    System.getProperty("jpackage.app-path") != null,
                    getAppJarDirOrNull(),
                    new File(System.getProperty("user.home")),
                    System.getenv("APPDATA"),
                    System.getenv("XDG_CONFIG_HOME"),
                    new File("."));
        }

        /**
         * @param os            null = neznámý OS, chová se jako mimo jpackage
         * @param jpackage      je nastavena systémová vlastnost jpackage.app-path
         * @param appJarDir     adresář hlavního jaru, null pokud ho nelze zjistit (pak jako mimo jpackage)
         * @param userHome      domovský adresář uživatele
         * @param appData       proměnná prostředí APPDATA (Windows), může být null
         * @param xdgConfigHome proměnná prostředí XDG_CONFIG_HOME (Linux), může být null
         * @param workingDir    pracovní adresář (jen pro vyhledání config.properties z verze 2.8)
         */
        static Locations resolve(OperatingSystem os, boolean jpackage, File appJarDir, File userHome,
                                 String appData, String xdgConfigHome, File workingDir) {
            if (jpackage && os != null && appJarDir != null) {
                File userDataDir = getUserDataDir(os, userHome, appData, xdgConfigHome).getAbsoluteFile();
                return new Locations(true, appJarDir.getAbsoluteFile(),
                        new File(appJarDir, VALIDATOR_CONFIG_DIR_NAME).getAbsoluteFile(),
                        new File(userDataDir, DEFAULT_LOG_DIR),
                        userDataDir, workingDir);
            }
            File validatorConfigDir = new File(VALIDATOR_CONFIG_DIR_NAME);
            if (appJarDir != null) {
                File besideJar = new File(appJarDir, VALIDATOR_CONFIG_DIR_NAME);
                if (besideJar.isDirectory()) {
                    validatorConfigDir = besideJar.getAbsoluteFile();
                }
            }
            return new Locations(false, appJarDir, validatorConfigDir, new File(DEFAULT_LOG_DIR), null, workingDir);
        }

        private static File getUserDataDir(OperatingSystem os, File userHome, String appData, String xdgConfigHome) {
            switch (os) {
                case WINDOWS:
                    if (appData != null && !appData.isEmpty()) {
                        return new File(appData, USER_DATA_DIR_NAME);
                    }
                    return new File(userHome, "AppData/Roaming/" + USER_DATA_DIR_NAME);
                case LINUX:
                    // podle XDG Base Directory musí být XDG_CONFIG_HOME absolutní, jinak se ignoruje
                    if (xdgConfigHome != null && !xdgConfigHome.isEmpty() && new File(xdgConfigHome).isAbsolute()) {
                        return new File(xdgConfigHome, USER_DATA_DIR_NAME);
                    }
                    return new File(userHome, ".config/" + USER_DATA_DIR_NAME);
                case MAC:
                default:
                    return new File(userHome, "Library/Application Support/" + USER_DATA_DIR_NAME);
            }
        }

        /**
         * Uložená cesta ke konfiguraci validace se nahradí výchozí, pokud neexistuje (přesun portable, reinstalace
         * jinam) nebo leží uvnitř instalace z jpackage (této nebo jiné verze, i macOS .app).
         */
        boolean isStaleValidatorConfigDir(File dir) {
            if (!dir.isDirectory()) {
                return true;
            }
            if (!jpackage) {
                // java -jar: existující adresář zvolený uživatelem se nikdy nepřepisuje
                return false;
            }
            // adresář aplikace: rodič adresáře jaru (Windows <instalace>, Linux <instalace>/lib, macOS Contents)
            if (appJarDir.getParentFile() != null) {
                Path appDir = appJarDir.getParentFile().toPath().toAbsolutePath().normalize();
                if (dir.toPath().toAbsolutePath().normalize().startsWith(appDir)) {
                    return true;
                }
            }
            // jiná (starší, přesunutá) macOS .app této aplikace; na Windows a Linuxu starou instalaci odstraní
            // aktualizace a přesunutý portable adresář zachytí kontrola existence výše
            String path = dir.getAbsolutePath().replace('\\', '/');
            return path.contains("/KomplexniValidator.app/Contents/app/");
        }

        /**
         * Soubor config.properties z verze 2.8 a starších (v pracovním adresáři, u instalace ve Windows typicky
         * v adresáři aplikace); použije se jednou pro migraci nastavení do adresáře uživatele.
         */
        File findLegacyConfigFile(String fileName) {
            File inWorkingDir = new File(workingDir, fileName);
            if (inWorkingDir.isFile()) {
                return inWorkingDir;
            }
            if (appJarDir != null && appJarDir.getParentFile() != null) {
                File inAppDir = new File(appJarDir.getParentFile(), fileName);
                if (inAppDir.isFile()) {
                    return inAppDir;
                }
            }
            return null;
        }
    }

    private static File getAppJarDirOrNull() {
        try {
            return new File(ConfigurationManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadProperties() throws IOException {
        if (configFile.exists()) {
            properties.load(new InputStreamReader(new FileInputStream(configFile), "UTF-8"));
        }
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

    public Integer getIntegerOrNull(String propertyName) {
        String stringVal = properties.getProperty(propertyName);
        if (stringVal == null) {
            return null;
        } else {
            return Integer.valueOf(stringVal);
        }
    }

    public String getStringOrDefault(String propertyName, String defaultValue) {
        String stringVal = properties.getProperty(propertyName);
        if (stringVal == null) {
            return defaultValue;
        } else {
            return stringVal;
        }
    }

    /**
     * @param propertyName
     * @return never null, if value is not found an empty Set<String> is returned
     */
    public Set<String> getStringSet(String propertyName) {
        String stringVal = properties.getProperty(propertyName);
        if (stringVal == null) {
            return Collections.emptySet();
        } else {
            String[] items = stringVal.split(",");
            HashSet<String> result = new HashSet<>(items.length);
            for (String item : items) {
                result.add(item);
            }
            return result;
        }
    }

    public void setInteger(String propertyName, Integer value) {
        if (value != null) {
            properties.setProperty(propertyName, value.toString());
            saveProperties();
        }
    }

    public void setBoolean(String propertyName, Boolean value) {
        if (value != null) {
            properties.setProperty(propertyName, value.toString());
            saveProperties();
        }
    }

    public void setString(String propertyName, String value) {
        if (value != null) {
            properties.setProperty(propertyName, value);
            saveProperties();
        }
    }

    public void setStringSet(String propertyName, Set<String> set) {
        if (set != null) {
            StringBuilder builder = new StringBuilder();
            int counter = 0;
            for (String item : set) {
                builder.append(item);
                if (counter != set.size() - 1) {
                    builder.append(',');
                }
                counter++;
            }
            String value = builder.toString();
            properties.setProperty(propertyName, value);
            saveProperties();
        }
    }

    public void setFile(String propertyName, File file) {
        try {
            if (file == null) {
                properties.remove(propertyName);
            } else {
                properties.setProperty(propertyName, file.getCanonicalPath());
            }
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
