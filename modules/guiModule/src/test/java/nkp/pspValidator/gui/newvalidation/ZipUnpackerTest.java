package nkp.pspValidator.gui.newvalidation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * Rozbalení ZIPu před validací (dialog jednoho balíku i běh skupiny): adresář balíku, zbytek z minula, zrušení, chyby.
 */
public class ZipUnpackerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File zip(String name, String... entries) throws IOException {
        File zip = new File(tmp.newFolder(), name);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            for (String entry : entries) {
                out.putNextEntry(new ZipEntry(entry));
                if (!entry.endsWith("/")) {
                    out.write(entry.getBytes(StandardCharsets.UTF_8));
                }
                out.closeEntry();
            }
        }
        return zip;
    }

    @Test
    public void singleDirectoryInZipIsPackage() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml", "nk-1/txt/a.txt");
        List<String> messages = new ArrayList<>();
        ZipUnpacker.Result result = new ZipUnpacker(tmpDir).unpack(zip, () -> false, messages::add);
        //novy pracovni adresar primo v adresari pro docasne soubory, v nem <nazev ZIPu>/<jedina polozka>
        File workDir = result.getContainerDir();
        assertEquals(tmpDir, workDir.getParentFile());
        assertTrue(workDir.getName(), workDir.getName().startsWith("kv-rozbaleno-"));
        assertEquals(new File(new File(workDir, "balik"), "nk-1"), result.getPspDir());
        assertTrue(new File(result.getPspDir(), "txt/a.txt").isFile());
        assertEquals(Arrays.asList(
                "Zpracovávám soubor " + zip.getAbsolutePath(),
                "Kontroluji soubor " + zip.getAbsolutePath(),
                "Kontroluji adresář " + tmpDir.getAbsolutePath(),
                "Rozbaluji " + zip.getAbsolutePath() + " do adresáře " + new File(workDir, "balik").getAbsolutePath()), messages);
    }

    @Test
    public void filesInRootMeanContainerIsPackage() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("V-KORENI.ZIP", "mets.xml", "info.xml");
        ZipUnpacker.Result result = new ZipUnpacker(tmpDir).unpack(zip, () -> false, null);
        assertEquals(new File(result.getContainerDir(), "V-KORENI"), result.getPspDir());
        assertTrue(new File(result.getPspDir(), "info.xml").isFile());
    }

    /**
     * Existující adresář se stejným názvem, jako má ZIP, se nesmí smazat ani změnit: může to být cizí adresář nebo
     * sama validovaná skupina (skupina "balik" obsahující "balik.zip" v adresáři pro dočasné soubory).
     */
    @Test
    public void existingDirectoryOfTheSameNameIsNotTouched() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File existing = new File(tmpDir, "balik/stary/soubor.txt");
        assertTrue(existing.getParentFile().mkdirs());
        Files.write(existing.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml");
        List<String> messages = new ArrayList<>();
        ZipUnpacker.Result result = new ZipUnpacker(tmpDir).unpack(zip, () -> false, messages::add);
        assertTrue(existing.isFile());
        assertEquals("x", new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8));
        assertFalse(new File(tmpDir, "balik/nk-1").exists());
        assertTrue(new File(result.getPspDir(), "mets.xml").isFile());
        for (String message : messages) {
            assertFalse(message, message.startsWith("Mažu"));
        }
    }

    @Test
    public void twoUnpacksOfTheSameZipDoNotShareDirectory() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml");
        ZipUnpacker unpacker = new ZipUnpacker(tmpDir);
        ZipUnpacker.Result first = unpacker.unpack(zip, () -> false, null);
        ZipUnpacker.Result second = unpacker.unpack(zip, () -> false, null);
        assertNotEquals(first.getContainerDir(), second.getContainerDir());
        assertTrue(new File(first.getPspDir(), "mets.xml").isFile());
        assertTrue(new File(second.getPspDir(), "mets.xml").isFile());
    }

    @Test
    public void deleteWorkDirRemovesOnlyOwnDirectories() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml");
        ZipUnpacker.Result result = new ZipUnpacker(tmpDir).unpack(zip, () -> false, null);
        File foreign = new File(tmpDir, "cizi");
        assertTrue(foreign.mkdirs());
        ZipUnpacker.deleteWorkDir(foreign);
        assertTrue("cizí adresář se nesmí smazat", foreign.isDirectory());
        ZipUnpacker.deleteWorkDir(result.getContainerDir());
        assertFalse(result.getContainerDir().exists());
        assertArrayEquals(new String[]{"cizi"}, tmpDir.list());
    }

    /** Zrušení až po rozbalení: rozbalený obsah nesmí zůstat na disku. */
    @Test
    public void canceledAfterExtractionLeavesNothing() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml");
        //dotazy na zruseni: pred kontrolou ZIPu, po ni, pred rozbalenim (ne), po rozbaleni (ano)
        int[] calls = {0};
        assertNull(new ZipUnpacker(tmpDir).unpack(zip, () -> ++calls[0] >= 4, null));
        assertEquals(4, calls[0]);
        assertArrayEquals(new String[0], tmpDir.list());
    }

    /** Položka ZIPu s cestou mimo cílový adresář: chyba, nic se nezapíše mimo a nic nezůstane. */
    @Test
    public void entryOutsideTargetIsRejected() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/mets.xml", "../../unik.txt");
        try {
            new ZipUnpacker(tmpDir).unpack(zip, () -> false, null);
            fail("položka mimo cílový adresář");
        } catch (ZipUnpacker.UnpackException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("míří mimo cílový adresář"));
        }
        assertFalse(new File(tmpDir, "unik.txt").exists());
        assertFalse(new File(tmpDir.getParentFile(), "unik.txt").exists());
        assertArrayEquals(new String[0], tmpDir.list());
    }

    @Test
    public void canceledBeforeStartCreatesNothing() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml");
        assertNull(new ZipUnpacker(tmpDir).unpack(zip, () -> true, null));
        String[] left = tmpDir.list();
        assertNotNull(left);
        assertEquals(0, left.length);
    }

    @Test
    public void notAZip() throws Exception {
        File file = new File(tmp.newFolder(), "nezip.zip");
        Files.write(file.toPath(), "toto neni zip".getBytes(StandardCharsets.UTF_8));
        try {
            new ZipUnpacker(tmp.newFolder("tmp")).unpack(file, () -> false, null);
            fail("neni ZIP");
        } catch (ZipUnpacker.UnpackException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Chyba: Soubor " + file.getAbsolutePath() + " není korektní ZIP: "));
        }
    }

    @Test
    public void missingTmpDir() throws Exception {
        File zip = zip("balik.zip", "nk-1/");
        File missing = new File(tmp.getRoot(), "neni");
        try {
            new ZipUnpacker(missing).unpack(zip, () -> false, null);
            fail("adresar pro docasne soubory neexistuje");
        } catch (ZipUnpacker.UnpackException e) {
            assertEquals("Chyba: adresář " + missing.getAbsolutePath() + " neexistuje!", e.getMessage());
        }
        File file = tmp.newFile("soubor");
        try {
            new ZipUnpacker(file).unpack(zip, () -> false, null);
            fail("adresar pro docasne soubory je soubor");
        } catch (ZipUnpacker.UnpackException e) {
            assertEquals("Chyba: soubor " + file.getAbsolutePath() + " není adresář!", e.getMessage());
        }
    }

    @Test
    public void extractDirName() {
        assertEquals("balik", ZipUnpacker.extractDirName(new File("/a/balik.zip")));
        assertEquals("balik", ZipUnpacker.extractDirName(new File("/a/balik.Zip")));
        assertEquals("balik.tar", ZipUnpacker.extractDirName(new File("/a/balik.tar")));
    }
}
