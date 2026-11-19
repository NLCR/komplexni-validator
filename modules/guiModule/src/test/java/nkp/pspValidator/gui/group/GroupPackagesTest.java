package nkp.pspValidator.gui.group;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Balíky ve skupině: které položky adresáře jsou balíky, které se ignorují a v jakém pořadí.
 */
public class GroupPackagesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emptyDirectoryHasNoPackages() throws IOException {
        GroupPackages.Listing listing = GroupPackages.list(tmp.newFolder("prazdna"));
        assertEquals(Collections.emptyList(), listing.getPackages());
        assertEquals(Collections.emptyList(), listing.getIgnored());
    }

    @Test
    public void directoriesAndZipFilesAreSortedIgnoringCase() throws IOException {
        File group = tmp.newFolder("davka");
        new File(group, "b-balik").mkdir();
        new File(group, "A-balik").mkdir();
        new File(group, "c.ZIP").createNewFile();
        new File(group, "d.zip").createNewFile();
        //adresar s priponou .zip je adresar
        new File(group, "e.zip").mkdir();
        new File(group, "poznamky.txt").createNewFile();
        new File(group, "archiv.zip.bak").createNewFile();
        //skryte polozky se vynechavaji uplne
        new File(group, ".DS_Store").createNewFile();
        new File(group, ".git").mkdir();

        GroupPackages.Listing listing = GroupPackages.list(group);
        assertEquals(Arrays.asList("A-balik", "b-balik", "c.ZIP", "d.zip", "e.zip"), names(listing.getPackages()));
        assertEquals(Arrays.asList(false, false, true, true, false), zipFlags(listing.getPackages()));
        assertEquals(Arrays.asList("archiv.zip.bak", "poznamky.txt"), listing.getIgnored());
        assertEquals(group, listing.getGroupDir());
        assertEquals(new File(group, "c.ZIP"), listing.getPackages().get(2).getFile());
    }

    @Test
    public void singlePackage() throws IOException {
        File group = tmp.newFolder("jeden");
        new File(group, "balik").mkdir();
        GroupPackages.Listing listing = GroupPackages.list(group);
        assertEquals(Collections.singletonList("balik"), names(listing.getPackages()));
        assertEquals(Collections.emptyList(), listing.getIgnored());
    }

    @Test
    public void sameNameDifferentCaseKeepsBothInStableOrder() throws IOException {
        File group = tmp.newFolder("velikost");
        new File(group, "a").mkdir();
        new File(group, "B").mkdir();
        new File(group, "b1").mkdir();
        assertEquals(Arrays.asList("a", "B", "b1"), names(GroupPackages.list(group).getPackages()));
    }

    @Test
    public void missingDirectoryOrFileIsError() throws IOException {
        File missing = new File(tmp.getRoot(), "neni");
        try {
            GroupPackages.list(missing);
            fail("adresar neexistuje");
        } catch (IOException e) {
            assertEquals("Nelze číst obsah adresáře " + missing.getPath() + ".", e.getMessage());
        }
        File file = tmp.newFile("soubor.txt");
        try {
            GroupPackages.list(file);
            fail("soubor neni adresar");
        } catch (IOException e) {
            assertEquals("Nelze číst obsah adresáře " + file.getPath() + ".", e.getMessage());
        }
    }

    private static List<String> names(List<GroupPackages.Entry> entries) {
        List<String> result = new ArrayList<>();
        for (GroupPackages.Entry entry : entries) {
            result.add(entry.getName());
        }
        return result;
    }

    private static List<Boolean> zipFlags(List<GroupPackages.Entry> entries) {
        List<Boolean> result = new ArrayList<>();
        for (GroupPackages.Entry entry : entries) {
            result.add(entry.isZip());
        }
        return result;
    }
}
