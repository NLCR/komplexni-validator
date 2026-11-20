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

    /** Zrušení až po rozbalení všech souborů: rozbalený obsah nesmí zůstat na disku. */
    @Test
    public void canceledAfterExtractionLeavesNothing() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("balik.zip", "nk-1/", "nk-1/mets.xml", "nk-1/info.xml");
        boolean[] cancel = {false};
        List<String> progress = new ArrayList<>();
        assertNull(new ZipUnpacker(tmpDir).unpack(zip, () -> cancel[0], null, p -> {
            progress.add(p.getFilesDone() + "/" + p.getFilesTotal());
            if (p.getFilesDone() == p.getFilesTotal()) {
                cancel[0] = true;
            }
        }));
        //rozbalilo se vse, zruseni prislo az pak
        assertEquals(Arrays.asList("0/2", "1/2", "2/2"), progress);
        assertArrayEquals(new String[0], tmpDir.list());
    }

    /** Zrušení během rozbalování rozbalování přeruší (další soubory se už nerozbalí) a pracovní adresář smaže. */
    @Test
    public void canceledDuringExtractionStopsAndLeavesNothing() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("skupina.zip", "a/1.txt", "a/2.txt", "b/3.txt", "b/4.txt");
        boolean[] cancel = {false};
        List<String> progress = new ArrayList<>();
        assertNull(new ZipUnpacker(tmpDir).unpack(zip, () -> cancel[0], null, p -> {
            progress.add(p.getFilesDone() + "/" + p.getFilesTotal());
            if (p.getFilesDone() == 1) {
                cancel[0] = true;
            }
        }));
        assertEquals(Arrays.asList("0/4", "1/4"), progress);
        assertArrayEquals(new String[0], tmpDir.list());
    }

    /** Průběh: na začátku 0 z N souborů, pak po každém souboru; adresáře se nepočítají, bajty jsou velikosti souborů. */
    @Test
    public void progressPerFileWithBytes() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        //obsah polozky je jeji nazev: "a/mets.xml" 10, "a/info.xml" 10, "b/x.txt" 7 bajtu
        File zip = zip("skupina.zip", "a/", "a/mets.xml", "a/info.xml", "b/x.txt");
        List<String> progress = new ArrayList<>();
        ZipUnpacker.Result result = new ZipUnpacker(tmpDir).unpack(zip, () -> false, null, p -> progress.add(p.toString()));
        assertEquals(Arrays.asList("0/3 files, 0/27 B", "1/3 files, 10/27 B", "2/3 files, 20/27 B", "3/3 files, 27/27 B"), progress);
        assertEquals(new File(result.getContainerDir(), "skupina"), result.getExtractDir());
        assertTrue(new File(result.getExtractDir(), "a/mets.xml").isFile());
        assertTrue(new File(result.getExtractDir(), "b/x.txt").isFile());
        ZipUnpacker.deleteWorkDir(result.getContainerDir());
    }

    /** Položka mimo cíl se pozná dřív, než se cokoli zapíše (ani platné položky před ní). */
    @Test
    public void entryOutsideTargetIsFoundBeforeWriting() throws Exception {
        File zip = zip("balik.zip", "nk-1/mets.xml", "nk-1/info.xml", "../unik.txt");
        File out = new File(tmp.newFolder("cil"), "rozbaleno");
        try {
            ZipUnpacker.extract(zip, out, () -> false, null);
            fail("položka mimo cílový adresář");
        } catch (IOException e) {
            assertEquals("položka '../unik.txt' míří mimo cílový adresář", e.getMessage());
        }
        assertArrayEquals(new String[0], out.list());
        assertFalse(new File(out.getParentFile(), "unik.txt").exists());
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
        //nazvy, ze kterych by vznikl prazdny nazev, "." nebo "..": rozbalovalo by se mimo pracovni adresar
        assertEquals("rozbaleno", ZipUnpacker.extractDirName(new File("/a/.zip")));
        assertEquals("rozbaleno", ZipUnpacker.extractDirName(new File("/a/..zip")));
        assertEquals("rozbaleno", ZipUnpacker.extractDirName(new File("/a/...zip")));
        assertEquals("...", ZipUnpacker.extractDirName(new File("/a/....zip")));
    }

    /** Archiv pojmenovaný „...zip“ se musí rozbalit do vlastního pracovního adresáře, ne o úroveň výš. */
    @Test
    public void zipNamedWithDotsStaysInsideWorkDir() throws Exception {
        File tmpDir = tmp.newFolder("tmp");
        File zip = zip("...zip", "a/mets.xml", "b/mets.xml");
        ZipUnpacker.Result result = new ZipUnpacker(tmpDir).unpack(zip, () -> false, null);
        File workDir = result.getContainerDir();
        assertEquals(tmpDir, workDir.getParentFile());
        assertTrue(new File(workDir, "rozbaleno/a/mets.xml").isFile());
        assertFalse(new File(tmpDir, "a").exists());
        assertArrayEquals(new String[]{workDir.getName()}, tmpDir.list());
    }
}
