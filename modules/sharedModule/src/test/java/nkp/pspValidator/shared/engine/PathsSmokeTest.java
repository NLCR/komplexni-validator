package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Cesty s mezerami a diakritikou (ukol z .local/doc/testy/plan-a-postup.md, 2. cast).
 * <p>
 * V src/test/resources/monograph_1.2 jsou dve kopie zmenseneho balicku b50eb6b0 (obsah shodny s empty_images/b50eb6b0...,
 * ~620 kB): v adresari "path containing spaces" a "path_containing_diacritics_áéíýóůúě_čďňřšťž". Test je validuje
 * (DMF monograph 1.2) a porovnava pocty ERROR a WARNING s toutez kopii v obycejne ceste (TemporaryFolder, adresar
 * "plain_path"). Nazev kopie nesouhlasi s PSP-ID, takze pravidlo PSP_DIR_MATCHES_PSP_ID hlasi ERROR ve vsech variantach
 * stejne; tim se porovnani nerozbije. Tezi fixuje, ze zpracovani cest (soubory, XSD, odkazy z METS) nezavisi na
 * mezerach a diakritice.
 */
public class PathsSmokeTest {

    private static final String SPACES = "src/test/resources/monograph_1.2/path containing spaces";
    private static final String DIACRITICS = "src/test/resources/monograph_1.2/path_containing_diacritics_áéíýóůúě_čďňřšťž";
    private static final Set<String> EXCLUDED_RULES = Collections.singleton("MODS_IDENTIFIERS_URN_NBN_REGISTERED");
    private static final Dmf DMF = new Dmf(Dmf.Type.MONOGRAPH, "1.2");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String validate(File dir) throws Exception {
        assertTrue("balicek neexistuje: " + dir, dir.isDirectory());
        String log = FdmfTestSupport.run(DMF, dir);
        for (String line : log.split("\n")) {
            boolean crash = (line.contains("nečekaná chyba") && !line.contains("EmptyParamEvaluationException"))
                    || line.contains("nesplněn kontrakt");
            assertFalse("funkce spadla nebo konfigurace obsahuje chybu: " + line.trim(), crash);
        }
        return log;
    }

    private int[] plainPathCounts() throws Exception {
        File plain = new File(tmp.newFolder("obycejna"), "plain_path");
        copyDir(new File(SPACES).toPath(), plain.toPath());
        String log = validate(plain);
        int[] counts = RepoPackagesSupport.countErrorsAndWarnings(log, EXCLUDED_RULES);
        assertTrue("kontrola: kopie v obycejne ceste ma mit nejake chyby (PSP-ID)", counts[0] > 0);
        return counts;
    }

    @Test
    public void pathWithSpacesGivesSameResultAsPlainPath() throws Exception {
        int[] expected = plainPathCounts();
        int[] actual = RepoPackagesSupport.countErrorsAndWarnings(validate(new File(SPACES)), EXCLUDED_RULES);
        assertEquals("ERROR, cesta s mezerami", expected[0], actual[0]);
        assertEquals("WARNING, cesta s mezerami", expected[1], actual[1]);
    }

    @Test
    public void pathWithDiacriticsGivesSameResultAsPlainPath() throws Exception {
        int[] expected = plainPathCounts();
        int[] actual = RepoPackagesSupport.countErrorsAndWarnings(validate(new File(DIACRITICS)), EXCLUDED_RULES);
        assertEquals("ERROR, cesta s diakritikou", expected[0], actual[0]);
        assertEquals("WARNING, cesta s diakritikou", expected[1], actual[1]);
    }

    private static void copyDir(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) throws IOException {
                Files.createDirectories(dst.resolve(src.relativize(d).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                Files.copy(f, dst.resolve(src.relativize(f).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
