package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Textovy protokol v nizsi verbosity (Validator.run, 0 az 3) je podmnozinou radku vyssi verbosity se stejnym souhrnem.
 * Nevalidni balicek, aby verbosity 1 a 2 mely co vypsat (sekce a pravidla s problemy).
 */
public class ProtocolVerbosityTest {

    private static final File PACKAGE = new File("src/test/resources/fund_unit_0.2/page_no_genre/nk-00027r");
    private static final Dmf DMF = new Dmf(Dmf.Type.FUND_UNIT, "0.2");

    @Test
    public void lowerVerbosityIsSubsetOfHigher() throws Exception {
        List<List<String>> logs = new ArrayList<>();
        for (int verbosity = 0; verbosity <= 3; verbosity++) {
            logs.add(nonEmptyLines(FdmfTestSupport.run(DMF, PACKAGE, verbosity, null)));
        }
        String summary = summaryLine(logs.get(3));
        assertTrue("balicek ma byt nevalidni: " + summary, summary.endsWith("balík je: nevalidní"));

        // 0: jen souhrn a paticka s verzi
        assertEquals(2, logs.get(0).size());
        // 1: navic sekce s problemy, bez pravidel; 2: navic pravidla s problemy a hlasky; 3: vse
        assertTrue(logs.get(1).stream().anyMatch(l -> l.startsWith("Sekce ")));
        assertTrue(logs.get(1).stream().noneMatch(l -> l.startsWith("Pravidlo ")));
        assertTrue(logs.get(2).stream().anyMatch(l -> l.startsWith("Pravidlo ")));
        assertTrue(logs.get(2).stream().anyMatch(l -> l.startsWith("\tERROR: ")));
        assertTrue(logs.get(2).stream().noneMatch(l -> l.matches("^Pravidlo \\S+: 0 problémů$")));
        assertTrue(logs.get(3).stream().anyMatch(l -> l.matches("^Pravidlo \\S+: 0 problémů$")));

        for (int verbosity = 0; verbosity < 3; verbosity++) {
            List<String> lower = logs.get(verbosity);
            List<String> higher = logs.get(verbosity + 1);
            assertEquals("souhrn ve verbosity " + verbosity, summary, summaryLine(lower));
            assertTrue("verbosity " + verbosity + " ma mit mene radku nez " + (verbosity + 1), lower.size() < higher.size());
            assertTrue("verbosity " + verbosity + " neni podmnozinou " + (verbosity + 1) + ": " + missing(lower, higher),
                    missing(lower, higher).isEmpty());
        }
    }

    private static List<String> nonEmptyLines(String log) {
        List<String> result = new ArrayList<>();
        for (String line : log.replace("\r\n", "\n").split("\n")) {
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

    private static String summaryLine(List<String> lines) {
        return lines.stream().filter(l -> l.startsWith("Celkem: ")).findFirst()
                .orElseThrow(() -> new AssertionError("chybi radek Celkem"));
    }

    /**
     * Radky lower, ktere v higher chybi (s ohledem na pocet vyskytu).
     */
    private static List<String> missing(List<String> lower, List<String> higher) {
        Map<String, Integer> available = new HashMap<>();
        for (String line : higher) {
            available.merge(line, 1, Integer::sum);
        }
        List<String> result = new ArrayList<>();
        for (String line : lower) {
            Integer count = available.get(line);
            if (count == null || count == 0) {
                result.add(line);
            } else {
                available.put(line, count - 1);
            }
        }
        return result;
    }
}
