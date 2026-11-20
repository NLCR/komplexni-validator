package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.shared.Dmf;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Skupina ze souboru ZIP: balíky se validují v rozbaleném archivu, ale tabulka, CSV, stavový řádek a nadpis ukazují
 * cestu k souboru ZIP a balíku v něm, ne dočasný adresář. Cesty jsou napsané ručně (oddělovač podle systému přes File).
 */
public class GroupInZipTest {

    private static final File ZIP = new File("/data/Dávka 1.zip");
    private static final File EXTRACT = new File("/tmp/kv-rozbaleno-123/Dávka 1");

    private static GroupZipLayout rootLayout() {
        return GroupZipLayout.of(Arrays.asList("nk-01/mets.xml", "nk-02.zip", "precti.txt"));
    }

    private static GroupZipLayout singleRootLayout() {
        return GroupZipLayout.of(Arrays.asList("Dávka 1/", "Dávka 1/Balík č. 1/mets.xml", "Dávka 1/nk-02.zip"));
    }

    @Test
    public void listingAfterUnpackWithPackagesInRoot() {
        GroupPackages.Listing listing = GroupPackages.inZip(ZIP, rootLayout(), EXTRACT);
        assertEquals(EXTRACT, listing.getGroupDir());
        assertEquals(ZIP, listing.getZip());
        assertEquals(ZIP, listing.getSource());
        assertEquals(Collections.singletonList("precti.txt"), listing.getIgnored());
        GroupPackages.Entry dir = listing.getPackages().get(0);
        GroupPackages.Entry zip = listing.getPackages().get(1);
        //validuje se rozbaleny adresar ...
        assertEquals(new File("/tmp/kv-rozbaleno-123/Dávka 1/nk-01"), dir.getFile());
        assertFalse(dir.isZip());
        assertEquals(new File("/tmp/kv-rozbaleno-123/Dávka 1/nk-02.zip"), zip.getFile());
        assertTrue(zip.isZip());
        //... uzivatel vidi soubor ZIP a balik v nem
        assertEquals(new File("/data/Dávka 1.zip/nk-01").getAbsolutePath(), dir.getDisplayPath());
        assertEquals(new File("/data/Dávka 1.zip/nk-02.zip").getAbsolutePath(), zip.getDisplayPath());
        assertEquals("nk-01", dir.getName());
    }

    @Test
    public void listingAfterUnpackWithSingleRootDirectory() {
        GroupPackages.Listing listing = GroupPackages.inZip(ZIP, singleRootLayout(), EXTRACT);
        assertEquals(new File("/tmp/kv-rozbaleno-123/Dávka 1/Dávka 1"), listing.getGroupDir());
        GroupPackages.Entry first = listing.getPackages().get(0);
        assertEquals(new File("/tmp/kv-rozbaleno-123/Dávka 1/Dávka 1/Balík č. 1"), first.getFile());
        assertEquals(new File("/data/Dávka 1.zip/Dávka 1/Balík č. 1").getAbsolutePath(), first.getDisplayPath());
        assertEquals("Balík č. 1", first.getName());
    }

    /** V dialogu (před rozbalením) soubory balíků jen označují místo v archivu. */
    @Test
    public void listingBeforeUnpack() {
        GroupPackages.Listing listing = GroupPackages.inZip(ZIP, rootLayout(), null);
        assertEquals(ZIP, listing.getGroupDir());
        assertEquals(new File("/data/Dávka 1.zip/nk-01"), listing.getPackages().get(0).getFile());
    }

    /** Skupina v adresáři: cesta pro uživatele je cesta balíku, zdroj je adresář. */
    @Test
    public void directoryGroupDisplayPath() {
        GroupPackages.Entry entry = new GroupPackages.Entry(new File("/data/davka/nk-01"), false);
        assertEquals(new File("/data/davka/nk-01").getAbsolutePath(), entry.getDisplayPath());
        GroupPackages.Listing listing = new GroupPackages.Listing(new File("/data/davka"), Collections.singletonList(entry),
                Collections.<String>emptyList());
        assertNull(listing.getZip());
        assertEquals(new File("/data/davka"), listing.getSource());
        GroupRun run = new GroupRun(listing);
        assertFalse(run.isFromZip());
        assertEquals("davka", run.getTitle());
        assertEquals("Adresář skupiny: " + new File("/data/davka").getAbsolutePath(), run.sourceDescription());
        assertEquals("davka-souhrn.csv", run.csvFileName());
    }

    @Test
    public void runTextsUseZip() {
        GroupRun run = new GroupRun(GroupPackages.inZip(ZIP, rootLayout(), EXTRACT));
        assertTrue(run.isFromZip());
        assertEquals(EXTRACT, run.getGroupDir());
        assertEquals(ZIP, run.getSource());
        assertEquals("Dávka 1.zip", run.getTitle());
        assertEquals("Soubor ZIP se skupinou: " + ZIP.getAbsolutePath(), run.sourceDescription());
        assertEquals("Dávka 1-souhrn.csv", run.csvFileName());
        run.runStarted(run.getItems(), 0);
        run.runEnded(1000, LocalDateTime.of(2026, 11, 20, 10, 0));
        assertEquals("Validace skupiny " + ZIP.getAbsolutePath() + " hotova.", run.statusText());
        GroupRun canceled = new GroupRun(GroupPackages.inZip(ZIP, rootLayout(), EXTRACT));
        canceled.runStarted(canceled.getItems(), 0);
        canceled.cancelRequested();
        canceled.runEnded(1000, LocalDateTime.of(2026, 11, 20, 10, 0));
        assertEquals("Validace skupiny " + ZIP.getAbsolutePath() + " zrušena.", canceled.statusText());
    }

    /** Přípona .zip se z výchozího názvu CSV odstraní bez ohledu na velikost písmen, jiná přípona zůstane. */
    @Test
    public void csvFileNameEdges() {
        assertEquals("DAVKA-souhrn.csv", new GroupRun(GroupPackages.inZip(new File("/d/DAVKA.ZIP"), rootLayout(), EXTRACT)).csvFileName());
        assertEquals("davka.dat-souhrn.csv", new GroupRun(GroupPackages.inZip(new File("/d/davka.dat"), rootLayout(), EXTRACT)).csvFileName());
    }

    @Test
    public void tableTooltipAndCsvShowZipPath() throws Exception {
        GroupRun run = new GroupRun(GroupPackages.inZip(ZIP, singleRootLayout(), EXTRACT));
        GroupItem dir = run.getItems().get(0);
        GroupItem zip = run.getItems().get(1);
        String dirPath = new File("/data/Dávka 1.zip/Dávka 1/Balík č. 1").getAbsolutePath();
        String zipPath = new File("/data/Dávka 1.zip/Dávka 1/nk-02.zip").getAbsolutePath();
        assertEquals("Adresář: " + dirPath, GroupDetailTexts.nameTooltip(dir));
        assertEquals("Soubor ZIP: " + zipPath, GroupDetailTexts.nameTooltip(zip));
        assertEquals(dirPath, dir.getDisplayPath());

        run.runStarted(run.getItems(), 0);
        run.packageStarted(dir);
        run.packageFinished(dir, new Dmf(Dmf.Type.MONOGRAPH, "1.2"), new ProblemCounts(0, 1, 2), true, 1500, null, null);
        run.packageStarted(zip);
        run.packageFailed(zip, null, null, "Chyba: Soubor není korektní ZIP", 100);
        run.runEnded(2000, LocalDateTime.of(2026, 11, 20, 10, 0));
        assertEquals("Soubor ZIP: " + zipPath + "\nChyba: Soubor není korektní ZIP", GroupDetailTexts.nameTooltip(zip));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GroupCsv.write(run.getItems(), out);
        String csv = new String(out.toByteArray(), 3, out.size() - 3, StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertEquals(3, lines.length);
        assertEquals("Balík č. 1;" + dirPath + ";Monografie;1.2;validní;0;1;2;1,5;;;", lines[1]);
        assertEquals("nk-02.zip;" + zipPath + ";;;chyba;;;;0,1;;;Chyba: Soubor není korektní ZIP", lines[2]);
        assertFalse("dočasný adresář se v CSV neukazuje", csv.contains("kv-rozbaleno"));
    }
}
