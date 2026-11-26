package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verze aplikace a datum buildu.
 * <p>
 * Hodnoty se ctou z resource {@value #RESOURCE}, ktery generuje Gradle pri buildu (task generateVersionProperties
 * v modules/sharedModule/build.gradle) z appVersion v gradle.properties. Jedine misto, kde se verze meni,
 * je tedy gradle.properties. Pri behu z IDE bez Gradle buildu resource chybi a pouziji se fallbacky
 * {@value #FALLBACK_VERSION_CODE} a {@value #FALLBACK_BUILD_DATE}.
 * <p>
 * Version code muze obsahovat jednu pomlcku oddelujici vlastni verzi od suffixu (dev, alpha, beta2, rc1; konvence v gradle/app-version.gradle).
 * Atribut validatorVersion v konfiguracnich souborech (fDMF, profily) se porovnava jen s major verzi,
 * viz {@link #checkConfigValidatorVersion(String, File)}.
 */
public class Version {

    private static final Logger LOG = Logger.getLogger(Version.class.getSimpleName());

    static final String RESOURCE = "/nkp/pspValidator/shared/version.properties";
    static final String FALLBACK_VERSION_CODE = "0.0.0-dev";
    static final String FALLBACK_BUILD_DATE = "neznámé";

    private static final Pattern MAJOR = Pattern.compile("^\\s*(\\d+)(\\.\\d+)*\\s*$");

    /**
     * Verze aplikace, napr. 3.0-alpha, 3.0, 3.0.1.
     */
    public static final String VERSION_CODE;
    /**
     * Datum buildu pro lidi, napr. "8. 11. 2026".
     */
    public static final String BUILD_DATE;
    /**
     * Datum buildu ve formatu ISO 8601 (2026-11-08), nebo null, kdyz neni znamo.
     */
    public static final String BUILD_DATE_ISO;

    static {
        Properties props = loadProperties();
        VERSION_CODE = nonEmptyOrDefault(props.getProperty("version"), FALLBACK_VERSION_CODE);
        BUILD_DATE = nonEmptyOrDefault(props.getProperty("buildDate"), FALLBACK_BUILD_DATE);
        BUILD_DATE_ISO = nonEmptyOrDefault(props.getProperty("buildDateIso"), null);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = Version.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOG.warning(String.format("chybí %s (běh bez Gradle buildu?), verze aplikace %s", RESOURCE, FALLBACK_VERSION_CODE));
            } else {
                props.load(in);
            }
        } catch (IOException e) {
            LOG.warning(String.format("nelze načíst %s: %s", RESOURCE, e.getMessage()));
        }
        return props;
    }

    private static String nonEmptyOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    /**
     * Major cast verze: vse pred pomlckou, z toho cislo pred prvni teckou (3.0-alpha -> 3, 3.0.1 -> 3, 3 -> 3).
     *
     * @return major verze, nebo null, kdyz verze neni ve tvaru cislo[.cislo...]
     */
    static Integer majorVersion(String version) {
        if (version == null) {
            return null;
        }
        String pure = version.split("-", 2)[0];
        Matcher m = MAJOR.matcher(pure);
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    /**
     * Zkontroluje atribut validatorVersion konfiguracniho souboru (fDMF, profil metadat, profil binarniho souboru)
     * proti verzi aplikace {@link #VERSION_CODE}.
     */
    public static void checkConfigValidatorVersion(String versionFromConfig, File configFile) throws ValidatorConfigurationException {
        checkConfigValidatorVersion(VERSION_CODE, versionFromConfig, configFile);
    }

    /**
     * Porovnava se jen major verze: aplikace 3.0-alpha, 3.0.1 nebo 3.2 nacte konfiguraci s validatorVersion "3.0"
     * i "3.1", ale ne "2.9". Atribut nese verzi aplikace platnou pri vzniku konfigurace a meni se hromadne az se
     * zmenou major verze. Pri behu bez version.properties (fallback {@value #FALLBACK_VERSION_CODE}) se kontrola
     * vypne s varovanim, jinak by z IDE neprosla zadna konfigurace.
     */
    static void checkConfigValidatorVersion(String appVersion, String versionFromConfig, File configFile) throws ValidatorConfigurationException {
        String path = configFile == null ? "?" : configFile.getAbsolutePath();
        if (FALLBACK_VERSION_CODE.equals(appVersion)) {
            LOG.fine(String.format("verze aplikace neznámá (%s), kontrola validatorVersion souboru %s vypnuta", appVersion, path));
            return;
        }
        Integer appMajor = majorVersion(appVersion);
        if (appMajor == null) {
            throw new IllegalStateException(String.format("neplatná verze aplikace '%s'", appVersion));
        }
        if (versionFromConfig == null || versionFromConfig.trim().isEmpty()) {
            throw new ValidatorConfigurationException("chybí atribut validatorVersion v souboru %s: očekávána verze s major %d (např. '%d.0'), protože validátor má verzi %s",
                    path, appMajor, appMajor, appVersion);
        }
        Integer configMajor = majorVersion(versionFromConfig);
        if (configMajor == null || !configMajor.equals(appMajor)) {
            throw new ValidatorConfigurationException("nesouhlasí verze validátoru: soubor %s má validatorVersion='%s', validátor ve verzi %s načte jen konfiguraci s major verzí %d (např. validatorVersion='%d.0')",
                    path, versionFromConfig, appVersion, appMajor, appMajor);
        }
    }
}
