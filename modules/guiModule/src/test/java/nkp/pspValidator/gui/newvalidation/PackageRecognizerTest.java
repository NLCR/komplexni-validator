package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.Dmf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * Rozpoznání typu a verze balíku v dialogu Nová validace nad testovacími balíky jádra (adresář i ZIP).
 */
public class PackageRecognizerTest {

    private static final File FUND_UNIT = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "fund_unit_0.2/valid_clipping/nk-00027x");
    private static final File MONOGRAPH = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static final File PERIODICAL = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "periodical_1.4/ope301-00000v");
    private static final File WRONG_TYPE = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "monograph_wrongType/b50eb6b0-f0a4-11e3-b72e-005056827e52");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final PackageRecognizer recognizer = new PackageRecognizer();

    @Test
    public void directoryFundUnit() {
        Recognition r = recognizer.recognize(ValidationSource.DIRECTORY, FUND_UNIT.getPath());
        assertEquals(Recognition.Status.RECOGNIZED, r.getStatus());
        assertEquals(Dmf.Type.FUND_UNIT, r.getType());
        assertEquals("0.2", r.getVersionInPackage());
        assertNull(r.getVersionProblem());
    }

    @Test
    public void directoryMonographAndPeriodical() {
        Recognition mon = recognizer.recognize(ValidationSource.DIRECTORY, MONOGRAPH.getPath());
        assertEquals(Dmf.Type.MONOGRAPH, mon.getType());
        assertEquals("1.2", mon.getVersionInPackage());
        //metadataVersion s velkym V
        Recognition per = recognizer.recognize(ValidationSource.DIRECTORY, "  " + PERIODICAL.getPath() + " ");
        assertEquals(Dmf.Type.PERIODICAL, per.getType());
        assertEquals("1.4", per.getVersionInPackage());
    }

    @Test
    public void unknownMetsType() {
        Recognition r = recognizer.recognize(ValidationSource.DIRECTORY, WRONG_TYPE.getPath());
        assertEquals(Recognition.Status.NOT_RECOGNIZED, r.getStatus());
        assertNull(r.getType());
        assertTrue(r.getMessage(), r.getMessage().startsWith("Typ dokumentu nelze zjistit: "));
        assertTrue(r.getMessage(), r.getMessage().contains("Book"));
    }

    @Test
    public void emptyDirectoryIsNotPackage() throws IOException {
        Recognition r = recognizer.recognize(ValidationSource.DIRECTORY, tmp.newFolder("prazdny").getPath());
        assertEquals(Recognition.Status.NOT_RECOGNIZED, r.getStatus());
        assertTrue(r.getMessage(), r.getMessage().contains("nebyl nalezen soubor PRIMARY-METS"));
    }

    @Test
    public void twoMetsFiles() throws IOException {
        File dir = copyOf(FUND_UNIT, "dva-mets");
        Files.copy(new File(dir, "mets_nk-00027x.xml").toPath(), new File(dir, "mets_kopie.xml").toPath());
        Recognition r = recognizer.recognize(ValidationSource.DIRECTORY, dir.getPath());
        assertEquals(Recognition.Status.NOT_RECOGNIZED, r.getStatus());
        assertTrue(r.getMessage(), r.getMessage().contains("nalezeno více možných souborů PRIMARY-METS"));
    }

    @Test
    public void versionMissingInInfo() throws IOException {
        File dir = copyOf(FUND_UNIT, "bez-verze");
        File info = new File(dir, "info_nk-00027x.xml");
        String content = new String(Files.readAllBytes(info.toPath()), StandardCharsets.UTF_8);
        String without = content.replaceAll("<metadataversion>[^<]*</metadataversion>", "");
        assertNotEquals(content, without);
        Files.write(info.toPath(), without.getBytes(StandardCharsets.UTF_8));
        Recognition r = recognizer.recognize(ValidationSource.DIRECTORY, dir.getPath());
        assertEquals(Recognition.Status.RECOGNIZED, r.getStatus());
        assertEquals(Dmf.Type.FUND_UNIT, r.getType());
        assertNull(r.getVersionInPackage());
        assertNull(r.getVersionProblem());
    }

    @Test
    public void infoFileMissing() throws IOException {
        File dir = copyOf(FUND_UNIT, "bez-info");
        assertTrue(new File(dir, "info_nk-00027x.xml").delete());
        Recognition r = recognizer.recognize(ValidationSource.DIRECTORY, dir.getPath());
        assertEquals(Recognition.Status.RECOGNIZED, r.getStatus());
        assertNull(r.getVersionInPackage());
        assertTrue(r.getVersionProblem(), r.getVersionProblem().contains("nenalezen soubor INFO"));
    }

    @Test
    public void invalidPaths() throws IOException {
        assertEquals(Recognition.Status.INVALID_PATH, recognizer.recognize(ValidationSource.DIRECTORY, "").getStatus());
        assertEquals(Recognition.Status.INVALID_PATH, recognizer.recognize(ValidationSource.DIRECTORY, null).getStatus());
        Recognition missing = recognizer.recognize(ValidationSource.DIRECTORY, new File(tmp.getRoot(), "neni").getPath());
        assertEquals(Recognition.Status.INVALID_PATH, missing.getStatus());
        assertTrue(missing.getMessage(), missing.getMessage().contains("neexistuje"));
        File file = tmp.newFile("soubor.zip");
        assertEquals(Recognition.Status.INVALID_PATH, recognizer.recognize(ValidationSource.DIRECTORY, file.getPath()).getStatus());
        assertEquals(Recognition.Status.INVALID_PATH, recognizer.recognize(ValidationSource.ZIP, FUND_UNIT.getPath()).getStatus());
    }

    @Test
    public void zipWithPackageDirectory() throws IOException {
        File zip = zip(FUND_UNIT, "nk-00027x/", "s-adresarem.zip");
        Recognition r = recognizer.recognize(ValidationSource.ZIP, zip.getPath());
        assertEquals(Recognition.Status.RECOGNIZED, r.getStatus());
        assertEquals(Dmf.Type.FUND_UNIT, r.getType());
        assertEquals("0.2", r.getVersionInPackage());
    }

    @Test
    public void zipWithPackageInRoot() throws IOException {
        File zip = zip(MONOGRAPH, "", "v-koreni.zip");
        Recognition r = recognizer.recognize(ValidationSource.ZIP, zip.getPath());
        assertEquals(Recognition.Status.RECOGNIZED, r.getStatus());
        assertEquals(Dmf.Type.MONOGRAPH, r.getType());
        assertEquals("1.2", r.getVersionInPackage());
    }

    @Test
    public void zipWithoutPackage() throws IOException {
        File zip = zip(new File(FUND_UNIT, "alto"), "obal/alto/", "bez-balíku.zip");
        Recognition r = recognizer.recognize(ValidationSource.ZIP, zip.getPath());
        assertEquals(Recognition.Status.NOT_RECOGNIZED, r.getStatus());
        assertTrue(r.getMessage(), r.getMessage().contains("nebyl nalezen soubor PRIMARY-METS"));
        //cesta k docasnemu adresari se do hlasky nedostane
        assertFalse(r.getMessage(), r.getMessage().contains("kv-rozpoznani"));
    }

    @Test
    public void notAZip() throws IOException {
        File file = tmp.newFile("nezip.zip");
        Files.write(file.toPath(), "toto neni zip".getBytes(StandardCharsets.UTF_8));
        Recognition r = recognizer.recognize(ValidationSource.ZIP, file.getPath());
        assertEquals(Recognition.Status.NOT_RECOGNIZED, r.getStatus());
        assertTrue(r.getMessage(), r.getMessage().startsWith("Soubor není platný ZIP"));
    }

    @Test
    public void zipWithSingleFile() throws IOException {
        File file = tmp.newFile("jeden-soubor.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            out.putNextEntry(new ZipEntry("jen-soubor.txt"));
            out.write(1);
            out.closeEntry();
        }
        Recognition r = recognizer.recognize(ValidationSource.ZIP, file.getPath());
        assertEquals(Recognition.Status.NOT_RECOGNIZED, r.getStatus());
        assertTrue(r.getMessage(), r.getMessage().contains("jen soubor jen-soubor.txt"));
    }

    private File copyOf(File packageDir, String name) throws IOException {
        File target = tmp.newFolder(name);
        File[] files = packageDir.listFiles(File::isFile);
        assertNotNull(files);
        for (File file : files) {
            Files.copy(file.toPath(), new File(target, file.getName()).toPath());
        }
        return target;
    }

    /** ZIP ze souborů v kořeni adresáře (podadresáře balíku nejsou pro rozpoznání potřeba, přidá se jeden kvůli struktuře). */
    private File zip(File dir, String prefix, String name) throws IOException {
        File zip = new File(tmp.getRoot(), name);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            if (!prefix.isEmpty()) {
                out.putNextEntry(new ZipEntry(prefix));
                out.closeEntry();
            }
            File[] files = dir.listFiles(File::isFile);
            assertNotNull(files);
            for (File file : files) {
                out.putNextEntry(new ZipEntry(prefix + file.getName()));
                out.write(Files.readAllBytes(file.toPath()));
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry(prefix + "txt/neco.txt"));
            out.write("x".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }
}
