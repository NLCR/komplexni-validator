package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * checkFileListsMatch: soubory nalezene na disku (expected_files) proti souborum z metadat (found_files). Porovnava se
 * absolutni cesta jako retezec, tedy s rozlisenim velikosti pismen na vsech systemech (driv TreeSet&lt;File&gt;, ktery
 * na Windows velikost pismen nerozlisoval). Na disk se nesaha, test proto plati na kazdem systemu stejne.
 */
public class VfCheckFileListsMatchTest {

    private static final File ROOT = new File("balik").getAbsoluteFile();

    private static ValidationResult validate(List<File> expected, List<File> found) {
        VfCheckFileListsMatch vf = new VfCheckFileListsMatch("checkFileListsMatch", new Engine(null));
        vf.withValueParam(VfCheckFileListsMatch.PARAM_FILES_EXPECTED, ValueType.FILE_LIST, new ValueEvaluation(expected));
        vf.withValueParam(VfCheckFileListsMatch.PARAM_FILES_FOUND, ValueType.FILE_LIST, new ValueEvaluation(found));
        return vf.validate();
    }

    private static File f(String relative) {
        return new File(ROOT, relative);
    }

    private static String abs(String relative) {
        return f(relative).getAbsolutePath();
    }

    @Test
    public void sameFiles() {
        ValidationResult r = validate(Arrays.asList(f("mastercopy/mc_1.jp2"), f("mastercopy/mc_2.jp2")),
                Arrays.asList(f("mastercopy/mc_2.jp2"), f("mastercopy/mc_1.jp2")));
        assertFalse(r.hasProblems());
    }

    @Test
    public void referenceDiffersInCase() {
        ValidationResult r = validate(Arrays.asList(f("mastercopy/mc_1.jp2"), f("mastercopy/mc_2.jp2")),
                Arrays.asList(f("masterCopy/MC_1.jp2"), f("mastercopy/mc_2.jp2")));
        List<ValidationProblem> problems = r.getProblems();
        assertEquals(2, problems.size());
        assertEquals(Level.ERROR, problems.get(0).getLevel());
        assertEquals("nalezen neočekávaný soubor " + abs("masterCopy/MC_1.jp2") + "; v balíku je soubor " + abs("mastercopy/mc_1.jp2")
                + ", jehož název se liší velikostí písmen", problems.get(0).getFullMessage());
        assertEquals(Level.ERROR, problems.get(1).getLevel());
        assertEquals("nenalezen očekávaný soubor " + abs("mastercopy/mc_1.jp2") + "; odkaz v metadatech (" + abs("masterCopy/MC_1.jp2")
                + ") se liší velikostí písmen", problems.get(1).getFullMessage());
    }

    @Test
    public void differentFilesHaveNoCaseHint() {
        ValidationResult r = validate(Collections.singletonList(f("mastercopy/mc_1.jp2")),
                Collections.singletonList(f("mastercopy/mc_3.jp2")));
        List<ValidationProblem> problems = r.getProblems();
        assertEquals(2, problems.size());
        assertEquals("nalezen neočekávaný soubor " + abs("mastercopy/mc_3.jp2"), problems.get(0).getFullMessage());
        assertEquals("nenalezen očekávaný soubor " + abs("mastercopy/mc_1.jp2"), problems.get(1).getFullMessage());
    }

    @Test
    public void onlyMissingOrOnlyUnexpected() {
        ValidationResult missing = validate(Arrays.asList(f("a.xml"), f("b.xml")), Collections.singletonList(f("a.xml")));
        assertEquals(1, missing.getProblems().size());
        assertEquals("nenalezen očekávaný soubor " + abs("b.xml"), missing.getProblems().get(0).getFullMessage());

        ValidationResult unexpected = validate(Collections.singletonList(f("a.xml")), Arrays.asList(f("a.xml"), f("B.xml")));
        assertEquals(1, unexpected.getProblems().size());
        assertEquals("nalezen neočekávaný soubor " + abs("B.xml"), unexpected.getProblems().get(0).getFullMessage());
    }

    /**
     * Duplicitni odkaz se hlasi jen jednou (mnozina), jako driv.
     */
    @Test
    public void duplicatesAreIgnored() {
        assertFalse(validate(Collections.singletonList(f("a.xml")), Arrays.asList(f("a.xml"), f("a.xml"))).hasProblems());
        ValidationResult r = validate(Collections.singletonList(f("a.xml")), Arrays.asList(f("A.xml"), f("A.xml")));
        assertEquals(2, r.getProblems().size());
    }
}
