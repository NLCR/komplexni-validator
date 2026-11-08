package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rozbor textoveho logu validace pro RepoPackagesSmokeTest: pocty ERROR/WARNING podle zahlavi pravidel a hlasky jednoho pravidla.
 */
final class RepoPackagesSupport {

    // nacteni konfigurace a beh validace: FdmfTestSupport (sdileny registr), tady jen rozbor logu

    private static final Pattern RULE_HEADER = Pattern.compile("^Pravidlo (\\S+): (.*)$");
    private static final Pattern ERROR_COUNT = Pattern.compile("(\\d+)x ERROR");
    private static final Pattern WARNING_COUNT = Pattern.compile("(\\d+)x WARNING");

    /**
     * Soucet ERROR a WARNING ze zahlavi pravidel ("Pravidlo X: 3 problemy (1x ERROR, 2x WARNING)"),
     * bez pravidel ze seznamu excludedRules.
     *
     * @return pole {pocet ERROR, pocet WARNING}
     */
    static int[] countErrorsAndWarnings(String log, Collection<String> excludedRules) {
        int errors = 0;
        int warnings = 0;
        for (String line : log.split("\n")) {
            Matcher header = RULE_HEADER.matcher(stripCr(line));
            if (header.matches() && !excludedRules.contains(header.group(1))) {
                Matcher e = ERROR_COUNT.matcher(header.group(2));
                Matcher w = WARNING_COUNT.matcher(header.group(2));
                errors += e.find() ? Integer.parseInt(e.group(1)) : 0;
                warnings += w.find() ? Integer.parseInt(w.group(1)) : 0;
            }
        }
        return new int[]{errors, warnings};
    }

    /**
     * Radky problemu (ERROR/WARNING/INFO) vypsane pod zahlavim daneho pravidla, napr. "ERROR: nalezen necekany soubor: ...".
     */
    static List<String> problemsOfRule(String log, String ruleId) {
        List<String> result = new ArrayList<>();
        String currentRule = null;
        for (String line : log.split("\n")) {
            line = stripCr(line);
            Matcher header = RULE_HEADER.matcher(line);
            if (header.matches()) {
                currentRule = header.group(1);
            } else if (ruleId.equals(currentRule)
                    && (line.startsWith("\tERROR: ") || line.startsWith("\tWARNING: ") || line.startsWith("\tINFO: "))) {
                result.add(line.substring(1));
            }
        }
        return result;
    }

    private static String stripCr(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
