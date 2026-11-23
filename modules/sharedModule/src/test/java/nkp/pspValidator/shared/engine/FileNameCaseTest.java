package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfConfiguration;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import nkp.pspValidator.shared.ValidatorFactory;
import nkp.pspValidator.shared.engine.exceptions.HashComputationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Velikost pismen v odkazech na soubory (METS FLocat, polozky INFO, radky souboru CHECKSUM): odkaz se musi shodovat
 * se skutecnym nazvem souboru vcetne velikosti pismen na vsech systemech. Windows a macOS velikost pismen v nazvech
 * nerozlisuji, Linux ano; driv proto stejny balik dal na Linuxu chyby a na Windows a macOS ne.
 * <p>
 * Testy bezi na macOS (system souboru velikost pismen nerozlisuje, soubor s odkazem v jine velikosti pismen tam
 * "existuje") i na Linuxu (nerozlisuje, soubor "neexistuje") a ocekavane hlasky jsou na obou stejne. Ocekavani jsou
 * urcena rucne z upravy balicku: kazdy odkaz s jinou velikosti pismen da v kazdem pravidle, ktere ho kontroluje,
 * stejnou chybu jako odkaz na neexistujici soubor na Linuxu, s doplnenym skutecnym nazvem.
 * <p>
 * Zaklad: monograph_1.3.2/b50eb6b0 (validni: 0 ERROR, 5 WARNING) zkopirovany do docasneho adresare a upraveny tak,
 * aby kontrolni soucty upravenych souboru (fileSec primarniho METS, soubor CHECKSUM, INFO) sedely; jinak by pribyly
 * chyby nesouvisejici s velikosti pismen.
 */
public class FileNameCaseTest {

    private static final File MON_132 = new File("src/test/resources/monograph_1.3.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static final Dmf DMF_MON_132 = new Dmf(Dmf.Type.MONOGRAPH, "1.3.2");
    private static final String ID = "b50eb6b0-f0a4-11e3-b72e-005056827e52";
    private static final String MC = "mc_" + ID + "_0001.jp2";
    private static final String MC_UPPER = "MC_" + ID + "_0001.jp2";
    private static final String AMD = "amd_mets_" + ID + "_0001.xml";
    private static final String METS = "mets_" + ID + ".xml";
    private static final String CHECKSUM = "md5_" + ID + ".md5";
    private static final String INFO = "info_" + ID + ".xml";
    private static final Set<String> EXCLUDED_RULES = Collections.singleton("MODS_IDENTIFIERS_URN_NBN_REGISTERED");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /**
     * Neupraveny balicek: 0 ERROR, 5 WARNING (jako RepoPackagesSmokeTest), zadna hlaska o velikosti pismen.
     */
    @Test
    public void unchangedPackage() throws Exception {
        File psp = copyPackage();
        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        assertArrayEquals(new int[]{0, 5}, RepoPackagesSupport.countErrorsAndWarnings(log, EXCLUDED_RULES));
        assertFalse(log, log.contains("velikostí písmen"));
    }

    /**
     * Odkaz na mastercopy/mc_…_0001.jp2 zapsany jako masterCopy/MC_…_0001.jp2 (adresar i nazev souboru) ve vsech
     * metadatech: fileSec primarniho METS, sekundarni METS strany 1, soubor CHECKSUM a itemlist v INFO. Soubor na disku
     * ma spravny nazev.
     */
    @Test
    public void referenceToMasterCopyDiffersInCase() throws Exception {
        File psp = copyPackage();
        String ref = "masterCopy/" + MC_UPPER;
        replaceInFiles(psp, new String[][]{
                {METS, "xlink:href=\"mastercopy/" + MC + "\"", "xlink:href=\"" + ref + "\""},
                {"amdsec/" + AMD, "xlink:href=\"mastercopy/" + MC + "\"", "xlink:href=\"" + ref + "\""},
                {CHECKSUM, " /mastercopy/" + MC, " /masterCopy/" + MC_UPPER},
                {INFO, "<item>\\mastercopy\\" + MC + "</item>", "<item>\\masterCopy\\" + MC_UPPER + "</item>"},
        });
        //na macOS a Windows soubor pod odkazem "existuje", na Linuxu ne; vysledek validace na tom zaviset nesmi
        System.out.println("FileNameCaseTest: system souboru " + (new File(psp, ref).exists() ? "nerozlisuje" : "rozlisuje") + " velikost pismen");

        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        String actualAbs = new File(psp, "mastercopy/" + MC).getAbsolutePath();
        String refAbs = new File(psp, "masterCopy/" + MC_UPPER).getAbsolutePath();
        String caseMsg = "soubor masterCopy/" + MC_UPPER + " nenalezen; v balíku je soubor mastercopy/" + MC + ", jehož název se liší velikostí písmen";

        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("CHECKSUM_FILE_ALL_PATHS_MATCH_FILES", Arrays.asList(
                "ERROR: nenalezen záznam pro soubor: '" + actualAbs + "'; odkaz v metadatech (" + refAbs + ") se liší velikostí písmen",
                "ERROR: nalezený soubor nebyl očekáván: '" + refAbs + "'; v balíku je soubor " + actualAbs + ", jehož název se liší velikostí písmen"));
        expected.put("CHECKSUM_FILE_ALL_CHECKSUMS_MATCH", Collections.singletonList(
                "ERROR: chyba výpočtu kontrolního součtu pro soubor /masterCopy/" + MC_UPPER + ": " + caseMsg));
        expected.put("INFO_ITEMLIST_REFERENCES_ALL_FILES", Arrays.asList(
                "ERROR: soubor INFO se odkazuje na neočekávaný soubor: " + refAbs + "; v balíku je soubor " + actualAbs + ", jehož název se liší velikostí písmen",
                "ERROR: soubor INFO se neodkazuje na očekávaný soubor: " + actualAbs + "; odkaz v metadatech (" + refAbs + ") se liší velikostí písmen"));
        expected.put("PRIMARY-METS_FILESEC_ALL_MASTER_COPY_FILES_REFERENCED", Arrays.asList(
                "ERROR: nalezen neočekávaný soubor " + refAbs + "; v balíku je soubor " + actualAbs + ", jehož název se liší velikostí písmen",
                "ERROR: nenalezen očekávaný soubor " + actualAbs + "; odkaz v metadatech (" + refAbs + ") se liší velikostí písmen"));
        expected.put("PRIMARY-METS_FILESEC_CHECKSUMS_MATCH", Collections.singletonList("ERROR: " + caseMsg));
        expected.put("PRIMARY-METS_FILESEC_SIZES_MATCH", Collections.singletonList("WARNING: " + caseMsg));
        expected.put("SECONDARY-METS_FILESEC_MASTER_COPY_FILE_REFERENCED", Collections.singletonList(
                "ERROR: fileSec/fileGrp s ID 'MC_IMGGRP': odkazovaný soubor (" + ref + ") není mezi očekávanými soubory (sekundární mets amdsec/" + AMD + "); v balíku je soubor mastercopy/" + MC + ", jehož název se liší velikostí písmen"));

        assertRuleProblems(log, expected);
        //nic jineho nepribylo: 9 ERROR, 5 + 1 WARNING
        assertArrayEquals(new int[]{9, 6}, RepoPackagesSupport.countErrorsAndWarnings(log, EXCLUDED_RULES));
    }

    /**
     * Odkazy v INFO na primarni METS (mainmets) a na soubor CHECKSUM (checksum) s jinou velikosti pismen. Porovnani
     * nazvu primarniho METS rozlisovalo velikost pismen vsude uz driv, cesta k souboru CHECKSUM jen na macOS a Linuxu
     * (File.equals na Windows ne); hlaska nove uvede, ze jde jen o velikost pismen.
     */
    @Test
    public void infoReferencesDifferInCase() throws Exception {
        File psp = copyPackage();
        replaceInFiles(psp, new String[][]{
                {INFO, "<mainmets>" + METS + "</mainmets>", "<mainmets>METS_" + ID + ".xml</mainmets>"},
                {INFO, ">\\" + CHECKSUM + "</checksum>", ">\\MD5_" + ID + ".md5</checksum>"},
        });
        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("INFO_REFERENCES_PRIMARY-METS", Collections.singletonList(
                "ERROR: nalezený název souboru PRIMARY-METS (METS_" + ID + ".xml) se neshoduje se skutečným názvem (" + METS + "); názvy se liší jen velikostí písmen"));
        expected.put("INFO_CHECKSUM_FILE_CHECKSUM_MATCHES", Collections.singletonList(
                "ERROR: element checksum obsahuje cestu ke špatnému souboru: namísto (" + new File(psp, CHECKSUM).getAbsolutePath()
                        + ") obsahuje (" + new File(psp, "MD5_" + ID + ".md5").getAbsolutePath() + "); názvy se liší jen velikostí písmen"));
        assertRuleProblems(log, expected);
        assertArrayEquals(new int[]{2, 5}, RepoPackagesSupport.countErrorsAndWarnings(log, EXCLUDED_RULES));
    }

    /**
     * Odkaz, ktery se lisi jinak nez velikosti pismen, dava stejne hlasky jako driv (bez doplnku o velikosti pismen).
     */
    @Test
    public void referenceToMissingFileHasNoCaseHint() throws Exception {
        File psp = copyPackage();
        String ref = "mastercopy/mc_" + ID + "_0009.jp2";
        replaceInFiles(psp, new String[][]{
                {"amdsec/" + AMD, "xlink:href=\"mastercopy/" + MC + "\"", "xlink:href=\"" + ref + "\""},
        });
        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("SECONDARY-METS_FILESEC_MASTER_COPY_FILE_REFERENCED", Collections.singletonList(
                "ERROR: fileSec/fileGrp s ID 'MC_IMGGRP': odkazovaný soubor (" + ref + ") není mezi očekávanými soubory (sekundární mets amdsec/" + AMD + ")"));
        assertRuleProblems(log, expected);
        assertArrayEquals(new int[]{1, 5}, RepoPackagesSupport.countErrorsAndWarnings(log, EXCLUDED_RULES));
        assertFalse(log, log.contains("velikostí písmen"));
    }

    /**
     * Engine necte soubor balíku pod nazvem, ktery se od skutecneho lisi velikosti pismen (XML i MD5), na zadnem
     * systemu; soubor se spravnym nazvem precte. Pouziva se pro soubory, jejichz nazev pochazi z metadat
     * (napr. sekundarni METS v composed_perio_1.3 podle fileSec primarniho METS).
     */
    @Test
    public void engineDoesNotReadFileUnderNameDifferingInCase() throws Exception {
        File psp = tmp.newFolder("balik");
        assertTrue(new File(psp, "amdsec").mkdir());
        File actual = new File(psp, "amdsec/amd_0001.xml");
        Files.write(actual.toPath(), "<a/>".getBytes(StandardCharsets.UTF_8));
        Engine engine = new Engine(null);
        engine.setProvidedFile("PSP_DIR", psp);

        File wrong = new File(psp, "AMDSEC/amd_0001.XML");
        String caseMsg = "soubor AMDSEC/amd_0001.XML nenalezen; v balíku je soubor amdsec/amd_0001.xml, jehož název se liší velikostí písmen";
        try {
            engine.getXmlDocument(wrong, true);
            fail("soubor s jinou velikosti pismen se nema precist");
        } catch (XmlFileParsingException e) {
            assertEquals("chyba čtení v souboru " + wrong.getAbsolutePath() + ": " + caseMsg, e.getMessage());
        }
        try {
            engine.computeMd5(wrong);
            fail("soubor s jinou velikosti pismen se nema precist");
        } catch (HashComputationException e) {
            assertEquals(caseMsg, e.getMessage());
            assertEquals(wrong, e.getFile());
        }
        assertEquals(0, engine.getMd5ComputationCount());

        assertEquals("a", engine.getXmlDocument(actual, true).getDocumentElement().getNodeName());
        assertEquals(DigestUtils.md5Hex("<a/>"), engine.computeMd5(actual));
    }

    /**
     * Soubor mimo adresar balíku (konfigurace validatoru, XSD) se nekontroluje.
     */
    @Test
    public void engineDoesNotCheckFilesOutsidePackage() throws Exception {
        File psp = tmp.newFolder("balik");
        File outside = tmp.newFile("Mimo.xml");
        Files.write(outside.toPath(), "<b/>".getBytes(StandardCharsets.UTF_8));
        Engine engine = new Engine(null);
        engine.setProvidedFile("PSP_DIR", psp);
        assertEquals("b", engine.getXmlDocument(outside, true).getDocumentElement().getNodeName());
        assertEquals(0, engine.getPackageFileLookup().getListingsLoaded());
        //bez PSP_DIR se take nekontroluje
        Engine noPsp = new Engine(null);
        assertNull(noPsp.lookupPackageFile(outside));
        assertEquals(DigestUtils.md5Hex("<b/>"), noPsp.computeMd5(outside));
    }

    /**
     * Vypisy adresaru se pamatuji jen v ramci jednoho behu validace: Validator.run je na zacatku zahodi, aby se projevily
     * zmeny souboru od predchoziho dotazu. (CLI i GUI stavi pro kazdou validaci novy validator, vysledky pravidel se
     * v ramci validatoru pamatuji; uz pri sestaveni validatoru se ctou nektere promenne, napr. PSP_ID z INFO.)
     * Overeno poctem vypisu: validator, ktery pred behem vypsal adresar mastercopy, ho pri behu vypise znovu, takze
     * vypisu je o jeden vic nez u validatoru bez tohoto dotazu.
     */
    @Test
    public void directoryListingsAreClearedAtStartOfRun() throws Exception {
        File psp = copyPackage();
        Validator fresh = buildValidator(psp);
        int listingsBeforeRun = fresh.getEngine().getPackageFileLookup().getListingsLoaded();
        run(fresh, psp);
        int listingsTotal = fresh.getEngine().getPackageFileLookup().getListingsLoaded();
        assertTrue("vypisu adresaru: " + listingsTotal, listingsTotal > listingsBeforeRun);

        Validator prepared = buildValidator(psp);
        assertEquals(listingsBeforeRun, prepared.getEngine().getPackageFileLookup().getListingsLoaded());
        assertEquals(nkp.pspValidator.shared.engine.utils.PackageFileLookup.Status.FOUND_EXACT, prepared.getEngine().lookupPackageFile(new File(psp, "mastercopy/" + MC)).getStatus());
        assertEquals(listingsBeforeRun + 1, prepared.getEngine().getPackageFileLookup().getListingsLoaded());
        run(prepared, psp);
        assertEquals(listingsTotal + 1, prepared.getEngine().getPackageFileLookup().getListingsLoaded());
    }

    /**
     * Soubor CHECKSUM má pro tentýž soubor dva řádky: správný a druhý s jinou velikostí písmen. Druhý řádek je záznam
     * navíc a nesmí se ztratit (na Windows by ho množina souborů sloučila s prvním, File.equals tam velikost písmen
     * nerozlišuje; na macOS a Linuxu test prochází i bez té opravy).
     */
    @Test
    public void checksumFileWithSecondRecordDifferingInCase() throws Exception {
        File psp = copyPackage();
        File checksum = new File(psp, CHECKSUM);
        String content = read(checksum);
        String separator = content.contains("\r\n") ? "\r\n" : "\n";
        write(checksum, (content.endsWith(separator) ? content : content + separator)
                + md5(new File(psp, "mastercopy/" + MC)) + " /masterCopy/" + MC_UPPER + separator);
        replaceAll(new File(psp, INFO), md5Original(CHECKSUM), md5(checksum));

        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        String refAbs = new File(psp, "masterCopy/" + MC_UPPER).getAbsolutePath();
        Map<String, List<String>> expected = new LinkedHashMap<>();
        //spravny zaznam existuje, proto zadne "nenalezen záznam" a zadny doplnek
        expected.put("CHECKSUM_FILE_ALL_PATHS_MATCH_FILES", Collections.singletonList(
                "ERROR: nalezený soubor nebyl očekáván: '" + refAbs + "'"));
        expected.put("CHECKSUM_FILE_ALL_CHECKSUMS_MATCH", Collections.singletonList(
                "ERROR: chyba výpočtu kontrolního součtu pro soubor /masterCopy/" + MC_UPPER + ": soubor masterCopy/" + MC_UPPER
                        + " nenalezen; v balíku je soubor mastercopy/" + MC + ", jehož název se liší velikostí písmen"));
        assertRuleProblems(log, expected);
    }

    /**
     * Odkaz ve vedlejším METS vede na soubor přes symbolický odkaz na adresář (mastercopx -> mastercopy). Názvy v odkazu
     * přesně odpovídají názvům v balíku a cíl je očekávaný soubor, pravidlo proto chybu nehlásí (jako před zpřísněním
     * velikosti písmen). Tentýž odkaz s jinou velikostí písmen chybou je.
     */
    @Test
    public void referenceThroughSymbolicLink() throws Exception {
        File psp = copyPackage();
        try {
            Files.createSymbolicLink(new File(psp, "mastercopx").toPath(), new File("mastercopy").toPath());
        } catch (IOException | UnsupportedOperationException e) {
            Assume.assumeNoException("symbolický odkaz nelze vytvořit", e);
        }
        replaceInFiles(psp, new String[][]{
                {"amdsec/" + AMD, "xlink:href=\"mastercopy/" + MC + "\"", "xlink:href=\"mastercopx/" + MC + "\""},
        });
        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        assertEquals(Collections.emptyList(), RepoPackagesSupport.problemsOfRule(log, "SECONDARY-METS_FILESEC_MASTER_COPY_FILE_REFERENCED"));
    }

    @Test
    public void referenceThroughSymbolicLinkDifferingInCase() throws Exception {
        File psp = copyPackage();
        try {
            Files.createSymbolicLink(new File(psp, "mastercopx").toPath(), new File("mastercopy").toPath());
        } catch (IOException | UnsupportedOperationException e) {
            Assume.assumeNoException("symbolický odkaz nelze vytvořit", e);
        }
        replaceInFiles(psp, new String[][]{
                {"amdsec/" + AMD, "xlink:href=\"mastercopy/" + MC + "\"", "xlink:href=\"masterCopx/" + MC + "\""},
        });
        String log = FdmfTestSupport.run(DMF_MON_132, psp);
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("SECONDARY-METS_FILESEC_MASTER_COPY_FILE_REFERENCED", Collections.singletonList(
                "ERROR: fileSec/fileGrp s ID 'MC_IMGGRP': odkazovaný soubor (masterCopx/" + MC + ") není mezi očekávanými soubory (sekundární mets amdsec/"
                        + AMD + "); v balíku je soubor mastercopx/" + MC + ", jehož název se liší velikostí písmen"));
        assertRuleProblems(log, expected);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static void assertRuleProblems(String log, Map<String, List<String>> expected) {
        for (Map.Entry<String, List<String>> rule : expected.entrySet()) {
            List<String> problems = RepoPackagesSupport.problemsOfRule(log, rule.getKey());
            assertEquals("pravidlo " + rule.getKey() + ": " + problems, rule.getValue().size(), problems.size());
            for (int i = 0; i < problems.size(); i++) {
                String problem = problems.get(i);
                String exp = rule.getValue().get(i);
                //text protokolu muze na konec pripojit " (soubor: …)"
                assertTrue("pravidlo " + rule.getKey() + "\nocekavano: " + exp + "\nje:        " + problem,
                        problem.equals(exp) || problem.startsWith(exp + " (soubor: "));
            }
        }
    }

    private File copyPackage() throws IOException {
        File target = new File(tmp.newFolder("psp"), MON_132.getName());
        Path src = MON_132.toPath();
        try (Stream<Path> paths = Files.walk(src)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                Path t = target.toPath().resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(t);
                } else {
                    Files.copy(p, t);
                }
            }
        }
        return target;
    }

    /**
     * Nahradi texty v souborech balíku ({soubor, puvodni, novy}; puvodni text musi byt v souboru prave jednou) a opravi
     * kontrolni soucty zmenenych souboru: atribut CHECKSUM ve fileSec primarniho METS a radek v souboru CHECKSUM (stary
     * soucet se nahradi novym), soucet souboru CHECKSUM v INFO. Upravy nemeni delku souboru (jen velikost pismen),
     * atributy SIZE proto plati dal.
     */
    private static void replaceInFiles(File psp, String[][] replacements) throws IOException {
        Map<String, String> oldMd5 = new LinkedHashMap<>();
        for (String[] r : replacements) {
            File f = new File(psp, r[0]);
            oldMd5.putIfAbsent(r[0], md5(f));
            String content = read(f);
            int first = content.indexOf(r[1]);
            assertTrue("v " + r[0] + " neni '" + r[1] + "'", first >= 0);
            assertEquals("v " + r[0] + " je '" + r[1] + "' vickrat", first, content.lastIndexOf(r[1]));
            assertEquals("uprava ma zachovat delku", r[1].length(), r[2].length());
            write(f, content.replace(r[1], r[2]));
        }
        //soucty sekundarnich METS a dalsich souboru jsou ve fileSec primarniho METS a v souboru CHECKSUM
        for (Map.Entry<String, String> e : oldMd5.entrySet()) {
            if (e.getKey().equals(INFO) || e.getKey().equals(CHECKSUM) || e.getKey().equals(METS)) {
                continue;
            }
            String newMd5 = md5(new File(psp, e.getKey()));
            replaceAll(new File(psp, METS), e.getValue(), newMd5);
            replaceAll(new File(psp, CHECKSUM), e.getValue(), newMd5);
        }
        //primarni METS je v souboru CHECKSUM (ve fileSec sam sebe nema)
        File mets = new File(psp, METS);
        String metsMd5Original = md5Original(METS);
        replaceAll(new File(psp, CHECKSUM), metsMd5Original, md5(mets));
        //soubor CHECKSUM ma soucet v INFO
        replaceAll(new File(psp, INFO), md5Original(CHECKSUM), md5(new File(psp, CHECKSUM)));
    }

    private static String md5Original(String name) throws IOException {
        return md5(new File(MON_132, name));
    }

    private static void replaceAll(File f, String from, String to) throws IOException {
        if (from.equals(to)) {
            return;
        }
        String content = read(f);
        assertTrue("v " + f.getName() + " neni soucet " + from, content.contains(from));
        write(f, content.replace(from, to));
    }

    private static String md5(File f) throws IOException {
        return DigestUtils.md5Hex(Files.readAllBytes(f.toPath()));
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static void write(File f, String content) throws IOException {
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static Validator buildValidator(File psp) throws Exception {
        ValidatorConfigurationManager configManager = FdmfTestSupport.sharedConfigManager();
        FdmfConfiguration fdmfConfig = FdmfTestSupport.sharedRegistry().getFdmfConfig(DMF_MON_132);
        ExternalUtilManager utilManager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            utilManager.setUtilDisabled(util);
        }
        fdmfConfig.initBinaryFileProfiles(utilManager);
        return ValidatorFactory.buildValidator(fdmfConfig, psp, configManager.getDictionaryManager());
    }

    private static void run(Validator validator, File psp) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, "UTF-8");
        validator.run(psp, null, out, 1, null, null, null, null, DMF_MON_132);
    }
}
