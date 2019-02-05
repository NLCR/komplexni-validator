package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Level;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;

/**
 * Created by Martin Řehánek on 15.12.16.
 */
public class ValidatorProtocolTextBuilder {
    private final PrintStream out;

    public ValidatorProtocolTextBuilder(PrintStream out) {
        this.out = out;
    }

    public void logSectionStart(String sectionName, int sectionProblemsTotal, Map<Level, Integer> sectionProblemsByLevel) {
        if (out != null) {
            out.println();
            String sectionTitle = String.format("Sekce %s: %s", sectionName, buildSummary(sectionProblemsTotal, sectionProblemsByLevel));
            out.println(sectionTitle);
            out.println(buildDelimiter(sectionTitle.length()));
        }
    }

    public void logSectionSkipped(String sectionName) {
        if (out != null) {
            out.println();
            String sectionTitle = String.format("Sekce %s: přeskočena", sectionName);
            out.println(sectionTitle);
            out.println(buildDelimiter(sectionTitle.length()));
        }
    }

    private String buildSummary(int totalProblems, Map<Level, Integer> problemsByLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.declineProblemNumber(totalProblems));
        if (totalProblems != 0) {
            builder.append(" (");
            boolean first = true;
            for (int i = 0; i < Level.values().length; i++) {
                Level level = Level.values()[i];
                int count = problemsByLevel.get(level);
                if (count != 0) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append(String.format("%dx %s", count, level));
                    first = false;
                }
            }
            builder.append(")");
        }
        return builder.toString();
    }

    private String buildDelimiter(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append('-');
        }
        return builder.toString();
    }

    public void logRuleStart(String ruleName, String ruleDescription, int ruleProblemsTotal, Map<Level, Integer> ruleProblemsByLevel) {
        if (out != null) {
            out.println(String.format("Pravidlo %s: %s", ruleName, buildSummary(ruleProblemsTotal, ruleProblemsByLevel)));
            out.println(String.format("\t%s", ruleDescription));
        }
    }

    public void logRuleError(Level errorElevel, String errorMessage) {
        if (out != null) {
            out.println(String.format("\t%s: %s", errorElevel, errorMessage));
        }
    }

    public void logPackageSummary(Integer totalProblemsSum, Map<Level, Integer> totalProblemsByLevel, boolean valid) {
        if (out != null) {
            String verdict = valid ? "validní" : "nevalidní";
            out.println(String.format("\nCelkem: %s, balík je: %s", buildSummary(totalProblemsSum, totalProblemsByLevel), verdict));
        }
    }

    public void logXmlExportStarted(File xmlOutputFile) {
        if (out != null) {
            out.println(String.format("Vytvářím xml export do souboru %s.", xmlOutputFile.getAbsolutePath()));
        }
    }

    public void logXmlExportCreated() {
        if (out != null) {
            out.println("Xml export vytvořen a uložen.");
        }
    }

}
