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

    /**
     * Uplynulý čas běžící validace po celých sekundách (zaokrouhleno dolů, aby údaj tikal pravidelně): „0:41“,
     * „12:05“, od hodiny „1:02:03“.
     */
    public static String elapsed(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return hours + ":" + twoDigits(minutes) + ":" + twoDigits(secs);
        }
        return minutes + ":" + twoDigits(secs);
    }

    private static String twoDigits(long value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    /** „19. 11. 2026 14:02“. */
    public static String dateTime(LocalDateTime time) {
        return DATE_TIME.format(time);
    }

    /** Řádek pod tlačítkem v prázdném stavu: „Nástroje: 5 z 9 dostupných“. */
    public static String toolsAvailable(int available, int total) {
        return "Nástroje: " + available + " z " + total + " " + plural(total, "dostupného", "dostupných", "dostupných");
    }

    /** Řádek pod tlačítkem v prázdném stavu, dokud se dostupnost nástrojů zjišťuje. */
    public static final String TOOLS_CHECKING = "Nástroje: zjišťuji…";

    /**
     * Řádek o nástrojích v prázdném stavu: „Nástroje: 5 z 9 dostupných“, nebo {@link #TOOLS_CHECKING}, když
     * dostupnost některého nástroje ještě žádná kontrola nezjistila ({@code available} je null).
     */
    public static String toolsInfo(Integer available, int total) {
        return available == null ? TOOLS_CHECKING : toolsAvailable(available, total);
    }

    /** Třída šedého odznaku počtu, když je počet nulový (barva úrovně jen u nenulového počtu). */
    public static final String BADGE_ZERO = "badge-muted";

    /**
     * Třída odznaku s počtem v horní liště: u nenulového počtu barva úrovně ({@code levelClass}, např.
     * {@code badge-error}), u nuly šedý {@link #BADGE_ZERO}.
     */
    public static String countBadgeClass(int count, String levelClass) {
        return count > 0 ? levelClass : BADGE_ZERO;
    }

    /** „Konfigurace: 39 fDMF“. */
    public static String fdmfCount(int count) {
        return "Konfigurace: " + count + " fDMF";
    }

    /**
     * Jako {@link #fdmfCount(int)}; u konfigurace jiné vedlejší verze než aplikace (OLDER_MINOR, NEWER_MINOR) s dovětkem
     * „Konfigurace: 39 fDMF (verze 3.0, aplikace 3.1)“. Ve stavu OK nebo bez známé verze se text nemění.
     *
     * @param configVersion   verze konfigurace z manifestu (MAJOR.MINOR)
     * @param appMajorMinor   verze aplikace (MAJOR.MINOR)
     */
    public static String fdmfCount(int count, nkp.pspValidator.shared.ConfigManifest.Status status, String configVersion,
                                   String appMajorMinor) {
        boolean differentMinor = status == nkp.pspValidator.shared.ConfigManifest.Status.OLDER_MINOR
                || status == nkp.pspValidator.shared.ConfigManifest.Status.NEWER_MINOR;
        if (!differentMinor || configVersion == null || appMajorMinor == null) {
            return fdmfCount(count);
        }
        return fdmfCount(count) + " (verze " + configVersion + ", aplikace " + appMajorMinor + ")";
    }
}
