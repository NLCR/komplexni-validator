package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.VerifiedVersions;
import nkp.pspValidator.shared.externalUtils.VersionCheck;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Texty a rozhodnutí pro dialog Externí nástroje (tabulka nástrojů): celkový stav, souhrn v patičce, text stavu
 * v řádku (zkrácený, celý v tooltipu), podrobnosti chyby. Neověřená nebo nerozpoznaná verze nalezeného nástroje
 * ({@link ToolStatus#isVersionWarning()}) má za verzí „neověřená verze“ (nebo „verzi nelze rozpoznat“) a žlutou tečku,
 * ověřené řady a přesná verze, na které se ověřovalo, jsou v tooltipu; nástroj se přitom počítá jako dostupný.
 */
public final class ToolsOverview {

    /** Nejdelší text stavu v řádku tabulky, delší se zkrátí (celý je v tooltipu). */
    public static final int MAX_STATUS_LENGTH = 30;

    /**
     * Číslo verze ve výpisu nástroje: aspoň dvě čísla oddělená tečkou, volitelně s „v“ na začátku a s číslem sestavení
     * za pomlčkou („7.1.2-24“, „v8.0.5“, „1.20.1“); nesmí navazovat na písmeno, číslici ani tečku.
     */
    private static final Pattern VERSION_NUMBER = Pattern.compile("(?<![\\p{L}\\d.])v?(\\d+(?:\\.\\d+)+(?:-\\d+)?)");

    /** Celkový stav kontroly všech nástrojů. */
    public enum State {
        /** některý nástroj se ještě kontroluje */
        CHECKING,
        /** všechny nástroje jsou dostupné */
        ALL_AVAILABLE,
        /** kontrola doběhla, některý nástroj je nedostupný (nenalezen nebo chyba při spuštění) */
        SOME_UNAVAILABLE
    }

    private ToolsOverview() {
    }

    /** Celkový stav; nástroj bez stavu se považuje za kontrolovaný. */
    public static State state(Map<ExternalUtil, ToolStatus> statuses) {
        boolean someUnavailable = false;
        for (ExternalUtil util : ExternalUtil.values()) {
            ToolStatus status = statuses.get(util);
            if (status == null || status.getState() == ToolStatus.State.CHECKING) {
                return State.CHECKING;
            }
            if (!status.isAvailable()) {
                someUnavailable = true;
            }
        }
        return someUnavailable ? State.SOME_UNAVAILABLE : State.ALL_AVAILABLE;
    }

    /**
     * Jde dialog Externí nástroje při startu aplikace opustit (tlačítko Pokračovat, zavření okna)? Během kontroly ne,
     * po ní ano, i když některý nástroj chybí (nástroje jsou volitelné, aplikace pokračuje do hlavního okna).
     */
    public static boolean canContinue(State state) {
        return state != State.CHECKING;
    }

    /** Souhrn do patičky, např. „5 z 9 nástrojů dostupných“, během kontroly „Kontroluji nástroje, hotovo 4 z 9…“. */
    public static String summary(Map<ExternalUtil, ToolStatus> statuses) {
        int all = ExternalUtil.values().length;
        int done = 0;
        int available = 0;
        for (ExternalUtil util : ExternalUtil.values()) {
            ToolStatus status = statuses.get(util);
            if (status != null && status.getState() != ToolStatus.State.CHECKING) {
                done++;
                if (status.isAvailable()) {
                    available++;
                }
            }
        }
        if (done < all) {
            return "Zjišťuji dostupnost nástrojů, hotovo " + done + " " + Summaries.fromPreposition(all) + " " + all + "…";
        }
        return available + " " + Summaries.fromPreposition(all) + " " + all + " nástrojů dostupných";
    }

    /**
     * Text stavu do řádku tabulky: u nalezeného nástroje název a číslo verze ({@link #shortVersion}), zkrácený na
     * {@link #MAX_STATUS_LENGTH} znaků.
     */
    public static String statusText(ExternalUtil util, ToolStatus status) {
        if (status.getState() == ToolStatus.State.FOUND && status.getVersion() != null && !status.getVersion().isEmpty()) {
            return truncate("nalezen, " + shortVersion(util, status.getVersion()), MAX_STATUS_LENGTH) + versionWarningSuffix(status);
        }
        return truncate(status.getText(), MAX_STATUS_LENGTH);
    }

    /**
     * Přípona textu stavu u neověřené („, neověřená verze“) nebo nerozpoznané verze („, verzi nelze rozpoznat“),
     * jinak prázdný řetězec. Ke zkrácenému textu se připojuje celá, aby označení nezmizelo.
     */
    static String versionWarningSuffix(ToolStatus status) {
        if (!status.isVersionWarning()) {
            return "";
        }
        return status.getVersionCheck().getStatus() == VersionCheck.Status.UNRECOGNIZED
                ? ", verzi nelze rozpoznat"
                : ", neověřená verze";
    }

    /**
     * Text stavu do úzké tabulky (Nastavení), kde je název nástroje v tomtéž řádku vlevo: u nalezeného nástroje jen
     * číslo verze („nalezen, 7.1.2-24“), jinak jako {@link #statusText}.
     */
    public static String compactStatusText(ExternalUtil util, ToolStatus status) {
        //v uzkem sloupci se "nalezen, <verze>, neověřená verze" nevejde: oznaceni ma prednost, ze je nastroj nalezen,
        //rika zluta tecka
        if (status.isVersionWarning()) {
            if (status.getVersionCheck().getStatus() == VersionCheck.Status.UNRECOGNIZED) {
                return "verzi nelze rozpoznat";
            }
            String number = status.getVersion() == null ? null : versionNumber(status.getVersion());
            return number == null ? "neověřená verze" : "neověřená " + number;
        }
        if (status.getState() == ToolStatus.State.FOUND && status.getVersion() != null) {
            String number = versionNumber(status.getVersion());
            if (number != null) {
                return truncate("nalezen, " + number, MAX_STATUS_LENGTH) + versionWarningSuffix(status);
            }
        }
        return statusText(util, status);
    }

    /**
     * Krátký tvar verze nástroje: název nástroje a první číslo verze z výpisu, např. „ImageMagick 7.1.2-24“
     * z „ImageMagick 7.1.2-24 Q16-HDRI aarch64“ nebo „JHOVE 1.20.1“ z „Rel. 1.20.1“. Když výpis číslo verze
     * neobsahuje, vrátí první řádek výpisu beze změny (zkrátí se pak jako ostatní texty).
     */
    public static String shortVersion(ExternalUtil util, String version) {
        String number = versionNumber(version);
        return number != null ? util.getUserFriendlyName() + " " + number : firstLine(version);
    }

    /** První číslo verze z prvního řádku výpisu nástroje (bez „v“ na začátku), nebo null. */
    public static String versionNumber(String version) {
        Matcher matcher = VERSION_NUMBER.matcher(firstLine(version));
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String firstLine(String text) {
        return text.trim().split("\\R", 2)[0].trim();
    }

    /**
     * Celý text stavu do tooltipu: u nalezeného nástroje celá verze a na dalším řádku ověření verze
     * ({@link #versionDetails}), u nedostupného i výstup kontroly.
     */
    public static String statusTooltip(ToolStatus status) {
        String text = status.getText();
        if (status.getDetail() != null && !status.getDetail().isEmpty()) {
            return text + ": " + status.getDetail();
        }
        if (status.getState() == ToolStatus.State.FOUND) {
            String details = versionDetails(status.getVersionCheck());
            if (details != null) {
                return text + "\n" + details;
            }
        }
        return text;
    }

    /**
     * Ověření verze nalezeného nástroje do tooltipu, nebo null, když seznam ověřených řad není. Například
     * „Ověřená řada 1.34 (ověřeno s verzí 1.34.0).“, na Linuxu „Ověřená řada 1.34, ověřeno na macOS s verzí 1.34.0
     * (na této platformě zatím ne).“, u neověřené „Neověřená verze 1.36.0, výsledky kontrol se mohou lišit. Ověřené
     * řady: 1.29 (ověřeno s verzí 1.29.0-SNAPSHOT), 1.34 (ověřeno s verzí 1.34.0).“
     */
    public static String versionDetails(VersionCheck check) {
        if (check == null || check.getVerifiedVersions() == null) {
            return null;
        }
        VerifiedVersions verified = check.getVerifiedVersions();
        VerifiedVersions.Series matched = check.getMatchedSeries();
        switch (check.getStatus()) {
            case VERIFIED:
                return "Ověřená řada " + matched.getSeries()
                        + (matched.getVerifiedWith() == null ? "." : " (ověřeno s verzí " + matched.getVerifiedWith() + ").");
            case VERIFIED_ON_OTHER_PLATFORM:
                return "Ověřená řada " + matched.getSeries() + ", ověřeno na " + VersionCheck.platformName(verified.getInheritedFrom())
                        + (matched.getVerifiedWith() == null ? "" : " s verzí " + matched.getVerifiedWith())
                        + " (na této platformě zatím ne).";
            case UNVERIFIED:
                return "Neověřená verze " + check.getVersion() + ", výsledky kontrol se mohou lišit. " + verifiedSeriesSentence(verified);
            case UNRECOGNIZED:
                return "Verzi nelze rozpoznat, výsledky kontrol se mohou lišit. " + verifiedSeriesSentence(verified);
            default:
                return null;
        }
    }

    /**
     * „Ověřené řady: 1.29 (ověřeno s verzí 1.29.0-SNAPSHOT), 1.34 (ověřeno s verzí 1.34.0).“, u jedné řady „Ověřená
     * řada: …“, u převzatého seznamu „Ověřené řady (ověřeno na macOS): …“, u prázdného „Na této platformě nebyla
     * s validátorem ověřena žádná verze.“
     */
    private static String verifiedSeriesSentence(VerifiedVersions verified) {
        String where = verified.isInherited() ? " (ověřeno na " + VersionCheck.platformName(verified.getInheritedFrom()) + ")" : "";
        if (verified.getSeries().isEmpty()) {
            return verified.isInherited()
                    ? "Na " + VersionCheck.platformName(verified.getInheritedFrom()) + " nebyla s validátorem ověřena žádná verze."
                    : "Na této platformě nebyla s validátorem ověřena žádná verze.";
        }
        StringBuilder list = new StringBuilder();
        for (VerifiedVersions.Series series : verified.getSeries()) {
            list.append(list.length() == 0 ? "" : ", ").append(series.getSeries());
            if (series.getVerifiedWith() != null) {
                list.append(" (ověřeno s verzí ").append(series.getVerifiedWith()).append(')');
            }
        }
        return (verified.getSeries().size() == 1 ? "Ověřená řada" : "Ověřené řady") + where + ": " + list + ".";
    }

    /**
     * Třída stavové tečky z base.css; nenalezený nástroj není chyba (nástroje jsou volitelné), proto šedá; nalezený
     * s neověřenou nebo nerozpoznanou verzí žlutá.
     */
    public static String dotStyleClass(ToolStatus status) {
        switch (status.getState()) {
            case FOUND:
                return status.isVersionWarning() ? "status-dot-warning" : "status-dot-ok";
            case ERROR:
                return "status-dot-error";
            default:
                return "status-dot-none";
        }
    }

    /** Třída textu stavu z base.css, nebo null pro běžný text. */
    public static String textStyleClass(ToolStatus status) {
        switch (status.getState()) {
            case FOUND:
                return null;
            case ERROR:
                return "text-error";
            default:
                return "text-muted";
        }
    }

    /** Má smysl nabízet podrobnosti (výstup kontroly a návod k instalaci): jen u nedostupného nástroje. */
    public static boolean hasDetails(ToolStatus status) {
        return status.getState() == ToolStatus.State.NOT_FOUND || status.getState() == ToolStatus.State.ERROR;
    }

    /** Nadpis okna s podrobnostmi, např. „Kakadu: chyba při spuštění“. */
    public static String detailsTitle(ExternalUtil util, ToolStatus status) {
        return util.getUserFriendlyName() + ": " + status.getText();
    }

    /**
     * Celé podrobnosti kontroly nedostupného nástroje: nastavený adresář a celý výstup kontroly (u prázdného výstupu
     * hláška s prvním řádkem druhého proudu a návratovým kódem, jak ji sestaví detekce verze).
     *
     * @param configuredDir adresář nástroje z nastavení, nebo null
     */
    public static String detailsText(ToolStatus status, File configuredDir) {
        StringBuilder text = new StringBuilder();
        text.append("Adresář nástroje: ")
                .append(configuredDir == null ? "nenastaven" : configuredDir.getAbsolutePath())
                .append("\n\nVýstup kontroly:\n")
                .append(status.getDetail() == null || status.getDetail().isEmpty() ? "(bez podrobností)" : status.getDetail());
        return text.toString();
    }

    /** Tooltip tlačítka pro nastavení cesty. */
    public static String pathTooltip(File configuredDir) {
        return configuredDir == null
                ? "Adresář nástroje není nastaven."
                : "Nastavený adresář: " + configuredDir.getAbsolutePath();
    }

    /**
     * Zkrátí text na nejvýš {@code max} znaků včetně výpustky „…“ (mezery před výpustkou se vynechají); null zůstane
     * null.
     */
    public static String truncate(String text, int max) {
        if (max < 1) {
            throw new IllegalArgumentException("max must be positive: " + max);
        }
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1).trim() + "…";
    }
}
