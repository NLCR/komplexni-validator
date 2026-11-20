package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.newvalidation.Summaries;

import java.util.List;
import java.util.Locale;

/**
 * Texty okna Nastavení bez JavaFX: poznámka o neuložených změnách, popisky v záložkách Přeskakovaná pravidla
 * a Slovníky.
 */
public final class SettingsTexts {

    private SettingsTexts() {
    }

    /** Části nastavení, které se ukládají až tlačítkem Uložit (v pořadí záložek). */
    public enum Part {
        VERSIONS("výchozí verze DMF"),
        SKIPPING("přeskakovaná pravidla"),
        DIRS("adresáře a protokoly");

        private final String label;

        Part(String label) {
            this.label = label;
        }
    }

    /** Poznámka v patičce: „Neuložené změny: výchozí verze DMF, přeskakovaná pravidla.“, null bez změn. */
    public static String unsavedNote(List<Part> changed) {
        if (changed == null || changed.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder("Neuložené změny: ");
        for (int i = 0; i < changed.size(); i++) {
            text.append(i == 0 ? "" : ", ").append(changed.get(i).label);
        }
        return text.append('.').toString();
    }

    // ===== přeskakovaná pravidla =====

    /** Položka výběru verze: „2.3“, s přeskočenými sekcemi „2.3 (přeskakují se 2)“. */
    public static String skippingVersionLabel(String version, int skippedCount) {
        if (skippedCount <= 0) {
            return version;
        }
        return version + " (" + Summaries.plural(skippedCount, "přeskakuje", "přeskakují", "přeskakuje") + " se " + skippedCount + ")";
    }

    /** Souhrn nad seznamem sekcí: „Přeskakují se 2 ze 14 sekcí.“ */
    public static String skippingSummary(int skipped, int total) {
        if (total <= 0) {
            return "Tato verze nemá žádné sekce.";
        }
        if (skipped <= 0) {
            return "Validují se všechny sekce (" + total + ").";
        }
        return capitalize(Summaries.plural(skipped, "přeskakuje", "přeskakují", "přeskakuje")) + " se " + skipped
                + " " + fromPreposition(total) + " " + total + " " + Summaries.plural(total, "sekce", "sekcí", "sekcí") + ".";
    }

    /**
     * „z“ nebo „ze“ před číslovkou ve 2. pádě: „ze“ před číslovkou začínající na s, š, z, ž nebo čt (ze dvou, tří,
     * čtyř, šesti, sedmi, čtrnácti, šestnácti, sedmnácti, čtyřiceti až čtyřiceti devíti, šedesáti až sedmdesáti
     * devíti, sta, dvou set, ...), jinak „z“ (z pěti, z dvanácti, z dvaceti). Pro tisíc a víc vždy „z“.
     */
    static String fromPreposition(int count) {
        if (count >= 1000 || count < 0) {
            return "z";
        }
        if (count >= 100) {
            int hundreds = count / 100;
            return hundreds == 1 || hundreds == 2 || hundreds == 3 || hundreds == 4 || hundreds == 6 || hundreds == 7 ? "ze" : "z";
        }
        if (count == 2 || count == 3 || count == 4 || count == 6 || count == 7 || count == 14 || count == 16 || count == 17
                || count >= 40 && count < 50 || count >= 60 && count < 80) {
            return "ze";
        }
        return "z";
    }

    // ===== slovníky =====

    /** Lidský název slovníku z popisu s velkým počátečním písmenem; bez popisu identifikátor slovníku. */
    public static String dictionaryTitle(String description, String id) {
        if (description == null || description.trim().isEmpty()) {
            return id;
        }
        return capitalize(description.trim());
    }

    /** „1 hodnota“, „3 hodnoty“, „250 hodnot“. */
    public static String dictionaryValues(int count) {
        return count + " " + Summaries.plural(count, "hodnota", "hodnoty", "hodnot");
    }

    /**
     * Popisek odkazu u slovníku: u slovníku vedeného na GitHubu (kódy institucí) vede odkaz na seznam, který tam lze
     * upravovat, u ostatních na specifikaci číselníku.
     */
    public static String dictionaryLinkLabel(String specUrl) {
        if (specUrl != null && specUrl.trim().toLowerCase(java.util.Locale.ROOT).startsWith("https://github.com/")) {
            return "Seznam na GitHubu";
        }
        return "Specifikace";
    }

    /** Datum poslední aktualizace slovníku, nebo null, když se slovník neaktualizoval (nebo aktualizovat nejde). */
    public static String dictionaryLastSync(String syncUrl, String syncDate) {
        if (syncUrl == null || syncUrl.trim().isEmpty()) {
            return null;
        }
        if (syncDate == null || syncDate.trim().isEmpty()) {
            return "zatím neaktualizováno";
        }
        return "aktualizováno " + syncDate.trim();
    }

    private static String capitalize(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(new Locale("cs")) + text.substring(1);
    }
}
