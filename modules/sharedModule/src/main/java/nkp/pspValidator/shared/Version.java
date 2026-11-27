package nkp.pspValidator.shared;

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
 * Verzi konfigurace kontroluje {@link ConfigManifest} (major verze z manifest.properties).
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
}
