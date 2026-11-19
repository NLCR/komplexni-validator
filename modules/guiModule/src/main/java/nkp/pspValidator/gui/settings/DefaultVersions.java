package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.shared.Dmf;

import java.util.*;

/**
 * Záložka „Výchozí verze DMF“ v Nastavení: pro každý typ dokumentu verze, proti které se validuje balík, který verzi
 * neuvádí (preferovaná verze, klíče {@code prefer.*.version.enabled/code}). Volba „výchozí validátoru (X)“ preferování
 * vypne, konkrétní verze ho zapne. Změny se drží v konceptu a uloží až {@link #save()} (tlačítko Uložit).
 * <p>
 * Výběr odpovídající uloženému stavu se určuje stejně jako při validaci ({@link VersionSettings#preferredVersion}):
 * vypnuté preferování = výchozí verze validátoru; zapnuté = uložená verze, a pokud není k dispozici, nejnovější
 * dostupná. Nedotčený typ se při uložení nepřepisuje, takže uložené hodnoty zůstanou, jak byly.
 */
public class DefaultVersions {

    /** Položka výběru verze; verze null znamená „výchozí validátoru“. */
    public static final class Choice {
        public final String version;
        public final String label;

        public Choice(String version, String label) {
            this.version = version;
            this.label = label;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Choice)) {
                return false;
            }
            Choice other = (Choice) o;
            return Objects.equals(version, other.version) && Objects.equals(label, other.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, label);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final VersionSettings settings;
    private final Map<Dmf.Type, String> initial = new EnumMap<>(Dmf.Type.class);
    private final Map<Dmf.Type, String> current = new EnumMap<>(Dmf.Type.class);
    private final Map<Dmf.Type, String> staleNotes = new EnumMap<>(Dmf.Type.class);
    private final Set<Dmf.Type> touched = EnumSet.noneOf(Dmf.Type.class);

    public DefaultVersions(VersionSettings settings) {
        this.settings = settings;
        for (Dmf.Type type : Dmf.Type.values()) {
            boolean enabled = settings.isPreferenceEnabled(type);
            String stored = settings.storedPreferredCode(type);
            List<String> available = settings.availableVersions(type);
            String selection = initialSelection(enabled, stored, available);
            initial.put(type, selection);
            current.put(type, selection);
            String stale = staleNote(enabled, stored, available);
            if (stale != null) {
                staleNotes.put(type, stale);
            }
        }
    }

    // ===== čisté funkce (testované samostatně) =====

    /** Položky výběru: nejdřív „výchozí validátoru (X)“, pak dostupné verze v předaném pořadí (od nejnovější). */
    public static List<Choice> choices(String defaultVersion, List<String> available) {
        List<Choice> result = new ArrayList<>();
        result.add(new Choice(null, "výchozí validátoru (" + defaultVersion + ")"));
        for (String version : available) {
            result.add(new Choice(version, version));
        }
        return result;
    }

    /**
     * Výběr odpovídající uloženému stavu: null (výchozí validátoru), když je preferování vypnuté nebo pro typ není
     * žádná verze; jinak uložená verze, pokud je k dispozici, a když není, nejnovější dostupná (stejně jako při validaci).
     */
    public static String initialSelection(boolean enabled, String stored, List<String> available) {
        if (!enabled) {
            return null;
        }
        return VersionSettings.effectiveCode(stored, available);
    }

    /** Upozornění na uloženou verzi, která se nepoužije (není k dispozici nebo chybí), nebo null. */
    public static String staleNote(boolean enabled, String stored, List<String> available) {
        if (!enabled || available.isEmpty()) {
            return null;
        }
        if (stored == null || stored.trim().isEmpty()) {
            return "Uložená verze chybí, použije se nejnovější " + available.get(0) + ".";
        }
        if (!available.contains(stored)) {
            return "Uložená verze " + stored + " není k dispozici, použije se nejnovější " + available.get(0) + ".";
        }
        return null;
    }

    /** Verze, která se použije pro balík bez uvedené verze. */
    public static String usedVersion(String selection, String defaultVersion) {
        return selection != null ? selection : defaultVersion;
    }

    /**
     * Poznámka k řádku: chybějící fDMF typu, upozornění na uloženou verzi (dokud uživatel řádek nezmění), nebo
     * výchozí verze validátoru, pro kterou není fDMF. Jinak null.
     */
    public static String note(String selection, String defaultVersion, List<String> available, String staleNote, boolean touched) {
        if (available.isEmpty()) {
            return "Pro tento typ není k dispozici žádná verze fDMF.";
        }
        if (staleNote != null && !touched) {
            return staleNote;
        }
        if (selection == null && !available.contains(defaultVersion)) {
            return "Výchozí verze validátoru " + defaultVersion + " není k dispozici.";
        }
        return null;
    }

    // ===== koncept =====

    public List<Choice> choices(Dmf.Type type) {
        return choices(DocumentTypes.defaultVersion(type), settings.availableVersions(type));
    }

    /** Vybraná položka (jedna z {@link #choices(Dmf.Type)}). */
    public Choice selected(Dmf.Type type) {
        String selection = current.get(type);
        for (Choice choice : choices(type)) {
            if (Objects.equals(choice.version, selection)) {
                return choice;
            }
        }
        return choices(type).get(0);
    }

    public boolean hasVersions(Dmf.Type type) {
        return !settings.availableVersions(type).isEmpty();
    }

    /** Vybere verzi (null = výchozí validátoru); verze mimo dostupné se ignoruje. */
    public void select(Dmf.Type type, String version) {
        if (version != null && !settings.availableVersions(type).contains(version)) {
            return;
        }
        current.put(type, version);
        touched.add(type);
    }

    /** „Vrátit výchozí hodnoty validátoru“: všechny typy na výchozí verzi validátoru. */
    public void resetAll() {
        for (Dmf.Type type : Dmf.Type.values()) {
            select(type, null);
        }
    }

    public String usedVersion(Dmf.Type type) {
        return usedVersion(current.get(type), DocumentTypes.defaultVersion(type));
    }

    public String note(Dmf.Type type) {
        return note(current.get(type), DocumentTypes.defaultVersion(type), settings.availableVersions(type),
                staleNotes.get(type), touched.contains(type));
    }

    /** Typ se při uložení přepíše: změnil se výběr, nebo uživatel potvrdil volbou uloženou verzi, která neplatí. */
    private boolean isDirty(Dmf.Type type) {
        if (!touched.contains(type)) {
            return false;
        }
        return !Objects.equals(current.get(type), initial.get(type)) || staleNotes.containsKey(type);
    }

    public boolean isChanged() {
        for (Dmf.Type type : Dmf.Type.values()) {
            if (isDirty(type)) {
                return true;
            }
        }
        return false;
    }

    /** Uloží změněné typy do nastavení (stejné klíče jako dřív, {@link VersionSettings#setPreferredVersion}). */
    public void save() {
        for (Dmf.Type type : Dmf.Type.values()) {
            if (isDirty(type)) {
                settings.setPreferredVersion(type, current.get(type));
            }
        }
    }
}
