package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.gui.results.ResultTexts;

/**
 * Texty nad podrobnými výsledky balíku vybraného v tabulce skupiny: řádek se souhrnem balíku a hláška místo výsledků,
 * když je balík nemá. Bez JavaFX.
 */
public final class GroupDetailTexts {

    private GroupDetailTexts() {
    }

    /**
     * Souhrn vybraného balíku: „Monografie 2.2, nevalidní, 12 chyb, 3 varování, 5 informací, trvalo 3,4 s“; u chyby
     * na konci hláška.
     */
    public static String subtitle(GroupItem item) {
        StringBuilder text = new StringBuilder();
        if (item.getDmf() != null) {
            text.append(item.getTypeAndVersion()).append(", ");
        }
        text.append(item.getStatus().getLabel());
        ProblemCounts counts = item.getCounts();
        if (counts != null) {
            text.append(", ").append(ResultTexts.errors(counts.getErrors()))
                    .append(", ").append(ResultTexts.warnings(counts.getWarnings()))
                    .append(", ").append(ResultTexts.infos(counts.getInfos()));
        }
        if (item.getDurationMs() != null) {
            text.append(", trvalo ").append(ResultTexts.duration(item.getDurationMs()));
        }
        if (item.getMessage() != null) {
            text.append(": ").append(item.getMessage());
        }
        return text.toString();
    }

    /**
     * Hláška místo sekcí, pravidel a problémů.
     *
     * @param hasDetail podrobné výsledky balíku jsou v paměti (pak se hláška nezobrazuje)
     * @param evicted   podrobnosti byly kvůli paměti zahozeny
     * @return hláška, nebo null, když se mají zobrazit podrobné výsledky
     */
    public static String message(GroupItem item, boolean hasDetail, boolean evicted) {
        if (hasDetail) {
            return null;
        }
        if (evicted) {
            return "Podrobné výsledky tohoto balíku byly uvolněny z paměti, protože skupina má hodně problémů. Najdete je "
                    + "v protokolu XML a textovém logu (pokud se vytvářely), nebo balík validujte znovu přes nabídku pravým tlačítkem.";
        }
        switch (item.getStatus()) {
            case WAITING:
                return "Balík čeká na validaci.";
            case RUNNING:
                return item.isZip() ? "Rozbaluji soubor ZIP a připravuji validaci balíku…" : "Připravuji validaci balíku…";
            case ERROR:
                return "Balík nešlo validovat: " + (item.getMessage() == null ? "podrobnosti jsou ve výpisu aplikace." : item.getMessage());
            case NOT_VALIDATED:
                return "Balík se nevalidoval, validace skupiny byla zrušena dřív, než na něj došla řada.";
            case CANCELED:
                return "Validace balíku byla zrušena před kontrolou prvního pravidla.";
            default:
                return "Podrobné výsledky balíku nejsou k dispozici.";
        }
    }
}
