package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.VersionComparator;
import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.newvalidation.Recognition;
import nkp.pspValidator.gui.newvalidation.Summaries;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.shared.Dmf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Texty dialogu Nová validace pro skupinu balíků: karta rozpoznání (počet balíků a rozpad podle typu a verze) a souhrny
 * sbalených sekcí. Verze v rozpadu je ta, proti které se bude validovat (vynucená, z balíku, preferovaná nebo výchozí,
 * stejně jako u jednoho balíku). Bez JavaFX.
 */
public final class GroupSummaries {

    /** Řazení fDMF: podle pořadí typu ve výčtu, u stejného typu od nejnovější verze. */
    public static final Comparator<Dmf> DMF_ORDER = Comparator.<Dmf, Integer>comparing(dmf -> dmf.getType().ordinal())
            .thenComparing(Dmf::getVersion, new VersionComparator(true));

    private GroupSummaries() {
    }

    /** Nadpis karty během rozpoznávání: „Rozpoznávám balíky… 3 z 12“. */
    public static String recognizingTitle(int done, int toRecognize) {
        return toRecognize <= 0 ? "Hledám balíky…" : "Rozpoznávám balíky… " + done + " z " + toRecognize;
    }

    /** „Nalezen 1 balík“, „Nalezeny 3 balíky“, „Nalezeno 12 balíků“, „Adresář neobsahuje žádný balík“. */
    public static String title(GroupRecognition group) {
        if (!group.isOk()) {
            return "Adresář nelze přečíst";
        }
        int n = group.getPackageCount();
        if (n == 0) {
            return "Adresář neobsahuje žádný balík";
        }
        return Summaries.plural(n, "Nalezen", "Nalezeny", "Nalezeno") + " " + packages(n);
    }

    /** „1 balík“, „3 balíky“, „12 balíků“. */
    public static String packages(int count) {
        return count + " " + Summaries.plural(count, "balík", "balíky", "balíků");
    }

    /**
     * Rozpad rozpoznaných balíků: „9× Monografie 2.2, 2× Periodika 2.1, 1 nerozpoznán“. Pořadí podle počtu (sestupně),
     * při shodě podle typu a verze; nerozpoznané balíky jsou vždy na konci. Null, když není co ukázat.
     */
    public static String breakdown(GroupRecognition group, VersionSettings versions) {
        if (!group.isOk() || group.getRecognitions().isEmpty()) {
            return null;
        }
        Map<Dmf, Integer> byDmf = new LinkedHashMap<>();
        Map<Dmf.Type, Integer> withoutVersion = new LinkedHashMap<>();
        int notRecognized = 0;
        for (Recognition recognition : group.getRecognitions()) {
            if (!recognition.isRecognized()) {
                notRecognized++;
                continue;
            }
            Dmf dmf = Summaries.effectiveDmf(recognition, versions);
            if (dmf == null) {
                withoutVersion.merge(recognition.getType(), 1, Integer::sum);
            } else {
                byDmf.merge(dmf, 1, Integer::sum);
            }
        }
        List<Object[]> parts = new ArrayList<>();
        for (Map.Entry<Dmf, Integer> entry : byDmf.entrySet()) {
            Dmf dmf = entry.getKey();
            parts.add(new Object[]{entry.getValue(), dmf.getType().ordinal(), dmf.getVersion(),
                    entry.getValue() + "× " + DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion()});
        }
        for (Map.Entry<Dmf.Type, Integer> entry : withoutVersion.entrySet()) {
            parts.add(new Object[]{entry.getValue(), entry.getKey().ordinal(), "",
                    entry.getValue() + "× " + DocumentTypes.name(entry.getKey()) + " (verzi nelze zjistit)"});
        }
        VersionComparator newestFirst = new VersionComparator(true);
        parts.sort(Comparator.<Object[], Integer>comparing(p -> -(Integer) p[0])
                .thenComparing(p -> (Integer) p[1])
                .thenComparing(p -> (String) p[2], (a, b) -> a.isEmpty() || b.isEmpty() ? a.compareTo(b) : newestFirst.compare(a, b)));
        StringBuilder text = new StringBuilder();
        for (Object[] part : parts) {
            text.append(text.length() == 0 ? "" : ", ").append((String) part[3]);
        }
        if (notRecognized > 0) {
            text.append(text.length() == 0 ? "" : ", ").append(notRecognized).append(' ')
                    .append(Summaries.plural(notRecognized, "nerozpoznán", "nerozpoznány", "nerozpoznáno"));
        }
        return text.toString();
    }

    /** Věta pod nadpisem karty: jak se bude validovat, ignorované soubory, limit rozpoznávání. */
    public static String detail(GroupRecognition group) {
        if (!group.isOk()) {
            return group.getError();
        }
        StringBuilder text = new StringBuilder();
        if (group.getPackageCount() == 0) {
            text.append("Balíkem je každý podadresář a soubor ZIP přímo v adresáři.");
        } else {
            text.append("Každý balík se validuje podle svého typu a verze, postupně jeden po druhém.");
        }
        int ignored = group.getIgnored().size();
        if (ignored > 0) {
            text.append(" Ignorováno: ").append(ignored).append(' ')
                    .append(Summaries.plural(ignored, "soubor, který není", "soubory, které nejsou", "souborů, které nejsou"))
                    .append(" ZIP (").append(group.getIgnored().get(0)).append(ignored > 1 ? ", …)." : ").");
        }
        int skipped = group.getNotRecognizedDueToLimit();
        if (skipped > 0) {
            text.append(" Rozpoznáno prvních ").append(group.getRecognitions().size()).append(" z ").append(group.getPackageCount())
                    .append(", ostatní se rozpoznají až při validaci.");
        }
        return text.toString();
    }

    /** fDMF, proti kterým se budou rozpoznané balíky validovat (bez duplicit, seřazené podle typu a verze). */
    public static Set<Dmf> effectiveDmfs(GroupRecognition group, VersionSettings versions) {
        Set<Dmf> result = new TreeSet<>(DMF_ORDER);
        if (group == null) {
            return result;
        }
        for (Recognition recognition : group.getRecognitions()) {
            Dmf dmf = Summaries.effectiveDmf(recognition, versions);
            if (dmf != null) {
                result.add(dmf);
            }
        }
        return result;
    }

    /** Souhrn sekce Verze DMF u skupiny: „podle balíků, nic není vynuceno“, „vynuceno u 2 typů“. */
    public static String versionsSummary(VersionSettings versions) {
        int count = versions.forcedTypesCount();
        if (count == 0) {
            return "podle balíků, nic není vynuceno";
        }
        return "vynuceno u " + count + " " + Summaries.plural(count, "typu", "typů", "typů");
    }

    /**
     * Souhrn sekce Přeskočená pravidla u skupiny.
     *
     * @param skippedByDmf přeskakované sekce pro každou fDMF ve skupině (hodnota null: fDMF není k dispozici)
     */
    public static String skippedSummary(Map<Dmf, List<String>> skippedByDmf) {
        if (skippedByDmf == null || skippedByDmf.isEmpty()) {
            return "určí se podle typů a verzí balíků";
        }
        int total = 0;
        int dmfs = 0;
        for (List<String> skipped : skippedByDmf.values()) {
            if (skipped != null && !skipped.isEmpty()) {
                total += skipped.size();
                dmfs++;
            }
        }
        if (total == 0) {
            return "žádná";
        }
        String sections = total + " " + Summaries.plural(total, "celá sekce", "celé sekce", "celých sekcí");
        if (dmfs == 1) {
            for (Map.Entry<Dmf, List<String>> entry : skippedByDmf.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    return sections + " (" + DocumentTypes.name(entry.getKey().getType()) + " " + entry.getKey().getVersion() + ")";
                }
            }
        }
        return sections + " (" + dmfs + " " + Summaries.plural(dmfs, "verze", "verze", "verzí") + " DMF)";
    }
}
