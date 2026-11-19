package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;

import java.io.File;

/**
 * Zdroj validace v dialogu Nová validace. Pořadí výčtu je pořadí přepínače. Skupina balíků je adresář, jehož přímé
 * položky (podadresáře a soubory .zip) se validují jako samostatné balíky (viz {@code gui.group.GroupPackages}).
 */
public enum ValidationSource {

    DIRECTORY("Adresář", "Adresář balíku", ConfigurationManager.PROP_LAST_PSP_DIR),
    ZIP("Soubor ZIP", "Soubor ZIP balíku", ConfigurationManager.PROP_LAST_PSP_ZIP),
    GROUP("Skupina balíků", "Adresář s balíky", ConfigurationManager.PROP_LAST_PSP_GROUP_DIR);

    private final String label;
    private final String pathLabel;
    private final String lastPathKey;

    ValidationSource(String label, String pathLabel, String lastPathKey) {
        this.label = label;
        this.pathLabel = pathLabel;
        this.lastPathKey = lastPathKey;
    }

    /** Text položky přepínače. */
    public String getLabel() {
        return label;
    }

    /** Popisek pole s cestou. */
    public String getPathLabel() {
        return pathLabel;
    }

    /** Podtitul dialogu Nová validace pro tento zdroj. */
    public String getDialogSubtitle() {
        return this == GROUP
                ? "Po výběru adresáře se rozpozná typ a verze každého balíku v něm."
                : "Po výběru balíku se rozpozná jeho typ a verze.";
    }

    /** Klíč v config.properties s naposledy vybranou cestou (stejný jako u dřívějších dialogů). */
    public String getLastPathKey() {
        return lastPathKey;
    }

    /**
     * Kontrola cesty před spuštěním validace a před rozpoznáním typu.
     *
     * @return srozumitelná chyba, nebo null, když je cesta v pořádku
     */
    public String checkPath(String pathText) {
        if (pathText == null || pathText.trim().isEmpty()) {
            switch (this) {
                case DIRECTORY:
                    return "Vyberte adresář balíku.";
                case GROUP:
                    return "Vyberte adresář s balíky.";
                default:
                    return "Vyberte soubor ZIP s balíkem.";
            }
        }
        File file = new File(pathText.trim());
        switch (this) {
            case DIRECTORY:
            case GROUP:
                if (!file.exists()) {
                    return String.format("Adresář %s neexistuje.", file.getPath());
                } else if (!file.isDirectory()) {
                    return String.format("%s není adresář.", file.getPath());
                } else if (!file.canRead()) {
                    return String.format("Nelze číst obsah adresáře %s.", file.getPath());
                }
                return null;
            case ZIP:
                if (!file.exists()) {
                    return String.format("Soubor %s neexistuje.", file.getPath());
                } else if (!file.isFile()) {
                    return String.format("%s není soubor.", file.getPath());
                } else if (!file.canRead()) {
                    return String.format("Nelze číst soubor %s.", file.getPath());
                }
                return null;
            default:
                throw new IllegalStateException();
        }
    }
}
