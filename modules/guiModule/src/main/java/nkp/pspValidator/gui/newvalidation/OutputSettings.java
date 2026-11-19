package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;

import java.util.Arrays;
import java.util.List;

/**
 * Výstupy validace (XML protokol, textový log a jeho podrobnost) uložené ve stejných klíčích config.properties jako
 * u dřívějších dialogů validace. Výchozí hodnoty také stejné: bez protokolu, bez logu, podrobnost 3 (úplně vše).
 */
public class OutputSettings {

    /** Podrobnost textového logu, čísla jsou hodnoty pro Validator.run a klíč text_log.verbosity. */
    public enum Verbosity {
        ALL(3, "úplně vše"),
        SECTIONS_AND_RULES_WITH_ERRORS(2, "jen sekce a pravidla obsahující chyby"),
        SECTIONS_WITH_ERRORS(1, "jen sekce obsahující chyby"),
        VALID_OR_INVALID(0, "jen validní/nevalidní");

        private final int level;
        private final String label;

        Verbosity(int level, String label) {
            this.level = level;
            this.label = label;
        }

        public int getLevel() {
            return level;
        }

        public String getLabel() {
            return label;
        }

        /** Pořadí v nabídce: od nejpodrobnější. */
        public static List<Verbosity> menuOrder() {
            return Arrays.asList(values());
        }

        public static Verbosity ofLevel(Integer level) {
            if (level != null) {
                for (Verbosity verbosity : values()) {
                    if (verbosity.level == level) {
                        return verbosity;
                    }
                }
            }
            return ALL;
        }

        @Override
        public String toString() {
            return "podrobnost: " + label;
        }
    }

    private final ConfigurationManager config;

    public OutputSettings(ConfigurationManager config) {
        this.config = config;
    }

    public boolean isCreateXmlLog() {
        return config.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, false);
    }

    public void setCreateXmlLog(boolean create) {
        config.setBoolean(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, create);
    }

    public boolean isCreateTxtLog() {
        return config.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, false);
    }

    public void setCreateTxtLog(boolean create) {
        config.setBoolean(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, create);
    }

    /** Uložená podrobnost; chybějící nebo neznámá hodnota znamená úplně vše (jako dřív výchozí volba v dialogu). */
    public Verbosity getVerbosity() {
        Integer level;
        try {
            level = config.getIntegerOrNull(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY);
        } catch (NumberFormatException e) {
            level = null;
        }
        return Verbosity.ofLevel(level);
    }

    public void setVerbosity(Verbosity verbosity) {
        config.setInteger(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY, verbosity.getLevel());
    }
}
