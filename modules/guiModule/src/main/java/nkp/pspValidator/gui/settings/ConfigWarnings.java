package nkp.pspValidator.gui.settings;

import nkp.pspValidator.shared.ConfigManifest;

/**
 * Neblokující upozornění na konfiguraci validátoru jiné vedlejší verze než aplikace (stejná major, jiná minor:
 * {@link ConfigManifest.Status#OLDER_MINOR}, {@link ConfigManifest.Status#NEWER_MINOR}). Konfigurace se načte a použije,
 * uživatel se jen upozorní (dialog inicializace při startu, Nastavení, hlavní okno). Bez JavaFX.
 */
public final class ConfigWarnings {

    private ConfigWarnings() {
    }

    /**
     * Upozornění pro stav manifestu: jeho hláška u OLDER_MINOR a NEWER_MINOR, jinak null (OK, nebo chybový stav, který
     * se hlásí jako chyba).
     */
    public static String warning(ConfigManifest.Status status, String message) {
        if (status == ConfigManifest.Status.OLDER_MINOR || status == ConfigManifest.Status.NEWER_MINOR) {
            return message;
        }
        return null;
    }

    /**
     * Text upozornění v okně inicializace při startu: upozornění na nahrazený adresář a na verzi konfigurace pod
     * sebou (každé může být null). Null, když není co ukázat; okno se pak zavře samo, jinak zůstane otevřené
     * a čeká na „Pokračovat“.
     */
    public static String startNotice(String replacedDirNotice, String manifestWarning) {
        if (replacedDirNotice == null) {
            return manifestWarning;
        }
        if (manifestWarning == null) {
            return replacedDirNotice;
        }
        return replacedDirNotice + "\n" + manifestWarning;
    }
}
