package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import java.io.File;
import java.util.Map;

/**
 * Texty a rozhodnutí pro dialog Externí nástroje (tabulka nástrojů): celkový stav, souhrn v patičce, text stavu
 * v řádku (zkrácený, celý v tooltipu), podrobnosti chyby.
 */
public final class ToolsOverview {

    /** Nejdelší text stavu v řádku tabulky, delší se zkrátí (celý je v tooltipu). */
    public static final int MAX_STATUS_LENGTH = 30;

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
            return "Kontroluji nástroje, hotovo " + done + " " + Summaries.fromPreposition(all) + " " + all + "…";
        }
        return available + " " + Summaries.fromPreposition(all) + " " + all + " nástrojů dostupných";
    }

    /** Text stavu do řádku tabulky, zkrácený na {@link #MAX_STATUS_LENGTH} znaků. */
    public static String statusText(ToolStatus status) {
        if (status.getState() == ToolStatus.State.CHECKING) {
            return "kontroluji…";
        }
        return truncate(status.getText(), MAX_STATUS_LENGTH);
    }

    /** Celý text stavu do tooltipu: u nalezeného nástroje celá verze, u nedostupného i výstup kontroly. */
    public static String statusTooltip(ToolStatus status) {
        String text = status.getState() == ToolStatus.State.CHECKING ? "kontroluji…" : status.getText();
        if (status.getDetail() != null && !status.getDetail().isEmpty()) {
            return text + ": " + status.getDetail();
        }
        return text;
    }

    /** Třída stavové tečky z base.css; nenalezený nástroj není chyba (nástroje jsou volitelné), proto šedá. */
    public static String dotStyleClass(ToolStatus status) {
        switch (status.getState()) {
            case FOUND:
                return "status-dot-ok";
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
