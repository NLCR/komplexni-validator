package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.gui.newvalidation.Recognition;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * Rozpoznání skupiny v dialogu Nová validace a texty karty rozpoznání a souhrnů sekcí (nad kopiemi testovacích balíků
 * jádra). Očekávané texty jsou napsané ručně podle zadání („12 balíků: 9× Monografie 2.2, …, 1 nerozpoznán“).
 */
public class GroupRecognitionTest {

    private static final File FUND_UNIT = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "fund_unit_0.2/valid_clipping/nk-00027x");
    private static final File MONOGRAPH = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static final File WRONG_TYPE = new File(GuiTestSupport.SHARED_TEST_RESOURCES, "monograph_wrongType/b50eb6b0-f0a4-11e3-b72e-005056827e52");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;
    private File group;

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("cfg"));
        registry = GuiTestSupport.registry();
        //5 balíků: 2× jednotky fondu (adresář a ZIP), monografie, nerozpoznaný typ, prázdný adresář; 1 ignorovaný soubor
        group = tmp.newFolder("davka");
        copyFiles(FUND_UNIT, new File(group, "a-fond"));
        zip(FUND_UNIT, "nk-00027x/", new File(group, "b-fond.zip"));
        copyFiles(MONOGRAPH, new File(group, "c-mono"));
        copyFiles(WRONG_TYPE, new File(group, "d-spatny-typ"));
        new File(group, "e-prazdny").mkdir();
        Files.write(new File(group, "poznamky.txt").toPath(), "x".getBytes());
    }

    private VersionSettings versions() {
        return new VersionSettings(config, registry);
    }

    @Test
    public void wholeGroup() {
        List<String> progress = new ArrayList<>();
        GroupRecognition result = GroupRecognition.recognize(group, 200, () -> false, (done, total) -> progress.add(done + "/" + total));
        assertTrue(result.isOk());
        assertEquals(5, result.getPackageCount());
        assertEquals(5, result.getRecognitions().size());
        assertEquals(0, result.getNotRecognizedDueToLimit());
        assertEquals(Arrays.asList("0/5", "1/5", "2/5", "3/5", "4/5"), progress);
        assertEquals("Nalezeno 5 balíků", GroupSummaries.title(result));
        assertEquals("2× Jednotky fondu 0.2, 1× Monografie 1.2, 2 nerozpoznány", GroupSummaries.breakdown(result, versions()));
        assertEquals("Každý balík se validuje podle svého typu a verze, postupně jeden po druhém. Ignorováno: 1 soubor, "
                + "který není ZIP (poznamky.txt).", GroupSummaries.detail(result));
        assertEquals(Arrays.asList(new Dmf(Dmf.Type.MONOGRAPH, "1.2"), new Dmf(Dmf.Type.FUND_UNIT, "0.2")),
                new ArrayList<>(GroupSummaries.effectiveDmfs(result, versions())));
    }

    @Test
    public void forcedVersionChangesBreakdown() {
        VersionSettings versions = versions();
        versions.setForcedVersion(Dmf.Type.MONOGRAPH, "2.2");
        GroupRecognition result = GroupRecognition.recognize(group, 200, () -> false, null);
        assertEquals("2× Jednotky fondu 0.2, 1× Monografie 2.2, 2 nerozpoznány", GroupSummaries.breakdown(result, versions));
        assertEquals("vynuceno u 1 typu", GroupSummaries.versionsSummary(versions));
    }

    @Test
    public void limitRecognizesOnlyFirstPackages() {
        List<String> progress = new ArrayList<>();
        GroupRecognition result = GroupRecognition.recognize(group, 2, () -> false, (done, total) -> progress.add(done + "/" + total));
        assertEquals(5, result.getPackageCount());
        assertEquals(2, result.getRecognitions().size());
        assertEquals(3, result.getNotRecognizedDueToLimit());
        assertEquals(Arrays.asList("0/2", "1/2"), progress);
        assertEquals("Nalezeno 5 balíků", GroupSummaries.title(result));
        assertEquals("2× Jednotky fondu 0.2", GroupSummaries.breakdown(result, versions()));
        assertEquals("Každý balík se validuje podle svého typu a verze, postupně jeden po druhém. Ignorováno: 1 soubor, "
                + "který není ZIP (poznamky.txt). Rozpoznáno prvních 2 z 5, ostatní se rozpoznají až při validaci.", GroupSummaries.detail(result));
    }

    @Test
    public void canceledRecognitionReturnsNull() {
        int[] calls = {0};
        //zrusi se po prvnim balíku
        GroupRecognition result = GroupRecognition.recognize(group, 200, () -> calls[0]++ >= 1, null);
        assertNull(result);
    }

    @Test
    public void unreadableDirectory() {
        File missing = new File(tmp.getRoot(), "neni");
        GroupRecognition result = GroupRecognition.recognize(missing, 200, () -> false, null);
        assertFalse(result.isOk());
        assertEquals(0, result.getPackageCount());
        assertEquals("Adresář nelze přečíst", GroupSummaries.title(result));
        assertEquals("Nelze číst obsah adresáře " + missing.getPath() + ".", GroupSummaries.detail(result));
        assertNull(GroupSummaries.breakdown(result, versions()));
        assertTrue(GroupSummaries.effectiveDmfs(result, versions()).isEmpty());
    }

    @Test
    public void emptyGroup() throws IOException {
        GroupRecognition result = GroupRecognition.recognize(tmp.newFolder("prazdna"), 200, () -> false, null);
        assertTrue(result.isOk());
        assertEquals("Adresář neobsahuje žádný balík", GroupSummaries.title(result));
        assertNull(GroupSummaries.breakdown(result, versions()));
        assertEquals("Balíkem je každý podadresář a soubor ZIP přímo v adresáři.", GroupSummaries.detail(result));
    }

    @Test
    public void versionProblemIsShownSeparately() throws IOException {
        File other = tmp.newFolder("bez-info");
        File dir = new File(other, "fond-bez-info");
        copyFiles(FUND_UNIT, dir);
        assertTrue(new File(dir, "info_nk-00027x.xml").delete());
        GroupRecognition result = GroupRecognition.recognize(other, 200, () -> false, null);
        assertEquals("1× Jednotky fondu (verzi nelze zjistit)", GroupSummaries.breakdown(result, versions()));
        //bez verze se fDMF neurci, takze ani nastroje a preskocene sekce
        assertTrue(GroupSummaries.effectiveDmfs(result, versions()).isEmpty());
        assertEquals("Nalezen 1 balík", GroupSummaries.title(result));
    }

    // ===== skupina v souboru ZIP =====

    /** Skupina v ZIPu s balíky v kořeni: stejné rozpoznání jako adresář, vnořený ZIP se rozpozná až při validaci. */
    @Test
    public void zipGroupWithPackagesInRoot() throws IOException {
        File zip = new File(tmp.newFolder("zipy"), "davka.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            addFiles(out, FUND_UNIT, "a-fond/");
            addFiles(out, MONOGRAPH, "c-mono/");
            addFiles(out, WRONG_TYPE, "d-spatny-typ/");
            addEntry(out, "e-prazdny/", null);
            addEntry(out, "b-fond.zip", "nezkoumá se".getBytes());
            addEntry(out, "poznamky.txt", "x".getBytes());
        }
        List<String> progress = new ArrayList<>();
        GroupRecognition result = GroupRecognition.recognize(zip, 200, () -> false, (done, total) -> progress.add(done + "/" + total));
        assertTrue(result.isOk());
        assertTrue(result.isZip());
        assertEquals(zip, result.getGroupDir());
        assertEquals(5, result.getPackageCount());
        assertEquals(Arrays.asList("0/5", "1/5", "2/5", "3/5", "4/5"), progress);
        assertEquals("Nalezeno 5 balíků", GroupSummaries.title(result));
        //a-fond, b-fond.zip (pri validaci), c-mono, d-spatny-typ (nerozpoznan), e-prazdny (nerozpoznan);
        //pri stejnem poctu poradi typu ve vyctu (monografie pred jednotkami fondu)
        assertEquals("1× Monografie 1.2, 1× Jednotky fondu 0.2, 2 nerozpoznány, 1 se rozpozná při validaci",
                GroupSummaries.breakdown(result, versions()));
        assertEquals("Každý balík se validuje podle svého typu a verze, postupně jeden po druhém. Soubor ZIP se před "
                + "validací celý rozbalí do dočasného adresáře. Ignorováno: 1 soubor, který není ZIP (poznamky.txt).",
                GroupSummaries.detail(result));
        Recognition nested = result.getRecognitions().get(1);
        assertTrue(nested.isDeferred());
        assertFalse(nested.isRecognized());
        assertEquals("Balík v souboru ZIP uvnitř skupiny se rozpozná až při validaci.", nested.getMessage());
        assertFalse(result.getRecognitions().get(0).isDeferred());
        //cesty v dialogu: soubor ZIP a balik v nem
        assertEquals(new File(zip, "a-fond").getAbsolutePath(), result.getListing().getPackages().get(0).getDisplayPath());
        assertEquals(Arrays.asList(new Dmf(Dmf.Type.MONOGRAPH, "1.2"), new Dmf(Dmf.Type.FUND_UNIT, "0.2")),
                new ArrayList<>(GroupSummaries.effectiveDmfs(result, versions())));
        //docasne adresare rozpoznani se smazaly
        assertNoRecognitionTmpDirsLeft();
    }

    /** Jediný kořenový adresář s balíky (s diakritikou a mezerou v názvu). */
    @Test
    public void zipGroupWithSingleRootDirectory() throws IOException {
        File zip = new File(tmp.newFolder("zipy"), "Dávka 1.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            addEntry(out, "Dávka 1/", null);
            addFiles(out, MONOGRAPH, "Dávka 1/Monografie č. 1/");
            addFiles(out, FUND_UNIT, "Dávka 1/fond/");
        }
        GroupRecognition result = GroupRecognition.recognize(zip, 200, () -> false, null);
        assertTrue(result.isOk());
        assertEquals(2, result.getPackageCount());
        assertEquals("1× Monografie 1.2, 1× Jednotky fondu 0.2", GroupSummaries.breakdown(result, versions()));
        assertEquals(new File(zip, "Dávka 1/Monografie č. 1").getAbsolutePath(), result.getListing().getPackages().get(1).getDisplayPath());
    }

    @Test
    public void zipGroupLimit() throws IOException {
        File zip = new File(tmp.newFolder("zipy"), "davka.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            addFiles(out, MONOGRAPH, "a/");
            addFiles(out, MONOGRAPH, "b/");
            addFiles(out, MONOGRAPH, "c/");
        }
        GroupRecognition result = GroupRecognition.recognize(zip, 2, () -> false, null);
        assertEquals(3, result.getPackageCount());
        assertEquals(2, result.getRecognitions().size());
        assertEquals(1, result.getNotRecognizedDueToLimit());
        assertEquals("2× Monografie 1.2", GroupSummaries.breakdown(result, versions()));
    }

    @Test
    public void zipGroupCanceled() throws IOException {
        File zip = new File(tmp.newFolder("zipy"), "davka.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            addFiles(out, MONOGRAPH, "a/");
            addFiles(out, MONOGRAPH, "b/");
        }
        int[] calls = {0};
        assertNull(GroupRecognition.recognize(zip, 200, () -> calls[0]++ >= 1, null));
    }

    @Test
    public void zipGroupErrors() throws IOException {
        File dir = tmp.newFolder("chyby");
        File notZip = new File(dir, "nezip.zip");
        Files.write(notZip.toPath(), "toto neni zip".getBytes());
        GroupRecognition result = GroupRecognition.recognize(notZip, 200, () -> false, null);
        assertFalse(result.isOk());
        assertTrue(result.isZip());
        assertEquals("Soubor ZIP nelze použít jako skupinu", GroupSummaries.title(result));
        assertTrue(result.getError(), result.getError().startsWith("Soubor nezip.zip není platný ZIP ("));
        assertEquals(result.getError(), GroupSummaries.detail(result));
        assertNull(GroupSummaries.breakdown(result, versions()));
        assertEquals(0, result.getPackageCount());

        File empty = new File(dir, "prazdny.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(empty))) {
            //bez polozek ZipOutputStream zapise jen konec centralniho adresare
            out.finish();
        }
        assertEquals("Soubor ZIP je prázdný.", GroupRecognition.recognize(empty, 200, () -> false, null).getError());

        File noPackages = new File(dir, "bez-baliku.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(noPackages))) {
            addEntry(out, "a.txt", "x".getBytes());
        }
        assertEquals("Soubor ZIP neobsahuje žádný balík (adresář nebo soubor ZIP).",
                GroupRecognition.recognize(noPackages, 200, () -> false, null).getError());

        File single = new File(dir, "jeden.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(single))) {
            addFiles(out, MONOGRAPH, "b50eb6b0/");
        }
        assertEquals("Soubor ZIP obsahuje jeden balík, ne skupinu balíků. Zvolte zdroj „Soubor ZIP“.",
                GroupRecognition.recognize(single, 200, () -> false, null).getError());

        File outside = new File(dir, "unik.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outside))) {
            addFiles(out, MONOGRAPH, "a/");
            addEntry(out, "../unik.txt", "x".getBytes());
        }
        assertEquals("Soubor ZIP obsahuje položku mířící mimo cílový adresář (../unik.txt), nelze ho bezpečně rozbalit.",
                GroupRecognition.recognize(outside, 200, () -> false, null).getError());
    }

    private static void addFiles(ZipOutputStream out, File packageDir, String prefix) throws IOException {
        addEntry(out, prefix, null);
        File[] files = packageDir.listFiles(File::isFile);
        assertNotNull(files);
        for (File file : files) {
            addEntry(out, prefix + file.getName(), Files.readAllBytes(file.toPath()));
        }
    }

    private static void addEntry(ZipOutputStream out, String name, byte[] content) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        if (content != null) {
            out.write(content);
        }
        out.closeEntry();
    }

    private static void assertNoRecognitionTmpDirsLeft() {
        //PackageRecognizer vytvari kv-rozpoznani* v java.io.tmpdir a hned je maze; zadny nesmi byt mladsi nez test
        File[] left = new File(System.getProperty("java.io.tmpdir")).listFiles((d, n) -> n.startsWith("kv-rozpoznani")
                && new File(d, n).lastModified() > System.currentTimeMillis() - 60_000);
        assertNotNull(left);
        assertEquals(Arrays.toString(left), 0, left.length);
    }

    // ===== texty bez cteni disku =====

    @Test
    public void titlePlurals() {
        assertEquals("Nalezen 1 balík", GroupSummaries.title(fake(1, 0)));
        assertEquals("Nalezeny 2 balíky", GroupSummaries.title(fake(2, 0)));
        assertEquals("Nalezeny 4 balíky", GroupSummaries.title(fake(4, 0)));
        assertEquals("Nalezeno 5 balíků", GroupSummaries.title(fake(5, 0)));
    }

    @Test
    public void breakdownOrderAndPlurals() {
        //3× periodika 2.1, 3× monografie 2.2 (shoda poctu: monografie je ve vycetu typu drive), 1× monografie 2.3
        //(stejny pocet jako 2.0 téhož typu: novejsi verze drive), 1× monografie 2.0, 5 nerozpoznanych
        List<Recognition> recognitions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            recognitions.add(Recognition.recognized(Dmf.Type.PERIODICAL, "2.1", null));
            recognitions.add(Recognition.recognized(Dmf.Type.MONOGRAPH, "2.2", null));
        }
        recognitions.add(Recognition.recognized(Dmf.Type.MONOGRAPH, "2.0", null));
        recognitions.add(Recognition.recognized(Dmf.Type.MONOGRAPH, "2.3", null));
        for (int i = 0; i < 5; i++) {
            recognitions.add(Recognition.notRecognized("není balík"));
        }
        GroupRecognition result = GroupRecognition.of(listing(recognitions.size(), 0), recognitions);
        assertEquals("3× Monografie 2.2, 3× Periodika 2.1, 1× Monografie 2.3, 1× Monografie 2.0, 5 nerozpoznáno",
                GroupSummaries.breakdown(result, versions()));
        assertEquals("1 nerozpoznán", GroupSummaries.breakdown(GroupRecognition.of(listing(1, 0),
                Collections.singletonList(Recognition.notRecognized("x"))), versions()));
    }

    @Test
    public void ignoredFilesPlural() {
        assertEquals("Každý balík se validuje podle svého typu a verze, postupně jeden po druhém. Ignorováno: 3 soubory, "
                + "které nejsou ZIP (s0.txt, …).", GroupSummaries.detail(fake(1, 3)));
        assertEquals("Každý balík se validuje podle svého typu a verze, postupně jeden po druhém. Ignorováno: 5 souborů, "
                + "které nejsou ZIP (s0.txt, …).", GroupSummaries.detail(fake(1, 5)));
    }

    @Test
    public void deferredPlurals() {
        List<Recognition> recognitions = new ArrayList<>();
        recognitions.add(Recognition.recognized(Dmf.Type.MONOGRAPH, "2.2", null));
        for (int i = 0; i < 2; i++) {
            recognitions.add(Recognition.deferred("x"));
        }
        assertEquals("1× Monografie 2.2, 2 se rozpoznají při validaci",
                GroupSummaries.breakdown(GroupRecognition.of(listing(3, 0), recognitions), versions()));
        for (int i = 0; i < 3; i++) {
            recognitions.add(Recognition.deferred("x"));
        }
        assertEquals("1× Monografie 2.2, 5 se rozpozná při validaci",
                GroupSummaries.breakdown(GroupRecognition.of(listing(6, 0), recognitions), versions()));
        assertEquals("1 se rozpozná při validaci", GroupSummaries.breakdown(GroupRecognition.of(listing(1, 0),
                Collections.singletonList(Recognition.deferred("x"))), versions()));
    }

    @Test
    public void recognizingTitle() {
        assertEquals("Hledám balíky…", GroupSummaries.recognizingTitle(0, 0));
        assertEquals("Rozpoznávám balíky… 3 z 12", GroupSummaries.recognizingTitle(3, 12));
    }

    @Test
    public void versionsSummary() {
        VersionSettings versions = versions();
        assertEquals("podle balíků, nic není vynuceno", GroupSummaries.versionsSummary(versions));
        versions.setForcedVersion(Dmf.Type.MONOGRAPH, "2.2");
        versions.setForcedVersion(Dmf.Type.PERIODICAL, "2.1");
        assertEquals("vynuceno u 2 typů", GroupSummaries.versionsSummary(versions));
    }

    @Test
    public void skippedSummary() {
        assertEquals("určí se podle typů a verzí balíků", GroupSummaries.skippedSummary(null));
        assertEquals("určí se podle typů a verzí balíků", GroupSummaries.skippedSummary(Collections.<Dmf, List<String>>emptyMap()));
        Map<Dmf, List<String>> map = new LinkedHashMap<>();
        map.put(new Dmf(Dmf.Type.MONOGRAPH, "2.2"), Collections.<String>emptyList());
        //fDMF neni k dispozici
        map.put(new Dmf(Dmf.Type.PERIODICAL, "9.9"), null);
        assertEquals("žádná", GroupSummaries.skippedSummary(map));
        map.put(new Dmf(Dmf.Type.FUND_UNIT, "0.2"), Arrays.asList("Soubor info", "Identifikátory"));
        assertEquals("2 celé sekce (Jednotky fondu 0.2)", GroupSummaries.skippedSummary(map));
        map.put(new Dmf(Dmf.Type.MONOGRAPH, "2.2"), Arrays.asList("A", "B", "C"));
        assertEquals("5 celých sekcí (2 verze DMF)", GroupSummaries.skippedSummary(map));
        Map<Dmf, List<String>> one = new LinkedHashMap<>();
        one.put(new Dmf(Dmf.Type.MONOGRAPH, "2.2"), Collections.singletonList("A"));
        assertEquals("1 celá sekce (Monografie 2.2)", GroupSummaries.skippedSummary(one));
    }

    // ===== pomocne =====

    private GroupRecognition fake(int packages, int ignored) {
        List<Recognition> recognitions = new ArrayList<>();
        for (int i = 0; i < packages; i++) {
            recognitions.add(Recognition.recognized(Dmf.Type.MONOGRAPH, "2.2", null));
        }
        return GroupRecognition.of(listing(packages, ignored), recognitions);
    }

    private GroupPackages.Listing listing(int packages, int ignored) {
        List<GroupPackages.Entry> entries = new ArrayList<>();
        for (int i = 0; i < packages; i++) {
            entries.add(new GroupPackages.Entry(new File(tmp.getRoot(), "b" + i), false));
        }
        List<String> ignoredNames = new ArrayList<>();
        for (int i = 0; i < ignored; i++) {
            ignoredNames.add("s" + i + ".txt");
        }
        return new GroupPackages.Listing(tmp.getRoot(), entries, ignoredNames);
    }

    private static void copyFiles(File packageDir, File target) throws IOException {
        assertTrue(target.mkdirs());
        File[] files = packageDir.listFiles(File::isFile);
        assertNotNull(files);
        for (File file : files) {
            Files.copy(file.toPath(), new File(target, file.getName()).toPath());
        }
    }

    private static void zip(File dir, String prefix, File zip) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry(prefix));
            out.closeEntry();
            File[] files = dir.listFiles(File::isFile);
            assertNotNull(files);
            for (File file : files) {
                out.putNextEntry(new ZipEntry(prefix + file.getName()));
                out.write(Files.readAllBytes(file.toPath()));
                out.closeEntry();
            }
        }
    }
}
