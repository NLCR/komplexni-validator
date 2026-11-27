package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manifest adresáře konfigurace validátoru: soubor {@value #FILE_NAME} s klíčem {@value #KEY_CONFIG_VERSION}.
 * <p>
 * Hodnota je MAJOR.MINOR verze validátoru, se kterou byla konfigurace vydána (bez PATCH a bez přípony předvydání,
 * např. {@code 3.0}). Každá konfigurace 3.x manifest má. Konfigurace 2.x a starší ho nemá, chybějící manifest tedy
 * znamená „starší než 3.0, nebo to není konfigurace validátoru“.
 * <p>
 * Validátor načte jen konfiguraci se stejnou major verzí. Rozdílná minor verze není chyba: starší konfigurace
 * (3.0 pro validátor 3.1) postrádá novější typy dokumentů a verze DMF, novější konfigurace (3.1 pro validátor 3.0)
 * může obsahovat části, které validátor neumí načíst. Obě situace se jen vyhodnotí ({@link Status#OLDER_MINOR},
 * {@link Status#NEWER_MINOR}) a upozornění ukáže GUI.
 * <p>
 * Soubor se čte jako UTF-8, hodnota {@code configVersion} se ořezává o okolní mezery, jinak musí mít přesně tvar
 * MAJOR.MINOR (dvě čísla oddělená tečkou; {@code 3}, {@code 3.0.1} ani {@code 3.0-beta} neplatí).
 * Verze aplikace se bere z {@link Version#VERSION_CODE}; při záložní verzi (běh bez version.properties) se kontrola
 * vypne stejně jako u {@link Version#checkConfigValidatorVersion(String, File)}.
 */
public final class ConfigManifest {

    private static final Logger LOG = Logger.getLogger(ConfigManifest.class.getSimpleName());

    public static final String FILE_NAME = "manifest.properties";
    public static final String KEY_CONFIG_VERSION = "configVersion";

    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d{1,9})\\.(\\d{1,9})$");
    private static final Pattern APP_VERSION = Pattern.compile("^(\\d{1,9})(?:\\.(\\d{1,9}))?(?:\\.\\d+)*$");

    private ConfigManifest() {
    }

    public enum Status {
        /** stejný major i minor (kontrola vypnutá při záložní verzi aplikace) */
        OK,
        /** stejný major, konfigurace má nižší minor než aplikace (není chyba) */
        OLDER_MINOR,
        /** stejný major, konfigurace má vyšší minor než aplikace (není chyba) */
        NEWER_MINOR,
        DIFFERENT_MAJOR,
        /** soubor manifest.properties v adresáři není */
        MISSING,
        /** soubor nejde přečíst, chybí klíč, nebo hodnota není MAJOR.MINOR */
        INVALID;

        /** Stav, ve kterém validátor konfiguraci nenačte. */
        public boolean isError() {
            return this == DIFFERENT_MAJOR || this == MISSING || this == INVALID;
        }
    }

    /** Výsledek vyhodnocení manifestu. */
    public static final class Result {
        private final Status status;
        private final String configVersion;
        private final String message;

        private Result(Status status, String configVersion, String message) {
            this.status = status;
            this.configVersion = configVersion;
            this.message = message;
        }

        public Status getStatus() {
            return status;
        }

        /** Verze konfigurace z manifestu (MAJOR.MINOR), nebo null, když ji nelze zjistit (MISSING, INVALID, kontrola vypnutá). */
        public String getConfigVersion() {
            return configVersion;
        }

        /** Hláška pro uživatele (česky, s cestou k adresáři); null ve stavu OK. */
        public String getMessage() {
            return message;
        }
    }

    /** Vyhodnotí manifest adresáře konfigurace proti verzi aplikace {@link Version#VERSION_CODE}. */
    public static Result evaluate(File configDir) {
        return evaluate(Version.VERSION_CODE, configDir);
    }

    static Result evaluate(String appVersion, File configDir) {
        String dirPath = configDir.getAbsolutePath();
        if (Version.FALLBACK_VERSION_CODE.equals(appVersion)) {
            LOG.fine(String.format("verze aplikace neznámá (%s), kontrola manifestu konfigurace %s vypnuta", appVersion, dirPath));
            return new Result(Status.OK, null, null);
        }
        int[] app = parseMajorMinor(appVersion);
        if (app == null) {
            throw new IllegalStateException(String.format("neplatná verze aplikace '%s'", appVersion));
        }
        File manifest = new File(configDir, FILE_NAME);
        if (!manifest.exists()) {
            return new Result(Status.MISSING, null, String.format(
                    "Adresář konfigurace %s neobsahuje soubor %s. Jde o konfiguraci pro verzi validátoru starší než 3.0, "
                            + "nebo to není adresář konfigurace validátoru. Validátor verze %s potřebuje konfiguraci verze %d.x.",
                    dirPath, FILE_NAME, appVersion, app[0]));
        }
        String configVersion = readConfigVersion(manifest);
        int[] config = configVersion == null ? null : parseMajorMinor(configVersion);
        if (config == null || !MAJOR_MINOR.matcher(configVersion).matches()) {
            return new Result(Status.INVALID, null, String.format(
                    "Soubor %s nejde přečíst nebo neobsahuje platný údaj %s (očekává se tvar MAJOR.MINOR, například 3.0).",
                    manifest.getAbsolutePath(), KEY_CONFIG_VERSION));
        }
        if (config[0] != app[0]) {
            return new Result(Status.DIFFERENT_MAJOR, configVersion, String.format(
                    "Adresář konfigurace %s obsahuje konfiguraci verze %s. Validátor verze %s potřebuje konfiguraci verze %d.x.",
                    dirPath, configVersion, appVersion, app[0]));
        }
        if (config[1] < app[1]) {
            return new Result(Status.OLDER_MINOR, configVersion, String.format(
                    "Adresář konfigurace %s obsahuje konfiguraci verze %s, validátor má verzi %s. "
                            + "Typy dokumentů a verze DMF přidané v novějších verzích validátoru v ní chybějí.",
                    dirPath, configVersion, appVersion));
        }
        if (config[1] > app[1]) {
            return new Result(Status.NEWER_MINOR, configVersion, String.format(
                    "Adresář konfigurace %s obsahuje konfiguraci verze %s, která je novější než validátor verze %s. "
                            + "Některé její části validátor nemusí umět načíst.",
                    dirPath, configVersion, appVersion));
        }
        return new Result(Status.OK, configVersion, null);
    }

    /**
     * Vyhodnotí manifest a pro stavy DIFFERENT_MAJOR, MISSING a INVALID vyhodí výjimku s hláškou pro uživatele.
     * OLDER_MINOR a NEWER_MINOR chyba nejsou, vrátí se výsledek, aby je mohlo GUI ohlásit jako varování.
     */
    public static Result check(File configDir) throws ValidatorConfigurationException {
        return check(Version.VERSION_CODE, configDir);
    }

    static Result check(String appVersion, File configDir) throws ValidatorConfigurationException {
        Result result = evaluate(appVersion, configDir);
        if (result.getStatus().isError()) {
            throw new ValidatorConfigurationException(result.getMessage());
        }
        return result;
    }

    /** Hodnota klíče configVersion (oříznutá), nebo null, když soubor nejde přečíst nebo klíč chybí či je prázdný. */
    private static String readConfigVersion(File manifest) {
        Properties props = new Properties();
        try (Reader in = new InputStreamReader(Files.newInputStream(manifest.toPath()), StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (IOException | IllegalArgumentException e) {
            //IllegalArgumentException: neplatna \\uXXXX sekvence
            LOG.warning(String.format("nelze číst %s: %s", manifest.getAbsolutePath(), e.getMessage()));
            return null;
        }
        String value = props.getProperty(KEY_CONFIG_VERSION);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /** MAJOR.MINOR verze aplikace pro zobrazení (3.0-beta -> 3.0, 3.1.2 -> 3.1), nebo null, když verze nemá platný tvar. */
    public static String majorMinor(String version) {
        int[] parsed = parseMajorMinor(version);
        return parsed == null ? null : parsed[0] + "." + parsed[1];
    }

    /**
     * Major a minor z verze: vše před pomlčkou, čísla oddělená tečkou (3.0-beta -> [3,0], 3.1.2 -> [3,1], 3 -> [3,0]).
     *
     * @return {major, minor}, nebo null, když verze není ve tvaru číslo[.číslo...]
     */
    static int[] parseMajorMinor(String version) {
        if (version == null) {
            return null;
        }
        String pure = version.split("-", 2)[0].trim();
        Matcher m = APP_VERSION.matcher(pure);
        if (!m.matches()) {
            return null;
        }
        return new int[]{Integer.parseInt(m.group(1)), m.group(2) == null ? 0 : Integer.parseInt(m.group(2))};
    }
}
