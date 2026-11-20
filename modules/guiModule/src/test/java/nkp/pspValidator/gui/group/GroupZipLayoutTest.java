package nkp.pspValidator.gui.group;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Skupina v souboru ZIP: určení balíků jen ze seznamu názvů položek archivu (kořen archivu nebo jediný kořenový
 * adresář, adresáře a vnořené ZIPy, ignorované a skryté položky, chyby). Očekávané hodnoty jsou napsané ručně.
 */
public class GroupZipLayoutTest {

    private static GroupZipLayout layout(String... names) {
        return GroupZipLayout.of(Arrays.asList(names));
    }

    /** Balíky jako „dir cesta“ / „zip cesta“ v pořadí rozložení. */
    private static List<String> packages(GroupZipLayout layout) {
        List<String> result = new ArrayList<>();
        for (GroupZipLayout.Package pkg : layout.getPackages()) {
            result.add(pkg.toString());
        }
        return result;
    }

    @Test
    public void packagesInZipRoot() {
        GroupZipLayout layout = layout("nk-02/", "nk-02/mets_nk-02.xml", "nk-02/info_nk-02.xml", "nk-02/txt/a.txt",
                "nk-01/mets_nk-01.xml", "nk-03.zip", "poznamky.txt");
        assertEquals(GroupZipLayout.Status.OK, layout.getStatus());
        assertTrue(layout.isOk());
        assertEquals("", layout.getGroupPrefix());
        //adresar nk-01 jen jako cast cesty souboru (bez polozky "nk-01/"), serazeno podle nazvu
        assertEquals(Arrays.asList("dir nk-01", "dir nk-02", "zip nk-03.zip"), packages(layout));
        assertEquals(Collections.singletonList("poznamky.txt"), layout.getIgnored());
        assertNull(layout.errorMessage());
        assertNull(layout.getProblemEntry());
        GroupZipLayout.Package first = layout.getPackages().get(0);
        assertEquals("nk-01", first.getName());
        assertEquals("nk-01", first.getPath());
        assertFalse(first.isZip());
        assertTrue(layout.getPackages().get(2).isZip());
    }

    /** Jediný kořenový adresář s balíky: balíky jsou v něm (jako CLI). */
    @Test
    public void singleRootDirectoryWithPackages() {
        GroupZipLayout layout = layout("davka/", "davka/nk-01/", "davka/nk-01/mets.xml", "davka/nk-02/info.xml",
                "davka/nk-03.ZIP", "davka/seznam.txt");
        assertTrue(layout.isOk());
        assertEquals("davka/", layout.getGroupPrefix());
        assertEquals(Arrays.asList("dir davka/nk-01", "dir davka/nk-02", "zip davka/nk-03.ZIP"), packages(layout));
        assertEquals("nk-02", layout.getPackages().get(1).getName());
        assertEquals(Collections.singletonList("seznam.txt"), layout.getIgnored());
    }

    /** Jediný kořenový adresář bez položky adresáře (jen cesty souborů). */
    @Test
    public void singleRootDirectoryOnlyAsPathPart() {
        GroupZipLayout layout = layout("davka/nk-01/mets.xml");
        assertEquals("davka/", layout.getGroupPrefix());
        assertEquals(Collections.singletonList("dir davka/nk-01"), packages(layout));
    }

    /** Kořenový adresář a vedle něj soubor: úroveň skupiny je kořen archivu, balíkem je jen adresář. */
    @Test
    public void rootDirectoryAndFileMeansZipRoot() {
        GroupZipLayout layout = layout("davka/nk-01/mets.xml", "precti.txt");
        assertEquals("", layout.getGroupPrefix());
        assertEquals(Collections.singletonList("dir davka"), packages(layout));
        assertEquals(Collections.singletonList("precti.txt"), layout.getIgnored());
    }

    /** Jediný soubor ZIP v kořeni je skupina s jedním balíkem (ne kořenový adresář). */
    @Test
    public void singleNestedZipInRoot() {
        GroupZipLayout layout = layout("nk-01.zip");
        assertTrue(layout.isOk());
        assertEquals("", layout.getGroupPrefix());
        assertEquals(Collections.singletonList("zip nk-01.zip"), packages(layout));
    }

    /** Vnořený ZIP ve skupině s kořenovým adresářem a další úroveň v něm se nerozebírá. */
    @Test
    public void nestedZipAndDeeperLevelsAreNotPackages() {
        GroupZipLayout layout = layout("davka/nk-01/podadresar/hluboko.zip", "davka/nk-01/mets.xml", "davka/b.zip");
        assertEquals(Arrays.asList("zip davka/b.zip", "dir davka/nk-01"), packages(layout));
    }

    @Test
    public void namesWithDiacriticsAndSpaces() {
        GroupZipLayout layout = layout("Dávka č. 1/", "Dávka č. 1/Šťastný balík/mets.xml", "Dávka č. 1/žluťoučký kůň.zip",
                "Dávka č. 1/Ábel/info.xml");
        assertEquals("Dávka č. 1/", layout.getGroupPrefix());
        //bez ohledu na velikost pismen: Ábel, Šťastný, žluťoučký (razeni String.CASE_INSENSITIVE_ORDER podle znaku)
        assertEquals(Arrays.asList("dir Dávka č. 1/Ábel", "dir Dávka č. 1/Šťastný balík", "zip Dávka č. 1/žluťoučký kůň.zip"),
                packages(layout));
        assertEquals("Šťastný balík", layout.getPackages().get(1).getName());
    }

    @Test
    public void caseInsensitiveOrder() {
        GroupZipLayout layout = layout("b/x", "A/x", "c.zip", "a2/x");
        assertEquals(Arrays.asList("dir A", "dir a2", "dir b", "zip c.zip"), packages(layout));
    }

    /** Skryté položky (tečka) a metadata Finderu nejsou balíky ani ignorované soubory. */
    @Test
    public void hiddenEntriesAndMacMetadataAreSkipped() {
        GroupZipLayout layout = layout("__MACOSX/", "__MACOSX/davka/._nk-01", "davka/", "davka/.DS_Store",
                "davka/nk-01/mets.xml", "davka/.skryty/mets.xml", ".DS_Store");
        //bez __MACOSX a .DS_Store je v koreni jen adresar davka
        assertEquals("davka/", layout.getGroupPrefix());
        assertEquals(Collections.singletonList("dir davka/nk-01"), packages(layout));
        assertEquals(Collections.<String>emptyList(), layout.getIgnored());
    }

    /** __MACOSX vynechává jen v kořeni; uvnitř skupiny je to obyčejný adresář. */
    @Test
    public void macMetadataOnlyInRoot() {
        GroupZipLayout layout = layout("a/x", "b/__MACOSX/x");
        assertEquals(Arrays.asList("dir a", "dir b"), packages(layout));
        GroupZipLayout nested = layout("davka/__MACOSX/x", "davka/nk-01/x");
        assertEquals(Arrays.asList("dir davka/__MACOSX", "dir davka/nk-01"), packages(nested));
    }

    @Test
    public void emptyZip() {
        assertEquals(GroupZipLayout.Status.EMPTY, GroupZipLayout.of(Collections.<String>emptyList()).getStatus());
        GroupZipLayout onlySlashes = layout("/", "//");
        assertEquals(GroupZipLayout.Status.EMPTY, onlySlashes.getStatus());
        assertFalse(onlySlashes.isOk());
        assertEquals("Soubor ZIP je prázdný.", onlySlashes.errorMessage());
        assertTrue(onlySlashes.getPackages().isEmpty());
    }

    @Test
    public void onlyHiddenEntriesHaveNoPackages() {
        GroupZipLayout layout = layout(".DS_Store", "__MACOSX/x");
        assertEquals(GroupZipLayout.Status.NO_PACKAGES, layout.getStatus());
        assertEquals("Soubor ZIP neobsahuje žádný balík (adresář nebo soubor ZIP).", layout.errorMessage());
    }

    @Test
    public void onlyFilesInRootHaveNoPackages() {
        GroupZipLayout layout = layout("a.txt", "b.pdf");
        assertEquals(GroupZipLayout.Status.NO_PACKAGES, layout.getStatus());
        assertEquals(Arrays.asList("a.txt", "b.pdf"), layout.getIgnored());
        assertTrue(layout.getPackages().isEmpty());
    }

    @Test
    public void rootDirectoryWithOnlyFilesHasNoPackages() {
        GroupZipLayout layout = layout("davka/", "davka/seznam.txt");
        assertEquals(GroupZipLayout.Status.NO_PACKAGES, layout.getStatus());
        assertEquals("davka/", layout.getGroupPrefix());
        assertEquals("Soubor ZIP neobsahuje žádný balík (adresář nebo soubor ZIP) v adresáři davka.", layout.errorMessage());
    }

    /** Soubory XML v kořeni archivu: archiv je jeden balík (jeho podadresáře by CLI validovalo jako balíky). */
    @Test
    public void xmlInZipRootIsSinglePackage() {
        GroupZipLayout layout = layout("mets_nk-01.xml", "info_nk-01.xml", "amdsec/a.xml", "usercopy/u.jp2");
        assertEquals(GroupZipLayout.Status.SINGLE_PACKAGE, layout.getStatus());
        assertEquals("Soubor ZIP obsahuje jeden balík, ne skupinu balíků. Zvolte zdroj „Soubor ZIP“.", layout.errorMessage());
        assertTrue(layout.getPackages().isEmpty());
    }

    /** Jediný kořenový adresář, který je sám balíkem (XML v něm): jeden balík, ne skupina. */
    @Test
    public void singleRootPackageIsSinglePackage() {
        GroupZipLayout layout = layout("nk-01/", "nk-01/METS_NK-01.XML", "nk-01/amdsec/a.xml", "nk-01/txt/t.txt");
        assertEquals(GroupZipLayout.Status.SINGLE_PACKAGE, layout.getStatus());
    }

    /** XML hlouběji než na úrovni skupiny nevadí (je uvnitř balíků). */
    @Test
    public void xmlInsidePackagesIsFine() {
        GroupZipLayout layout = layout("a/mets.xml", "b/info.xml");
        assertTrue(layout.isOk());
    }

    @Test
    public void entryOutsideTarget() {
        GroupZipLayout layout = layout("davka/nk-01/mets.xml", "davka/../../unik.txt");
        assertEquals(GroupZipLayout.Status.ENTRY_OUTSIDE, layout.getStatus());
        assertEquals("davka/../../unik.txt", layout.getProblemEntry());
        assertEquals("Soubor ZIP obsahuje položku mířící mimo cílový adresář (davka/../../unik.txt), nelze ho bezpečně rozbalit.",
                layout.errorMessage());
        assertTrue(layout.getPackages().isEmpty());
        //i s oddelovacem Windows a jako prvni polozka
        assertEquals(GroupZipLayout.Status.ENTRY_OUTSIDE, layout("..\\unik.txt", "a/x").getStatus());
        assertEquals(GroupZipLayout.Status.ENTRY_OUTSIDE, layout("..").getStatus());
    }

    @Test
    public void dotsInNamesAreNotEscape() {
        assertFalse(GroupZipLayout.escapes("a/..b/c"));
        assertFalse(GroupZipLayout.escapes("a/b../c"));
        assertFalse(GroupZipLayout.escapes("a/.../c"));
        assertTrue(GroupZipLayout.escapes("a/../c"));
        assertTrue(GroupZipLayout.escapes("a\\..\\c"));
        //skryty adresar zacinajici "..": neni unik, jen skryta polozka
        GroupZipLayout layout = layout("..skryty/x", "a/x", "b/x");
        assertTrue(layout.isOk());
        assertEquals(Arrays.asList("dir a", "dir b"), packages(layout));
    }

    /** Lomítko na začátku názvu (některé nástroje) se přeskočí jako prázdná část cesty. */
    @Test
    public void leadingSlashIsIgnored() {
        GroupZipLayout layout = layout("/a/x", "/b/y");
        assertEquals(Arrays.asList("dir a", "dir b"), packages(layout));
        GroupZipLayout root = layout("/davka/nk-01/x", "/davka/nk-02/y");
        assertEquals("davka/", root.getGroupPrefix());
        assertEquals(Arrays.asList("dir davka/nk-01", "dir davka/nk-02"), packages(root));
    }

    /** Položky zapsané s „./“ na začátku jsou běžné balíky, ne skryté položky. */
    @Test
    public void leadingDotSlashIsIgnored() {
        GroupZipLayout layout = layout("./a/mets.xml", "./b/mets.xml");
        assertEquals(Arrays.asList("dir a", "dir b"), packages(layout));
        //skutecne skryta polozka se dal vynechava
        GroupZipLayout hidden = layout("./.git/config", "./a/x", "./b/y");
        assertEquals(Arrays.asList("dir a", "dir b"), packages(hidden));
    }
}
