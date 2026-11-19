package nkp.pspValidator.gui.newvalidation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Podtitul dialogu Nová validace podle zdroje: u skupiny se rozpoznávají balíky v adresáři, ne jeden balík.
 */
public class ValidationSourceTest {

    @Test
    public void dialogSubtitleBySource() {
        assertEquals("Po výběru balíku se rozpozná jeho typ a verze.", ValidationSource.DIRECTORY.getDialogSubtitle());
        assertEquals("Po výběru balíku se rozpozná jeho typ a verze.", ValidationSource.ZIP.getDialogSubtitle());
        assertEquals("Po výběru adresáře se rozpozná typ a verze každého balíku v něm.", ValidationSource.GROUP.getDialogSubtitle());
    }
}
