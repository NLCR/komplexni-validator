package nkp.pspValidator.gui.settings;

import nkp.pspValidator.shared.ConfigManifest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigWarningsTest {

    @Test
    public void warningOnlyForDifferentMinor() {
        assertEquals("zpráva", ConfigWarnings.warning(ConfigManifest.Status.OLDER_MINOR, "zpráva"));
        assertEquals("zpráva", ConfigWarnings.warning(ConfigManifest.Status.NEWER_MINOR, "zpráva"));
        assertNull(ConfigWarnings.warning(ConfigManifest.Status.OK, null));
        //chybové stavy se hlásí jako chyba, ne jako upozornění
        assertNull(ConfigWarnings.warning(ConfigManifest.Status.DIFFERENT_MAJOR, "zpráva"));
        assertNull(ConfigWarnings.warning(ConfigManifest.Status.MISSING, "zpráva"));
        assertNull(ConfigWarnings.warning(ConfigManifest.Status.INVALID, "zpráva"));
    }

    /** okno inicializace se nezavře samo, právě když je co ukázat (výsledek není null) */
    @Test
    public void startNoticeDecidesWhetherWindowStaysOpen() {
        assertNull(ConfigWarnings.startNotice(null, null));
        assertEquals("nahrazeno", ConfigWarnings.startNotice("nahrazeno", null));
        assertEquals("verze", ConfigWarnings.startNotice(null, "verze"));
        assertEquals("nahrazeno\nverze", ConfigWarnings.startNotice("nahrazeno", "verze"));
    }
}
