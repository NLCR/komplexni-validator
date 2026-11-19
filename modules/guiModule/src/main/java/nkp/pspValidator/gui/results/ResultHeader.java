package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.Level;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Obsah horní lišty hlavního okna (název balíku, podtitul, verdikt a počty problémů) v jednotlivých fázích validace.
 * Počty a verdikt se jen přebírají z jádra ({@code ValidationState.ProgressListener.onValidationsFinish}), nepočítají
 * se znovu. Bez JavaFX.
 */
public final class ResultHeader {

    /** Fáze validace z pohledu horní lišty. */
    public enum Phase {
        RUNNING, FINISHED, FAILED, STOPPED
    }

    private final Phase phase;
    private final String title;
    private final String subtitle;
    private final String verdictText;
    private final String verdictStyleClass;
    private final int errors;
    private final int warnings;
    private final int infos;

    private ResultHeader(Phase phase, String title, String subtitle, String verdictText, String verdictStyleClass,
                         int errors, int warnings, int infos) {
        this.phase = phase;
        this.title = title;
        this.subtitle = subtitle;
        this.verdictText = verdictText;
        this.verdictStyleClass = verdictStyleClass;
        this.errors = errors;
        this.warnings = warnings;
        this.infos = infos;
    }

    /**
     * @param dmf typ a verze, podle kterých se validuje, nebo null, dokud nejsou rozpoznány
     */
    public static ResultHeader running(String packageName, Dmf dmf) {
        String subtitle = dmf == null ? "Rozpoznávám typ balíku, validace běží" : dmf + ", validace běží";
        return new ResultHeader(Phase.RUNNING, packageName, subtitle, null, null, 0, 0, 0);
    }

    /**
     * @param problemsByLevel počty problémů podle úrovně z jádra (chybějící úroveň znamená 0)
     * @param valid           verdikt z jádra
     */
    public static ResultHeader finished(String packageName, Dmf dmf, Map<Level, Integer> problemsByLevel, boolean valid,
                                        long durationMs, LocalDateTime finishedAt) {
        String subtitle = dmf + ", validováno " + ResultTexts.dateTime(finishedAt) + ", trvalo " + ResultTexts.duration(durationMs);
        return new ResultHeader(Phase.FINISHED, packageName, subtitle,
                valid ? "Validní" : "Nevalidní", valid ? "verdict-valid" : "verdict-invalid",
                count(problemsByLevel, Level.ERROR), count(problemsByLevel, Level.WARNING), count(problemsByLevel, Level.INFO));
    }

    /**
     * Validace skončila výjimkou (například nerozpoznaný typ nebo chybějící fDMF); podrobnost je ve stavovém řádku.
     */
    public static ResultHeader failed(String packageName, Dmf dmf) {
        String subtitle = dmf == null ? "Validace skončila chybou" : dmf + ", validace skončila chybou";
        return new ResultHeader(Phase.FAILED, packageName, subtitle, "Chyba", "verdict-invalid", 0, 0, 0);
    }

    public static ResultHeader stopped(String packageName, Dmf dmf) {
        String subtitle = dmf == null ? "Validace zrušena" : dmf + ", validace zrušena";
        return new ResultHeader(Phase.STOPPED, packageName, subtitle, "Zrušeno", "verdict-none", 0, 0, 0);
    }

    private static int count(Map<Level, Integer> byLevel, Level level) {
        if (byLevel == null) {
            return 0;
        }
        Integer count = byLevel.get(level);
        return count == null ? 0 : count;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    /** Text štítku s verdiktem, null během validace. */
    public String getVerdictText() {
        return verdictText;
    }

    /** Třída štítku s verdiktem (base.css), null během validace. */
    public String getVerdictStyleClass() {
        return verdictStyleClass;
    }

    /** Počty problémů se zobrazují jen u dokončené validace (jen tehdy jsou z jádra celkové počty). */
    public boolean isCountsVisible() {
        return phase == Phase.FINISHED;
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

    public String getErrorsText() {
        return ResultTexts.errors(errors);
    }

    public String getWarningsText() {
        return ResultTexts.warnings(warnings);
    }

    public String getInfosText() {
        return ResultTexts.infos(infos);
    }

    /** „Validovat znovu“ jde jen mimo běh validace. */
    public boolean isRunning() {
        return phase == Phase.RUNNING;
    }
}
