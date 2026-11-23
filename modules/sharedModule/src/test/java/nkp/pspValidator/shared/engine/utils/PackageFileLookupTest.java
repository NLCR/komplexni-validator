package nkp.pspValidator.shared.engine.utils;

import nkp.pspValidator.shared.engine.Utils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * PackageFileLookup: hledani souboru balíku podle cesty z metadat se shodou velikosti pismen. Testy bezi na macOS
 * a Windows (system souboru velikost pismen nerozlisuje) i na Linuxu (rozlisuje) a ocekavani plati vsude stejne:
 * trida porovnava cesty s vypisy adresaru, ne dotazem na system souboru.
 * <p>
 * Balicek v docasnem adresari: mastercopy/mc_0001.jp2, příloha/Čtení.xml, info.xml.
 */
public class PackageFileLookupTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File root;
    private PackageFileLookup lookup;

    @Before
    public void setUp() throws Exception {
        root = tmp.newFolder("balik");
        assertTrue(new File(root, "mastercopy").mkdir());
        Files.write(new File(root, "mastercopy/mc_0001.jp2").toPath(), new byte[]{1, 2, 3});
        assertTrue(new File(root, "příloha").mkdir());
        Files.write(new File(root, "příloha/Čtení.xml").toPath(), "<a/>".getBytes("UTF-8"));
        Files.write(new File(root, "info.xml").toPath(), "<info/>".getBytes("UTF-8"));
        lookup = new PackageFileLookup();
    }

    private PackageFileLookup.Result find(String pathFromMetadata) throws Exception {
        return lookup.lookup(root, Utils.buildAbsoluteFile(root, pathFromMetadata));
    }

    private static void assertResult(PackageFileLookup.Result r, PackageFileLookup.Status status, String requested, String actual) {
        assertEquals(status, r.getStatus());
        assertEquals(requested, r.getRequestedRelativePath());
        assertEquals(actual, r.getActualRelativePath());
    }

    @Test
    public void exactMatch() throws Exception {
        PackageFileLookup.Result r = find("mastercopy/mc_0001.jp2");
        assertResult(r, PackageFileLookup.Status.FOUND_EXACT, "mastercopy/mc_0001.jp2", "mastercopy/mc_0001.jp2");
        assertFalse(r.isCaseDiffers());
        assertEquals(new File(root, "mastercopy/mc_0001.jp2").getAbsolutePath(), r.getActualFile().getAbsolutePath());
        assertTrue(r.getActualFile().isFile());
    }

    @Test
    public void exactMatchWithBackslashesAndLeadingSlash() throws Exception {
        //oddelovace a uvodni lomitko jako v souboru INFO a CHECKSUM (Utils.buildAbsoluteFile)
        assertResult(find("\\mastercopy\\mc_0001.jp2"), PackageFileLookup.Status.FOUND_EXACT, "mastercopy/mc_0001.jp2", "mastercopy/mc_0001.jp2");
        assertResult(find("/mastercopy/mc_0001.jp2"), PackageFileLookup.Status.FOUND_EXACT, "mastercopy/mc_0001.jp2", "mastercopy/mc_0001.jp2");
        assertResult(find("./mastercopy/mc_0001.jp2"), PackageFileLookup.Status.FOUND_EXACT, "mastercopy/mc_0001.jp2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void fileNameCaseDiffers() throws Exception {
        PackageFileLookup.Result r = find("mastercopy/MC_0001.jp2");
        assertResult(r, PackageFileLookup.Status.FOUND_CASE_DIFFERS, "mastercopy/MC_0001.jp2", "mastercopy/mc_0001.jp2");
        assertTrue(r.isCaseDiffers());
        assertEquals(new File(root, "mastercopy/mc_0001.jp2").getAbsolutePath(), r.getActualFile().getAbsolutePath());
        assertEquals("soubor mastercopy/MC_0001.jp2 nenalezen; v balíku je soubor mastercopy/mc_0001.jp2, jehož název se liší velikostí písmen",
                r.caseDiffersMessage());
    }

    @Test
    public void extensionCaseDiffers() throws Exception {
        assertResult(find("mastercopy/mc_0001.JP2"), PackageFileLookup.Status.FOUND_CASE_DIFFERS, "mastercopy/mc_0001.JP2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void directoryCaseDiffers() throws Exception {
        assertResult(find("masterCopy/mc_0001.jp2"), PackageFileLookup.Status.FOUND_CASE_DIFFERS, "masterCopy/mc_0001.jp2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void directoryAndFileCaseDiffer() throws Exception {
        assertResult(find("MasterCopy/MC_0001.jp2"), PackageFileLookup.Status.FOUND_CASE_DIFFERS, "MasterCopy/MC_0001.jp2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void notFound() throws Exception {
        //jiny nazev souboru
        assertResult(find("mastercopy/mc_0002.jp2"), PackageFileLookup.Status.NOT_FOUND, "mastercopy/mc_0002.jp2", null);
        //neexistujici adresar
        assertResult(find("usercopy/mc_0001.jp2"), PackageFileLookup.Status.NOT_FOUND, "usercopy/mc_0001.jp2", null);
        //neexistujici adresar lisici se i velikosti pismen
        assertResult(find("UserCopy/mc_0001.jp2"), PackageFileLookup.Status.NOT_FOUND, "UserCopy/mc_0001.jp2", null);
        assertNull(find("mastercopy/mc_0002.jp2").getActualFile());
        //nazev lisici se jen o jeden znak navic
        assertResult(find("mastercopy/mc_0001.jp22"), PackageFileLookup.Status.NOT_FOUND, "mastercopy/mc_0001.jp22", null);
    }

    @Test
    public void directoryIsFoundAsWell() throws Exception {
        //trida hleda soubor i adresar; zda je to soubor, kontroluje volajici
        PackageFileLookup.Result exact = find("mastercopy");
        assertResult(exact, PackageFileLookup.Status.FOUND_EXACT, "mastercopy", "mastercopy");
        assertTrue(exact.getActualFile().isDirectory());
        PackageFileLookup.Result differs = find("MASTERCOPY");
        assertResult(differs, PackageFileLookup.Status.FOUND_CASE_DIFFERS, "MASTERCOPY", "mastercopy");
        assertTrue(differs.getActualFile().isDirectory());
    }

    @Test
    public void fileUsedAsDirectoryIsNotFound() throws Exception {
        assertResult(find("info.xml/mc_0001.jp2"), PackageFileLookup.Status.NOT_FOUND, "info.xml/mc_0001.jp2", null);
        assertResult(find("INFO.xml/x"), PackageFileLookup.Status.NOT_FOUND, "INFO.xml/x", null);
    }

    @Test
    public void diacritics() throws Exception {
        assertResult(find("příloha/Čtení.xml"), PackageFileLookup.Status.FOUND_EXACT, "příloha/Čtení.xml", "příloha/Čtení.xml");
        //velka a mala pismena s diakritikou
        PackageFileLookup.Result r = find("PŘÍLOHA/čtení.xml");
        assertResult(r, PackageFileLookup.Status.FOUND_CASE_DIFFERS, "PŘÍLOHA/čtení.xml", "příloha/Čtení.xml");
        assertEquals("soubor PŘÍLOHA/čtení.xml nenalezen; v balíku je soubor příloha/Čtení.xml, jehož název se liší velikostí písmen",
                r.caseDiffersMessage());
        //jine slozeni znaku (NFD) neni rozdil ve velikosti pismen
        String nfd = Normalizer.normalize("příloha/Čtení.xml", Normalizer.Form.NFD);
        assertNotEquals("příloha/Čtení.xml", nfd);
        assertEquals(PackageFileLookup.Status.FOUND_EXACT, find(nfd).getStatus());
        //bez diakritiky to neni tentyz nazev
        assertEquals(PackageFileLookup.Status.NOT_FOUND, find("priloha/Cteni.xml").getStatus());
    }

    /**
     * Dva soubory lisici se jen velikosti pismen lze vytvorit jen na systemu souboru, ktery velikost rozlisuje (Linux).
     * Na macOS a Windows se test preskoci.
     */
    @Test
    public void twoFilesDifferingOnlyInCase() throws Exception {
        File dir = new File(root, "alto");
        assertTrue(dir.mkdir());
        Files.write(new File(dir, "a.xml").toPath(), new byte[]{1});
        File upper = new File(dir, "A.xml");
        boolean caseSensitive = !upper.exists();
        Assume.assumeTrue("system souboru nerozlisuje velikost pismen", caseSensitive);
        Files.write(upper.toPath(), new byte[]{2});
        assertEquals(2, dir.list().length);

        assertResult(find("alto/a.xml"), PackageFileLookup.Status.FOUND_EXACT, "alto/a.xml", "alto/a.xml");
        assertResult(find("alto/A.xml"), PackageFileLookup.Status.FOUND_EXACT, "alto/A.xml", "alto/A.xml");
        //obe varianty se lisi, hlasi se prvni podle razeni ("A.xml" < "a.xml"), nezavisle na poradi vypisu adresare
        assertResult(find("alto/a.XML"), PackageFileLookup.Status.FOUND_CASE_DIFFERS, "alto/a.XML", "alto/A.xml");
    }

    @Test
    public void emptyPathIsNotAFile() throws Exception {
        assertResult(find(""), PackageFileLookup.Status.NOT_FOUND, "", null);
        assertResult(lookup.lookup(root, root), PackageFileLookup.Status.NOT_FOUND, "", null);
    }

    @Test
    public void absolutePathInsideRoot() {
        File absolute = new File(root.getAbsolutePath() + File.separator + "mastercopy" + File.separator + "MC_0001.jp2");
        assertTrue(absolute.isAbsolute());
        assertResult(lookup.lookup(root, absolute), PackageFileLookup.Status.FOUND_CASE_DIFFERS, "mastercopy/MC_0001.jp2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void pathOutsideRootIsNotChecked() throws Exception {
        File other = tmp.newFile("jinde.xml");
        PackageFileLookup.Result r = lookup.lookup(root, other);
        assertEquals(PackageFileLookup.Status.OUTSIDE_ROOT, r.getStatus());
        assertNull(r.getRequestedRelativePath());
        assertNull(r.getActualRelativePath());
        assertNull(r.getActualFile());
        //cesta s '..' vedouci ven z balíku
        assertEquals(PackageFileLookup.Status.OUTSIDE_ROOT, lookup.lookup(root, new File(root, "../jinde.xml")).getStatus());
        assertEquals(PackageFileLookup.Status.OUTSIDE_ROOT, lookup.lookup(root, new File(root, "mastercopy/../../balik2/x")).getStatus());
        //adresar se stejnym zacatkem nazvu neni uvnitr balíku
        assertEquals(PackageFileLookup.Status.OUTSIDE_ROOT, lookup.lookup(root, new File(root.getAbsolutePath() + "x", "mastercopy/mc_0001.jp2")).getStatus());
    }

    @Test
    public void pathWithDotsInsideRoot() {
        assertResult(lookup.lookup(root, new File(root, "mastercopy/../mastercopy/MC_0001.jp2")),
                PackageFileLookup.Status.FOUND_CASE_DIFFERS, "mastercopy/MC_0001.jp2", "mastercopy/mc_0001.jp2");
        assertResult(lookup.lookup(root, new File(root, "./mastercopy/./mc_0001.jp2")),
                PackageFileLookup.Status.FOUND_EXACT, "mastercopy/mc_0001.jp2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void rootGivenAsRelativePath() throws Exception {
        //koren balíku zadany relativne (napr. v testech src/test/resources/...): porovnava se po prevodu na absolutni cestu
        File cwd = new File("").getAbsoluteFile();
        File relativeRoot = cwd.toPath().relativize(root.toPath()).toFile();
        Assume.assumeFalse("docasny adresar na jinem disku", relativeRoot.isAbsolute());
        assertResult(lookup.lookup(relativeRoot, Utils.buildAbsoluteFile(relativeRoot, "mastercopy/MC_0001.jp2")),
                PackageFileLookup.Status.FOUND_CASE_DIFFERS, "mastercopy/MC_0001.jp2", "mastercopy/mc_0001.jp2");
    }

    @Test
    public void directoryListingIsCachedUntilCleared() throws Exception {
        assertEquals(0, lookup.getListingsLoaded());
        find("mastercopy/mc_0001.jp2");
        //koren a mastercopy
        assertEquals(2, lookup.getListingsLoaded());
        find("mastercopy/MC_0001.jp2");
        find("mastercopy/mc_0002.jp2");
        find("info.xml");
        assertEquals(2, lookup.getListingsLoaded());

        //soubor pridany behem validace se do vymazani mezipameti neprojevi (vypis adresare je z doby prvniho dotazu)
        Files.write(new File(root, "mastercopy/mc_0002.jp2").toPath(), new byte[]{1});
        assertEquals(PackageFileLookup.Status.NOT_FOUND, find("mastercopy/mc_0002.jp2").getStatus());
        lookup.clear();
        assertEquals(PackageFileLookup.Status.FOUND_EXACT, find("mastercopy/mc_0002.jp2").getStatus());
        assertEquals(4, lookup.getListingsLoaded());
    }

    @Test
    public void pathKeyDistinguishesCaseOnEverySystem() {
        File lower = new File(root, "mastercopy/mc_0001.jp2");
        File upper = new File(root, "mastercopy/MC_0001.jp2");
        assertFalse(PackageFileLookup.samePath(lower, upper));
        assertTrue(PackageFileLookup.samePath(lower, new File(root.getAbsolutePath(), "mastercopy/mc_0001.jp2")));
        assertEquals(2, PackageFileLookup.byPathKey(Arrays.asList(lower, upper, lower)).size());
    }

    @Test
    public void findCaseVariant() {
        File ref = new File(root, "masterCopy/MC_0001.jp2");
        File actual = new File(root, "mastercopy/mc_0001.jp2");
        File other = new File(root, "mastercopy/mc_0002.jp2");
        assertEquals(actual.getAbsoluteFile(), PackageFileLookup.findCaseVariant(ref, Arrays.asList(other, actual)));
        assertNull(PackageFileLookup.findCaseVariant(ref, Collections.singletonList(other)));
        //stejna cesta neni varianta
        assertNull(PackageFileLookup.findCaseVariant(actual, Collections.singletonList(actual)));
        assertNull(PackageFileLookup.findCaseVariant(ref, Collections.emptyList()));
        //vice variant: vysledek nezavisi na poradi, vybere se prvni podle razeni cest ("MASTERCOPY" < "mastercopy")
        File variant2 = new File(root, "MASTERCOPY/MC_0001.JP2");
        assertEquals(variant2.getAbsoluteFile(), PackageFileLookup.findCaseVariant(ref, Arrays.asList(actual, variant2)));
        assertEquals(variant2.getAbsoluteFile(), PackageFileLookup.findCaseVariant(ref, Arrays.asList(variant2, actual)));
    }

    @Test
    public void messages() {
        assertEquals("soubor a/B.txt nenalezen; v balíku je soubor a/b.txt, jehož název se liší velikostí písmen",
                PackageFileLookup.caseDiffersMessage("a/B.txt", "a/b.txt"));
        assertEquals("; v balíku je soubor a/b.txt, jehož název se liší velikostí písmen", PackageFileLookup.actualFileHint("a/b.txt"));
        assertEquals("; odkaz v metadatech (a/B.txt) se liší velikostí písmen", PackageFileLookup.referenceHint("a/B.txt"));
    }

    /**
     * Rejstřík variant: varianta se najde bez ohledu na velikost písmen, sama cesta variantou není, ze dvou variant
     * se vrátí ta s menším klíčem a pořadí kandidátů výsledek nemění.
     */
    @Test
    public void caseVariantIndexGivesTheSameAsSearchInCollection() {
        File a = new File("/psp/mastercopy/mc_1.jp2");
        File upperDir = new File("/psp/MASTERCOPY/mc_1.jp2");
        File upperName = new File("/psp/mastercopy/MC_1.jp2");
        File other = new File("/psp/mastercopy/mc_2.jp2");
        java.util.List<File> candidates = java.util.Arrays.asList(upperName, other, a, upperDir);
        java.util.Map<String, java.util.TreeMap<String, File>> index = PackageFileLookup.caseVariantIndex(candidates);
        //"/psp/MASTERCOPY/…" je v razeni retezcu pred "/psp/mastercopy/MC_1.jp2" (velka pismena maji mensi kod)
        assertEquals(upperDir, PackageFileLookup.findCaseVariant(a, index));
        assertEquals(upperDir, PackageFileLookup.findCaseVariant(a, candidates));
        assertEquals(a.getAbsoluteFile(), PackageFileLookup.findCaseVariant(new File("/psp/Mastercopy/Mc_1.jp2"),
                PackageFileLookup.caseVariantIndex(java.util.Arrays.asList(other, a))).getAbsoluteFile());
        assertNull(PackageFileLookup.findCaseVariant(other, index));
        assertNull(PackageFileLookup.findCaseVariant(new File("/psp/mastercopy/mc_3.jp2"), index));
        assertNull(PackageFileLookup.findCaseVariant(a, PackageFileLookup.caseVariantIndex(java.util.Collections.singletonList(a))));
    }
}
