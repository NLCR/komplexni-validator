package nkp.pspValidator.gui.results;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Texty hlavního okna s výsledky: počty se správným tvarem slova, doba validace, datum. Bez JavaFX.
 */
public final class ResultTexts {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d. M. yyyy H:mm");

    private ResultTexts() {
    }

    /** „1 chyba“, „2 chyby“, „5 chyb“, „0 chyb“. */
    public static String errors(int count) {
        return count + " " + plural(count, "chyba", "chyby", "chyb");
    }

    /** „1 varování“ (slovo se nemění). */
    public static String warnings(int count) {
        return count + " varování";
    }

    /** „1 informace“, „2 informace“, „5 informací“, „0 informací“. */
    public static String infos(int count) {
        return count + " " + plural(count, "informace", "informace", "informací");
    }

    /**
     * Tvar podle počtu: 1, 2 až 4, ostatní (0, 5 a víc).
     */
    static String plural(int count, String one, String few, String many) {
        if (count == 1) {
            return one;
        } else if (count >= 2 && count <= 4) {
            return few;
        } else {
            return many;
        }
    }

    /**
     * Doba validace: pod minutu v sekundách na desetiny („3,4 s“), pod hodinu v minutách a sekundách („2 min 5 s“),
     * jinak v hodinách a minutách („1 h 2 min“).
     */
    public static String duration(long millis) {
        long ms = Math.max(0, millis);
        long tenths = Math.round(ms / 100.0);
        if (tenths < 600) {
            return (tenths / 10) + "," + (tenths % 10) + " s";
        }
        long seconds = Math.round(ms / 1000.0);
        if (seconds < 3600) {
            return (seconds / 60) + " min " + (seconds % 60) + " s";
        }
        long minutes = seconds / 60;
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }

    /** „19. 11. 2026 14:02“. */
    public static String dateTime(LocalDateTime time) {
        return DATE_TIME.format(time);
    }

    /** Řádek pod tlačítkem v prázdném stavu: „Nástroje: 5 z 9 dostupných“. */
    public static String toolsAvailable(int available, int total) {
        return "Nástroje: " + available + " z " + total + " " + plural(total, "dostupného", "dostupných", "dostupných");
    }

    /** „Konfigurace: 39 fDMF“. */
    public static String fdmfCount(int count) {
        return "Konfigurace: " + count + " fDMF";
    }
}
