package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.Level;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Obsah horní lišty hlavního okna (název balíku, podtitul, verdikt a počty problémů) v jednotlivých fázích validace.
 * Verdikt a celkové počty se jen přebírají z jádra ({@code ValidationState.ProgressListener.onValidationsFinish}),
 * nepočítají se znovu; průběžné počty během běhu dodává {@link ValidationProgress}. Bez JavaFX.
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
    /** null = počty se nezobrazují */
    private final ProblemCounts counts;

    private ResultHeader(Phase phase, String title, String subtitle, String verdictText, String verdictStyleClass,
                         ProblemCounts counts) {
        this.phase = phase;
        this.title = title;
        this.subtitle = subtitle;
        this.verdictText = verdictText;
        this.verdictStyleClass = verdictStyleClass;
        this.counts = counts;
    }

    /**
     * @param dmf       typ a verze, podle kterých se validuje, nebo null, dokud nejsou rozpoznány
     * @param elapsedMs doba od spuštění validace (tiká po sekundách)
     * @param found     průběžné počty problémů z dokončených pravidel, null před načtením pravidel (odznaky se skryjí)
     */
    public static ResultHeader running(String packageName, Dmf dmf, long elapsedMs, ProblemCounts found) {
        String time = ResultTexts.elapsed(elapsedMs);
        String subtitle = dmf == null ? "Rozpoznávám typ balíku, validace běží " + time : dmf + ", validace běží " + time;
        return new ResultHeader(Phase.RUNNING, packageName, subtitle, null, null, found);
    }

    /**
     * @param problemsByLevel počty problémů podle úrovně z jádra (chybějící úroveň znamená 0)
     * @param valid           verdikt z jádra
     */
    public static ResultHeader finished(String packageName, Dmf dmf, Map<Level, Integer> problemsByLevel, boolean valid,
                                        long durationMs, LocalDateTime finishedAt) {
        String subtitle = dmf + ", validováno " + ResultTexts.dateTime(finishedAt) + ", trvalo " + ResultTexts.duration(durationMs);
        return new ResultHeader(Phase.FINISHED, packageName, subtitle,
                valid ? "Validní" : "Nevalidní", valid ? "verdict-valid" : "verdict-invalid", ProblemCounts.of(problemsByLevel));
    }

    /**
     * Validace skončila výjimkou (například nerozpoznaný typ, chybějící fDMF nebo chyba během pravidla); podrobnost
     * je v hlášce v okně.
     *
     * @param found počty problémů z pravidel dokončených před chybou, null, když se pravidla nenačetla (odznaky se skryjí)
     */
    public static ResultHeader failed(String packageName, Dmf dmf, ProblemCounts found) {
        String subtitle = dmf == null ? "Validace skončila chybou" : dmf + ", validace skončila chybou";
        return new ResultHeader(Phase.FAILED, packageName, subtitle, "Chyba", "verdict-invalid", found);
    }

    /**
     * @param found počty problémů z pravidel dokončených před zrušením, null, když se pravidla nenačetla (odznaky se skryjí)
     */
    public static ResultHeader stopped(String packageName, Dmf dmf, ProblemCounts found) {
        String subtitle = dmf == null ? "Validace zrušena" : dmf + ", validace zrušena";
        return new ResultHeader(Phase.STOPPED, packageName, subtitle, "Zrušeno", "verdict-none", found);
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

    /**
     * Počty problémů: u dokončené validace celkové z jádra, během běhu, po zrušení a po chybě průběžné (z dokončených
     * pravidel); nezobrazují se, dokud jádro neohlásí pravidla.
     */
    public boolean isCountsVisible() {
        return counts != null;
    }

    public int getErrors() {
        return counts == null ? 0 : counts.getErrors();
    }

    public int getWarnings() {
        return counts == null ? 0 : counts.getWarnings();
    }

    public int getInfos() {
        return counts == null ? 0 : counts.getInfos();
    }

    public String getErrorsText() {
        return ResultTexts.errors(getErrors());
    }

    public String getWarningsText() {
        return ResultTexts.warnings(getWarnings());
    }

    public String getInfosText() {
        return ResultTexts.infos(getInfos());
    }

    /** „Validovat znovu“ jde jen mimo běh validace. */
    public boolean isRunning() {
        return phase == Phase.RUNNING;
    }
}
