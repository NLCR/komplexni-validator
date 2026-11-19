package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.engine.Level;

import java.util.Map;

/**
 * Počty problémů podle úrovně (chyby, varování, informace). Neměnné. Bez JavaFX.
 */
public final class ProblemCounts {

    public static final ProblemCounts ZERO = new ProblemCounts(0, 0, 0);

    private final int errors;
    private final int warnings;
    private final int infos;

    public ProblemCounts(int errors, int warnings, int infos) {
        this.errors = errors;
        this.warnings = warnings;
        this.infos = infos;
    }

    /**
     * Počty z mapy jádra; chybějící úroveň (nebo celá mapa) znamená 0.
     */
    public static ProblemCounts of(Map<Level, Integer> byLevel) {
        return new ProblemCounts(count(byLevel, Level.ERROR), count(byLevel, Level.WARNING), count(byLevel, Level.INFO));
    }

    private static int count(Map<Level, Integer> byLevel, Level level) {
        if (byLevel == null) {
            return 0;
        }
        Integer count = byLevel.get(level);
        return count == null ? 0 : count;
    }

    public ProblemCounts plus(ProblemCounts other) {
        return new ProblemCounts(errors + other.errors, warnings + other.warnings, infos + other.infos);
    }

    public int getErrors() {
        return errors;
    }

    public int getWarnings() {
        return warnings;
    }

    public int getInfos() {
        return infos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProblemCounts)) return false;
        ProblemCounts that = (ProblemCounts) o;
        return errors == that.errors && warnings == that.warnings && infos == that.infos;
    }

    @Override
    public int hashCode() {
        return (errors * 31 + warnings) * 31 + infos;
    }

    @Override
    public String toString() {
        return errors + "/" + warnings + "/" + infos;
    }
}
