package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;

/**
 * Výsledek porovnání zjištěné verze externího nástroje s ověřenými řadami verzí pro platformu (hodnota). Neověřená
 * nebo nerozpoznaná verze validaci nezastaví: validace u pravidla nástroje přidá jednu INFO ({@link #protocolMessage}),
 * CLI a GUI ji označí.
 */
public final class VersionCheck {

    public enum Status {
        /** verze patří do ověřené řady platformy */
        VERIFIED,
        /** verze patří do řady ověřené na jiné platformě, jejíž seznam platforma přebírá (Linux z macOS) */
        VERIFIED_ON_OTHER_PLATFORM,
        /** verze nepatří do žádné ověřené řady (i když na platformě není ověřena žádná) */
        UNVERIFIED,
        /** z textu detekce nejde číslo verze rozpoznat */
        UNRECOGNIZED,
        /**
         * seznam ověřených řad pro nástroj a platformu v konfiguraci není (starší nebo upravená externalUtils.xml):
         * nic se neoznačuje
         */
        NO_LIST
    }

    /** Nejdelší výstup detekce v hlášce o nerozpoznané verzi (první řádek, delší se zkrátí). */
    static final int MAX_DETECTION_TEXT = 80;

    private final Status status;
    private final String detectionText;
    private final String version;
    private final VerifiedVersions verifiedVersions;
    private final VerifiedVersions.Series matchedSeries;

    private VersionCheck(Status status, String detectionText, String version, VerifiedVersions verifiedVersions,
                         VerifiedVersions.Series matchedSeries) {
        this.status = status;
        this.detectionText = detectionText;
        this.version = version;
        this.verifiedVersions = verifiedVersions;
        this.matchedSeries = matchedSeries;
    }

    /**
     * Porovná text detekce verze s ověřenými řadami.
     *
     * @param verifiedVersions ověřené řady pro platformu, nebo null, když je konfigurace neuvádí
     * @param detectionText    text, který vrátila detekce verze
     */
    public static VersionCheck evaluate(VerifiedVersions verifiedVersions, String detectionText) {
        String version = UtilVersion.parse(detectionText);
        if (verifiedVersions == null) {
            return new VersionCheck(Status.NO_LIST, detectionText, version, null, null);
        }
        if (version == null) {
            return new VersionCheck(Status.UNRECOGNIZED, detectionText, null, verifiedVersions, null);
        }
        VerifiedVersions.Series series = verifiedVersions.findSeries(version);
        if (series == null) {
            return new VersionCheck(Status.UNVERIFIED, detectionText, version, verifiedVersions, null);
        }
        Status status = verifiedVersions.isInherited() ? Status.VERIFIED_ON_OTHER_PLATFORM : Status.VERIFIED;
        return new VersionCheck(status, detectionText, version, verifiedVersions, series);
    }

    public Status getStatus() {
        return status;
    }

    /** Text detekce verze, ze kterého se vycházelo. */
    public String getDetectionText() {
        return detectionText;
    }

    /** Rozpoznané číslo verze ({@link UtilVersion#parse}), nebo null. */
    public String getVersion() {
        return version;
    }

    /** Ověřené řady pro platformu, nebo null ({@link Status#NO_LIST}). */
    public VerifiedVersions getVerifiedVersions() {
        return verifiedVersions;
    }

    /** Řada, do které verze patří, nebo null. */
    public VerifiedVersions.Series getMatchedSeries() {
        return matchedSeries;
    }

    /** Má se verze označit (neověřená nebo nerozpoznaná)? */
    public boolean isWarning() {
        return status == Status.UNVERIFIED || status == Status.UNRECOGNIZED;
    }

    /** Čitelný název platformy: „macOS“, „Windows“, „Linux“. */
    public static String platformName(OperatingSystem os) {
        switch (os) {
            case MAC:
                return "macOS";
            case WINDOWS:
                return "Windows";
            case LINUX:
                return "Linux";
            default:
                return os.name();
        }
    }

    /**
     * Ověřené řady do hlášky: „ověřená řada: 7.1“, „ověřené řady: 1.29, 1.34“, u převzatého seznamu „ověřené řady
     * na macOS: 1.29, 1.34“, u prázdného „na této platformě nebyla ověřena žádná verze“ (převzatého „na macOS nebyla
     * ověřena žádná verze“); null, když seznam není.
     */
    public String seriesPhrase() {
        if (verifiedVersions == null) {
            return null;
        }
        String where = verifiedVersions.isInherited() ? "na " + platformName(verifiedVersions.getInheritedFrom()) : null;
        int count = verifiedVersions.getSeries().size();
        if (count == 0) {
            return (where == null ? "na této platformě" : where) + " nebyla ověřena žádná verze";
        }
        return (count == 1 ? "ověřená řada" : "ověřené řady") + (where == null ? "" : " " + where) + ": "
                + verifiedVersions.seriesList();
    }

    /**
     * Text INFO do protokolu u pravidla, které nástroj používá, nebo null, když se verze neoznačuje. Například
     * „nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady: 1.29, 1.34); výsledky
     * kontroly se mohou lišit“.
     */
    public String protocolMessage(String utilName) {
        switch (status) {
            case UNVERIFIED:
                return String.format("nástroj %s je ve verzi %s, která nebyla s validátorem ověřena (%s); výsledky kontroly se mohou lišit",
                        utilName, version, seriesPhrase());
            case UNRECOGNIZED:
                return String.format("verzi nástroje %s nelze rozpoznat z výstupu „%s“ (%s); výsledky kontroly se mohou lišit",
                        utilName, shortDetectionText(), seriesPhrase());
            default:
                return null;
        }
    }

    /** Krátká hláška do protokolu (simpleMessage), nebo null. */
    public String protocolSimpleMessage() {
        switch (status) {
            case UNVERIFIED:
                return "neověřená verze nástroje";
            case UNRECOGNIZED:
                return "verzi nástroje nelze rozpoznat";
            default:
                return null;
        }
    }

    /**
     * Hodnota do XML protokolu (actualValue): u neověřené verze číslo verze, u nerozpoznané první řádek textu detekce
     * (zkrácený), jinak null.
     */
    public String protocolActualValue() {
        switch (status) {
            case UNVERIFIED:
                return version;
            case UNRECOGNIZED:
                return shortDetectionText();
            default:
                return null;
        }
    }

    /**
     * Poznámka k verzi do výpisu nástrojů v CLI (samostatný řádek pod řádkem s verzí), nebo null: u neověřené
     * „neověřená verze, ověřené řady: 1.29, 1.34“, u nerozpoznané „verzi nelze rozpoznat, ověřené řady: …“,
     * u verze ověřené na jiné platformě „ověřeno na macOS (řada 1.34)“. Ověřená verze poznámku nemá.
     */
    public String cliNote() {
        switch (status) {
            case UNVERIFIED:
                return "neověřená verze, " + seriesPhrase();
            case UNRECOGNIZED:
                return "verzi nelze rozpoznat, " + seriesPhrase();
            case VERIFIED_ON_OTHER_PLATFORM:
                return "ověřeno na " + platformName(verifiedVersions.getInheritedFrom()) + " (řada " + matchedSeries.getSeries() + ")";
            default:
                return null;
        }
    }

    /** První řádek textu detekce, zkrácený na {@link #MAX_DETECTION_TEXT} znaků. */
    String shortDetectionText() {
        String text = detectionText == null ? "" : detectionText.trim().split("\\R", 2)[0].trim();
        return text.length() <= MAX_DETECTION_TEXT ? text : text.substring(0, MAX_DETECTION_TEXT - 1).trim() + "…";
    }

    @Override
    public String toString() {
        return status + (version == null ? "" : " " + version) + (verifiedVersions == null ? "" : " " + verifiedVersions);
    }
}
