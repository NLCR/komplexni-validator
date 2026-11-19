package nkp.pspValidator.gui.settings;

/**
 * Záložky okna Nastavení v pořadí, v jakém jsou v seznamu vlevo. Klíč slouží k výběru záložky zvenku (snímky,
 * {@code -PsnapshotTab}).
 */
public enum SettingsTab {
    VERSIONS("versions", "Výchozí verze DMF"),
    TOOLS("tools", "Externí nástroje"),
    SKIPPING("skipping", "Přeskakovaná pravidla"),
    DICTIONARIES("dictionaries", "Slovníky"),
    DIRS("dirs", "Adresáře a protokoly");

    private final String key;
    private final String label;

    SettingsTab(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    /** Záložka podle klíče (bez ohledu na velikost písmen a okolní mezery), null pro neznámý nebo prázdný klíč. */
    public static SettingsTab fromKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        for (SettingsTab tab : values()) {
            if (tab.key.equalsIgnoreCase(trimmed)) {
                return tab;
            }
        }
        return null;
    }
}
