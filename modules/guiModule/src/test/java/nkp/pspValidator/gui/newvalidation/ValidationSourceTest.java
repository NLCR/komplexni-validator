package nkp.pspValidator.gui.newvalidation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Podtitul dialogu Nová validace podle zdroje (u skupiny se rozpoznávají balíky v adresáři nebo v souboru ZIP, ne
 * jeden balík) a kontrola cesty: skupina je adresář nebo soubor (ZIP se ověří až při rozpoznání).
 */
public class ValidationSourceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void dialogSubtitleBySource() {
        assertEquals("Po výběru balíku se rozpozná jeho typ a verze.", ValidationSource.DIRECTORY.getDialogSubtitle());
        assertEquals("Po výběru balíku se rozpozná jeho typ a verze.", ValidationSource.ZIP.getDialogSubtitle());
        assertEquals("Po výběru adresáře nebo souboru ZIP se rozpozná typ a verze každého balíku v něm.", ValidationSource.GROUP.getDialogSubtitle());
    }

    @Test
    public void pathLabels() {
        assertEquals("Adresář balíku", ValidationSource.DIRECTORY.getPathLabel());
        assertEquals("Soubor ZIP balíku", ValidationSource.ZIP.getPathLabel());
        assertEquals("Adresář nebo ZIP", ValidationSource.GROUP.getPathLabel());
    }

    @Test
    public void groupAcceptsDirectoryOrFile() throws Exception {
        File dir = tmp.newFolder("davka");
        File zip = tmp.newFile("davka.zip");
        assertNull(ValidationSource.GROUP.checkPath(dir.getPath()));
        assertNull(ValidationSource.GROUP.checkPath(" " + zip.getPath() + " "));
        //ze soubor neni ZIP, pozna az rozpoznani skupiny
        assertNull(ValidationSource.GROUP.checkPath(tmp.newFile("poznamky.txt").getPath()));
    }

    @Test
    public void groupPathProblems() {
        assertEquals("Vyberte adresář s balíky nebo soubor ZIP se skupinou balíků.", ValidationSource.GROUP.checkPath(null));
        assertEquals("Vyberte adresář s balíky nebo soubor ZIP se skupinou balíků.", ValidationSource.GROUP.checkPath("  "));
        File missing = new File(tmp.getRoot(), "neni.zip");
        assertEquals(missing.getPath() + " neexistuje.", ValidationSource.GROUP.checkPath(missing.getPath()));
    }

    /** Ostatní zdroje se nezměnily: adresář chce adresář, ZIP soubor. */
    @Test
    public void otherSourcesUnchanged() throws Exception {
        File dir = tmp.newFolder("balik");
        File file = tmp.newFile("balik.zip");
        assertEquals(file.getPath() + " není adresář.", ValidationSource.DIRECTORY.checkPath(file.getPath()));
        assertEquals(dir.getPath() + " není soubor.", ValidationSource.ZIP.checkPath(dir.getPath()));
        assertEquals("Vyberte adresář balíku.", ValidationSource.DIRECTORY.checkPath(""));
        assertEquals("Vyberte soubor ZIP s balíkem.", ValidationSource.ZIP.checkPath(""));
    }
}
