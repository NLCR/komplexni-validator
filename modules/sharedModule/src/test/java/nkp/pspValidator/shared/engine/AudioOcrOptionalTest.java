package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * OCR (adresare txt a alto) u zvuku: v DMF Zvuk 1.0 (kap. 2 body 8 a 9, tabulka kap. 5, kap. 5.7, 5.8 a 7.9), ve
 * fonovaleccich a v gramodeskach 0.4 a 0.5 (tabulka kap. 5 „NE“, kap. 5.7 a 5.8 „Tvoreni je nepovinne“) nepovinne,
 * v gramodeskach 0.3 povinne (DMF Zvuk 0.3, tabulka kap. 5 „ANO“). Kdyz adresar existuje, plati pro nej vsechna
 * pravidla. Behem se overuji jen fDMF audio_gram (0.3, 0.4, 0.5, 1.0); u audio_fono_0.3, audio_fono_1.0, audio_disc_1.0
 * a audio_no_carrier_1.0 je konfigurace OCR shodna s audio_gram 1.0 (stejne tri zmeny ve variables.xml a rules.xml),
 * samostatny balicek pro ne neni.
 * <p>
 * Zaklad je balicek src/test/resources/audio_gram_0.3/1234567890 (kopie do docasneho adresare); pro fDMF 1.0 se
 * prejmenuji adresare na nazvy podle DMF 1.0 (amdsec, mastercopy, usercopy, usercopy_audio). Ostatni chyby balicku
 * (prazdne soubory, checksumy) testy neresi, kontroluji jen pravidla OCR.
 */
public class AudioOcrOptionalTest {

    private static final File SOURCE = new File("src/test/resources/audio_gram_0.3/1234567890");
    private static final Dmf AUDIO_GRAM_1_0 = new Dmf(Dmf.Type.AUDIO_GRAM, "1.0");
    private static final Dmf AUDIO_GRAM_0_3 = new Dmf(Dmf.Type.AUDIO_GRAM, "0.3");
    private static final Dmf AUDIO_GRAM_0_4 = new Dmf(Dmf.Type.AUDIO_GRAM, "0.4");
    private static final Dmf AUDIO_GRAM_0_5 = new Dmf(Dmf.Type.AUDIO_GRAM, "0.5");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File copyPackage(boolean dirNames10) throws IOException {
        File target = new File(tmp.getRoot(), SOURCE.getName());
        Path src = SOURCE.toPath();
        try (Stream<Path> paths = Files.walk(src)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                Path t = target.toPath().resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(t);
                } else {
                    Files.copy(p, t, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        if (dirNames10) {
            rename(target, "amd_sec", "amdsec");
            rename(target, "master_copy", "mastercopy");
            rename(target, "user_copy", "usercopy");
            rename(target, "user_copy_audio", "usercopy_audio");
        }
        return target;
    }

    private static void rename(File root, String from, String to) throws IOException {
        Files.move(new File(root, from).toPath(), new File(root, to).toPath());
    }

    private static void delete(File root, String dir) throws IOException {
        try (Stream<Path> paths = Files.walk(new File(root, dir).toPath())) {
            for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }

    /**
     * Ve vedlejsich METS obrazu zmeni formatName PREMIS Objectu ALTO, takze zaznam pro ALTO neexistuje.
     */
    private static void removeAltoPremis(File root) throws IOException {
        File[] amdMets = new File(root, "amdsec").listFiles((d, n) -> n.matches("amd_mets_1234567890_[0-9]+\\.xml"));
        assertNotNull(amdMets);
        assertTrue(amdMets.length > 0);
        for (File f : amdMets) {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String changed = s.replace("<premis:formatName>text/xml</premis:formatName>", "<premis:formatName>application/x-test</premis:formatName>");
            assertNotEquals(s, changed);
            Files.write(f.toPath(), changed.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Ve vedlejsich METS obrazu odstrani cely techMD OBJ_003 (gramodesky 0.4 a 0.5 vybiraji zaznam ALTO podle ID,
     * ne podle formatName).
     */
    private static void removeAltoTechMd(File root) throws IOException {
        File[] amdMets = new File(root, "amdsec").listFiles((d, n) -> n.matches("amd_mets_1234567890_[0-9]+\\.xml"));
        assertNotNull(amdMets);
        assertTrue(amdMets.length > 0);
        for (File f : amdMets) {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String changed = s.replaceAll("(?s)<mets:techMD ID=\"OBJ_003\">.*?</mets:techMD>", "");
            assertNotEquals(s, changed);
            Files.write(f.toPath(), changed.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String run(Dmf dmf, File psp) throws Exception {
        String log = FdmfTestSupport.run(dmf, psp);
        for (String line : log.split("\n")) {
            boolean crash = line.contains("nečekaná chyba")
                    || line.contains("nesplněn kontrakt");
            assertFalse("funkce spadla nebo konfigurace obsahuje chybu: " + line.trim(), crash);
        }
        return log;
    }

    private static void assertNoProblems(String log, String rule) {
        assertTrue("v logu chybi pravidlo " + rule, log.contains("Pravidlo " + rule + ":"));
        List<String> problems = RepoPackagesSupport.problemsOfRule(log, rule);
        assertTrue("pravidlo " + rule + " hlasi: " + problems, problems.isEmpty());
    }

    private static void assertProblem(String log, String rule, String fragment) {
        List<String> problems = RepoPackagesSupport.problemsOfRule(log, rule);
        for (String problem : problems) {
            if (problem.contains(fragment)) {
                return;
            }
        }
        fail("pravidlo " + rule + " nehlasi '" + fragment + "', hlasi: " + problems);
    }

    @Test
    public void audio10WithoutOcrDirs() throws Exception {
        File psp = copyPackage(true);
        delete(psp, "alto");
        delete(psp, "txt");
        String log = run(AUDIO_GRAM_1_0, psp);
        assertNoProblems(log, "SINGLE_OCR-TEXT-DIR_FOUND");
        assertNoProblems(log, "SINGLE_OCR-ALTO-DIR_FOUND");
        assertNoProblems(log, "OCR-TEXT-DIR_IS_DIR");
        assertNoProblems(log, "OCR-ALTO-DIR_IS_DIR");
        assertNoProblems(log, "NUMBER_OF_FILES_IN_IMAGE-DIRS_MATCH");
        assertNoProblems(log, "OCR-ALTO_FILES_VALID_BY_XSD");
        assertNoProblems(log, "OCR-ALTO_FILES_SAME_VERSION");
        assertNoProblems(log, "PREMIS_OBJECT_FOR_ALTO_OCR_MATCHES_PROFILE");
    }

    /**
     * Bez ALTO neni PREMIS Object pro ALTO povinny.
     */
    @Test
    public void audio10WithoutOcrDirsAndWithoutAltoPremis() throws Exception {
        File psp = copyPackage(true);
        delete(psp, "alto");
        delete(psp, "txt");
        removeAltoPremis(psp);
        String log = run(AUDIO_GRAM_1_0, psp);
        assertNoProblems(log, "PREMIS_OBJECT_FOR_ALTO_OCR_MATCHES_PROFILE");
    }

    /**
     * S ALTO je PREMIS Object pro ALTO povinny jako drive.
     */
    @Test
    public void audio10WithAltoRequiresAltoPremis() throws Exception {
        File psp = copyPackage(true);
        removeAltoPremis(psp);
        String log = run(AUDIO_GRAM_1_0, psp);
        assertProblem(log, "PREMIS_OBJECT_FOR_ALTO_OCR_MATCHES_PROFILE", "ERROR: v mets:amdSec nenalezen ani jeden element");
    }

    @Test
    public void audio10ExistingOcrDirIsChecked() throws Exception {
        File psp = copyPackage(true);
        Files.write(new File(psp, "alto/poznamka.txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
        delete(psp, "txt");
        Files.write(new File(psp, "txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
        String log = run(AUDIO_GRAM_1_0, psp);
        assertNoProblems(log, "SINGLE_OCR-ALTO-DIR_FOUND");
        assertProblem(log, "NO_EXTRA_FILES_IN_OCR-ALTO-DIR", "ERROR: nalezen nečekaný soubor: poznamka.txt");
        assertProblem(log, "OCR-TEXT-DIR_IS_DIR", "ERROR:");
    }

    @Test
    public void audio10MissingAltoFileIsCounted() throws Exception {
        File psp = copyPackage(true);
        Files.delete(new File(psp, "alto/alto_1234567890_0002.xml").toPath());
        String log = run(AUDIO_GRAM_1_0, psp);
        assertProblem(log, "NUMBER_OF_FILES_IN_IMAGE-DIRS_MATCH", "WARNING: nalezeny různé velikosti seznamů souborů");
    }

    /**
     * Gramodesky 0.4 a 0.5: OCR nepovinne (DMF Zvuk 0.4 a 0.5, tabulka kap. 5: alto a txt „NE“, kap. 5.7 a 5.8
     * „Tvoreni je nepovinne“). Nazvy adresaru jsou stejne jako v DMF 1.0.
     */
    @Test
    public void audio04And05WithoutOcrDirs() throws Exception {
        for (Dmf dmf : new Dmf[]{AUDIO_GRAM_0_4, AUDIO_GRAM_0_5}) {
            File psp = copyPackage(true);
            delete(psp, "alto");
            delete(psp, "txt");
            removeAltoTechMd(psp);
            String log = run(dmf, psp);
            assertNoProblems(log, "SINGLE_OCR-TEXT-DIR_FOUND");
            assertNoProblems(log, "SINGLE_OCR-ALTO-DIR_FOUND");
            assertNoProblems(log, "OCR-TEXT-DIR_IS_DIR");
            assertNoProblems(log, "OCR-ALTO-DIR_IS_DIR");
            assertNoProblems(log, "NUMBER_OF_FILES_IN_IMAGE-DIRS_MATCH");
            assertNoProblems(log, "PREMIS_OBJECT_FOR_ALTO_OCR_MATCHES_PROFILE");
            delete(psp, "");
        }
    }

    /**
     * Gramodesky 0.4 a 0.5: existujici adresar OCR se kontroluje jako drive.
     */
    @Test
    public void audio04And05ExistingOcrDirIsChecked() throws Exception {
        for (Dmf dmf : new Dmf[]{AUDIO_GRAM_0_4, AUDIO_GRAM_0_5}) {
            File psp = copyPackage(true);
            Files.write(new File(psp, "alto/poznamka.txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
            removeAltoTechMd(psp);
            String log = run(dmf, psp);
            assertProblem(log, "NO_EXTRA_FILES_IN_OCR-ALTO-DIR", "ERROR: nalezen nečekaný soubor: poznamka.txt");
            assertProblem(log, "PREMIS_OBJECT_FOR_ALTO_OCR_MATCHES_PROFILE", "ERROR: v mets:amdSec nenalezen ani jeden element");
            delete(psp, "");
        }
    }

    /**
     * Gramodesky 0.3: OCR povinne (DMF Zvuk 0.3, tabulka kap. 5: alto a txt „ANO“).
     */
    @Test
    public void audio03WithoutOcrDirsIsError() throws Exception {
        File psp = copyPackage(false);
        delete(psp, "alto");
        delete(psp, "txt");
        String log = run(AUDIO_GRAM_0_3, psp);
        assertProblem(log, "SINGLE_OCR-TEXT-DIR_FOUND", "ERROR: seznam obsahuje 0 souborů namísto očekávaných 1");
        assertProblem(log, "SINGLE_OCR-ALTO-DIR_FOUND", "ERROR: seznam obsahuje 0 souborů namísto očekávaných 1");
    }
}
