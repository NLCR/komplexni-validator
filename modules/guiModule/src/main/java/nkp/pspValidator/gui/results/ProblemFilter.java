package nkp.pspValidator.gui.results;

import nkp.pspValidator.gui.validation.ProcessingState;
import nkp.pspValidator.gui.validation.RuleWithState;
import nkp.pspValidator.gui.validation.SectionWithState;
import nkp.pspValidator.shared.engine.Level;

/**
 * Filtr výsledků v hlavním okně: které úrovně problémů se zobrazují (chyby, varování, informace) a zda skrýt sekce
 * a pravidla bez problémů zobrazených úrovní. Neměnná hodnota, bez JavaFX.
 */
public final class ProblemFilter {

    private final boolean errors;
    private final boolean warnings;
    private final boolean infos;
    private final boolean onlyWithProblems;

    public ProblemFilter(boolean errors, boolean warnings, boolean infos, boolean onlyWithProblems) {
        this.errors = errors;
        this.warnings = warnings;
        this.infos = infos;
        this.onlyWithProblems = onlyWithProblems;
    }

    /** Výchozí stav: všechny úrovně, všechny sekce a pravidla (jako dřív volba „ERROR, WARNING, INFO“). */
    public static ProblemFilter all() {
        return new ProblemFilter(true, true, true, false);
    }

    public boolean isErrors() {
        return errors;
    }

    public boolean isWarnings() {
        return warnings;
    }

    public boolean isInfos() {
        return infos;
    }

    public boolean isOnlyWithProblems() {
        return onlyWithProblems;
    }

    public boolean accepts(Level level) {
        switch (level) {
            case ERROR:
                return errors;
            case WARNING:
                return warnings;
            case INFO:
                return infos;
            default:
                return false;
        }
    }

    /** Součet problémů zobrazených úrovní. */
    public int visibleCount(int errorCount, int warningCount, int infoCount) {
        return (errors ? errorCount : 0) + (warnings ? warningCount : 0) + (infos ? infoCount : 0);
    }

    public boolean showSection(SectionWithState section) {
        return show(section.getState(), section.getErrors(), section.getWarnings(), section.getInfos());
    }

    public boolean showRule(RuleWithState rule) {
        return show(rule.getState(), rule.getErrors(), rule.getWarnings(), rule.getInfos());
    }

    /**
     * Čekající a běžící položky se ukazují vždy (během validace ještě nemají konečné počty), ostatní při volbě
     * „jen s problémy“ jen s aspoň jedním problémem zobrazené úrovně.
     */
    private boolean show(ProcessingState state, int errorCount, int warningCount, int infoCount) {
        if (!onlyWithProblems || state == ProcessingState.WAITING || state == ProcessingState.RUNNING) {
            return true;
        }
        return visibleCount(errorCount, warningCount, infoCount) > 0;
    }
}
