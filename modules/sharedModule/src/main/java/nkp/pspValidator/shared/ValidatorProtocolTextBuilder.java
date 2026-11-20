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
        logRuleError(errorElevel, errorMessage, null);
    }

    /**
     * @param referencedFile cesta k souboru, ke kteremu hlaska patri (od adresare balicku vcetne, jako atributy file
     *                       a referencedFile v XML protokolu), nebo null. Pripoji se na konec radku jako
     *                       " (soubor: cesta)", pokud hlaska cestu (ani jeji cast uvnitr balicku) jeste neobsahuje.
     */
    public void logRuleError(Level errorElevel, String errorMessage, String referencedFile) {
        if (out != null) {
            String message = errorMessage;
            //hlaska na Windows obsahuje cestu se zpetnymi lomitky, referencedFile ma vzdy lomitka
            String comparable = message == null ? null : message.replace('\\', '/');
            if (referencedFile != null && !referencedFile.isEmpty()
                    && (comparable == null || !(comparable.contains(referencedFile) || comparable.contains(pathInsidePackage(referencedFile))))) {
                message = message + " (soubor: " + referencedFile + ")";
            }
            out.println(String.format("\t%s: %s", errorElevel, message));
        }
    }

    /**
     * Cast cesty za nazvem adresare balicku ("balik/pdf/a.pdf" -> "pdf/a.pdf"); cesta bez lomitka se vraci cela.
     */
    private static String pathInsidePackage(String path) {
        int slash = path.indexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    /**
     * Cesta k souboru od adresare balicku vcetne (napr. "nk-00027x/mastercopy/mc_0001.jp2"), tedy relativne vuci
     * adresari, ve kterem balicek lezi; stejna podoba jako atributy file a referencedFile v XML protokolu, jen vzdy
     * s lomitky '/'. Soubor mimo balicek se uvadi absolutni cestou (jako v XML protokolu).
     */
    public static String toPackageRelativePath(File packageRoot, File file) {
        if (file == null) {
            return null;
        }
        try {
            java.nio.file.Path root = packageRoot.getAbsoluteFile().toPath().normalize();
            java.nio.file.Path path = file.getAbsoluteFile().toPath().normalize();
            java.nio.file.Path parent = root.getParent();
            if (parent != null && path.startsWith(root) && !path.equals(root)) {
                return parent.relativize(path).toString().replace(File.separatorChar, '/');
            }
        } catch (RuntimeException e) {
            //fallback nize
        }
        return file.getAbsolutePath();
    }

    public void logPackageSummary(Integer totalProblemsSum, Map<Level, Integer> totalProblemsByLevel, boolean valid) {
        if (out != null) {
            String verdict = valid ? "validní" : "nevalidní";
            out.println(String.format("\nCelkem: %s, balík je: %s", buildSummary(totalProblemsSum, totalProblemsByLevel), verdict));
            out.println(String.format("Validováno Komplexním Validátorem ve verzi %s, build z %s", Version.VERSION_CODE, Version.BUILD_DATE));
            out.println();
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

    public void logXmlExportFailed(String reason) {
        if (out != null) {
            out.println("Xml export se nepodařilo vytvořit: " + reason);
        }
    }

}
