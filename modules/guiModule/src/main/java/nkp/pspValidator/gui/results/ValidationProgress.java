package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Průběh validace pro panel průběhu v hlavním okně: podíl hotových pravidel, „Sekce 4 z 12: Obrazová data“,
 * „pravidlo 3 z 8, celkem 37 z 210“, název běžícího pravidla a průběžné počty nalezených problémů. Vychází jen
 * z událostí jádra ({@code ValidationState.ProgressListener}): sekce a pravidla jsou známé z {@code onInitialization},
 * dál se hlásí začátek a konec sekce a pravidla. Jádro nehlásí zpracovávaný soubor ani průběh uvnitř pravidla.
 * <p>
 * Pořadí pravidel je pořadí, ve kterém je jádro spouští (sekce v pořadí seznamu, v sekci pravidla v pořadí seznamu).
 * Přeskočená sekce se počítá jako hotová (podíl postoupí o všechna její pravidla). Bez JavaFX, volá se z jednoho vlákna.
 */
public final class ValidationProgress {

    /** Nadpis panelu před prvním pravidlem (rozpoznání typu, sestavení validátoru, načtení balíku). */
    public static final String INITIALIZING_TITLE = "Inicializuji…";
    /** Podrobnost pod ukazatelem před prvním pravidlem. */
    public static final String INITIALIZING_DETAIL = "Rozpoznávám typ a verzi balíku a připravuji validátor.";
    /** Podíl, když ještě nejde určit (neurčitý ukazatel, odpovídá ProgressBar.INDETERMINATE_PROGRESS). */
    public static final double INDETERMINATE = -1;

    private final List<Integer> sectionOrder;
    private final Map<Integer, String> sectionNames;
    private final Map<Integer, List<Integer>> ruleIdsBySection;
    private final Map<Integer, Map<Integer, String>> ruleNamesBySection;
    /** počet pravidel v sekcích před danou sekcí (v pořadí spouštění) */
    private final Map<Integer, Integer> ruleOffsetBySection;
    private final int totalRules;

    private Integer currentSectionId;
    private Integer currentRuleId;
    private boolean ruleRunning;
    /** pořadí (od 1) posledního hotového pravidla v celkovém pořadí, 0 = žádné */
    private int completedRules;
    private ProblemCounts found = ProblemCounts.ZERO;

    private ValidationProgress(Builder builder) {
        this.sectionOrder = Collections.unmodifiableList(new ArrayList<>(builder.sections.keySet()));
        this.sectionNames = new HashMap<>(builder.sections);
        this.ruleIdsBySection = new HashMap<>();
        this.ruleNamesBySection = new HashMap<>();
        this.ruleOffsetBySection = new HashMap<>();
        int offset = 0;
        for (Integer sectionId : sectionOrder) {
            Map<Integer, String> rules = builder.rules.get(sectionId);
            ruleIdsBySection.put(sectionId, new ArrayList<>(rules.keySet()));
            ruleNamesBySection.put(sectionId, rules);
            ruleOffsetBySection.put(sectionId, offset);
            offset += rules.size();
        }
        this.totalRules = offset;
    }

    /**
     * Průběh ze sekcí a pravidel, jak je jádro ohlásí v {@code onInitialization} (klíče mapy mohou být kopie sekcí,
     * páruje se podle id).
     */
    public static ValidationProgress of(List<RulesSection> sections, Map<RulesSection, List<Rule>> rules) {
        Map<Integer, List<Rule>> rulesById = new HashMap<>();
        if (rules != null) {
            for (Map.Entry<RulesSection, List<Rule>> entry : rules.entrySet()) {
                rulesById.put(entry.getKey().getId(), entry.getValue());
            }
        }
        Builder builder = builder();
        for (RulesSection section : sections) {
            builder.section(section.getId(), section.getName());
            List<Rule> sectionRules = rulesById.get(section.getId());
            if (sectionRules != null) {
                for (Rule rule : sectionRules) {
                    builder.rule(rule.getId(), rule.getName());
                }
            }
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Sestavení průběhu: sekce a za každou její pravidla v pořadí spouštění. */
    public static final class Builder {
        private final LinkedHashMap<Integer, String> sections = new LinkedHashMap<>();
        private final Map<Integer, Map<Integer, String>> rules = new HashMap<>();
        private Integer lastSection;

        public Builder section(int id, String name) {
            sections.put(id, name);
            rules.put(id, new LinkedHashMap<>());
            lastSection = id;
            return this;
        }

        /** Pravidlo poslední přidané sekce. */
        public Builder rule(int id, String name) {
            if (lastSection == null) {
                throw new IllegalStateException("pravidlo bez sekce");
            }
            rules.get(lastSection).put(id, name);
            return this;
        }

        public ValidationProgress build() {
            return new ValidationProgress(this);
        }
    }

    // ===== udalosti jadra =====

    public void sectionStarted(int sectionId) {
        currentSectionId = sectionId;
        currentRuleId = null;
        ruleRunning = false;
    }

    /** Přeskočená sekce: všechna její pravidla se počítají jako hotová. */
    public void sectionSkipped(int sectionId) {
        Integer offset = ruleOffsetBySection.get(sectionId);
        if (offset != null) {
            completedRules = Math.max(completedRules, offset + ruleIdsBySection.get(sectionId).size());
        }
    }

    public void ruleStarted(int sectionId, int ruleId) {
        currentSectionId = sectionId;
        currentRuleId = ruleId;
        ruleRunning = true;
    }

    public void ruleFinished(int sectionId, int ruleId, ProblemCounts ruleProblems) {
        int position = globalPosition(sectionId, ruleId);
        if (position > 0) {
            completedRules = Math.max(completedRules, position);
        }
        if (ruleProblems != null) {
            found = found.plus(ruleProblems);
        }
        if (currentRuleId != null && currentRuleId == ruleId && currentSectionId != null && currentSectionId == sectionId) {
            ruleRunning = false;
        }
    }

    // ===== stav =====

    /** Už začala první sekce (do té doby běží inicializace). */
    public boolean isStarted() {
        return currentSectionId != null;
    }

    /**
     * Podíl hotových pravidel 0 až 1, {@link #INDETERMINATE} před první sekcí. Bez pravidel je hotovo hned po začátku.
     */
    public double getFraction() {
        if (!isStarted()) {
            return INDETERMINATE;
        }
        if (totalRules == 0) {
            return 1;
        }
        return Math.min(1.0, completedRules / (double) totalRules);
    }

    /** „Sekce 4 z 12: Obrazová data“, před první sekcí {@link #INITIALIZING_TITLE}. */
    public String getTitle() {
        if (!isStarted()) {
            return INITIALIZING_TITLE;
        }
        int index = sectionOrder.indexOf(currentSectionId) + 1;
        return "Sekce " + index + " z " + sectionOrder.size() + ": " + sectionNames.get(currentSectionId);
    }

    /**
     * „pravidlo 3 z 8, celkem 37 z 210“ pro běžící (nebo naposledy spuštěné) pravidlo; po začátku sekce před jejím
     * prvním pravidlem „pravidel v sekci: 8, celkem 210“; před první sekcí null.
     */
    public String getCountsText() {
        if (!isStarted()) {
            return null;
        }
        List<Integer> ruleIds = ruleIdsBySection.get(currentSectionId);
        int inSection = ruleIds == null ? 0 : ruleIds.size();
        if (currentRuleId == null || ruleIds == null) {
            return "pravidel v sekci: " + inSection + ", celkem " + totalRules;
        }
        int index = ruleIds.indexOf(currentRuleId) + 1;
        return "pravidlo " + index + " z " + inSection + ", celkem " + globalPosition(currentSectionId, currentRuleId) + " z " + totalRules;
    }

    /**
     * Název běžícího pravidla; mezi pravidly název naposledy spuštěného; před prvním pravidlem
     * {@link #INITIALIZING_DETAIL}, po začátku sekce před jejím prvním pravidlem null.
     */
    public String getDetail() {
        if (!isStarted()) {
            return INITIALIZING_DETAIL;
        }
        Map<Integer, String> names = ruleNamesBySection.get(currentSectionId);
        if (currentRuleId == null || names == null) {
            return null;
        }
        return names.get(currentRuleId);
    }

    /** Pravidlo právě běží (mezi koncem jednoho a začátkem dalšího ne). */
    public boolean isRuleRunning() {
        return ruleRunning;
    }

    /** Průběžné počty problémů ze všech dosud dokončených pravidel. */
    public ProblemCounts getFound() {
        return found;
    }

    public int getTotalRules() {
        return totalRules;
    }

    public int getTotalSections() {
        return sectionOrder.size();
    }

    /** Pořadí (od 1) posledního hotového pravidla v celkovém pořadí. */
    public int getCompletedRules() {
        return completedRules;
    }

    /** Pořadí pravidla (od 1) v celkovém pořadí spouštění, 0 pro neznámé pravidlo. */
    private int globalPosition(int sectionId, int ruleId) {
        List<Integer> ruleIds = ruleIdsBySection.get(sectionId);
        if (ruleIds == null) {
            return 0;
        }
        int index = ruleIds.indexOf(ruleId);
        return index < 0 ? 0 : ruleOffsetBySection.get(sectionId) + index + 1;
    }
}
