package nkp.pspValidator.gui.group;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Hlídání paměti u velké skupiny: podrobné výsledky balíků (sekce, pravidla, problémy) se drží, dokud jejich součtová
 * váha (počet problémů a pravidel) nepřekročí rozpočet. Pak se zahazují podrobnosti balíků, na které se uživatel
 * nejdéle nedíval (nebo které nejdéle doběhly); zamčené balíky (běžící a zobrazený) se nezahazují nikdy. Souhrn
 * balíku (řádek tabulky, CSV, protokol a log na disku) zůstává vždy. Bez JavaFX.
 *
 * @param <K> klíč (balík)
 * @param <V> podrobné výsledky balíku
 */
public class DetailRetention<K, V> {

    /**
     * Výchozí rozpočet: 250 000 problémů a pravidel dohromady. Problém v paměti zabírá řádově stovky bajtů, takže
     * podrobnosti celé skupiny zaberou nejvýš desítky až nízké stovky MB.
     */
    public static final long DEFAULT_BUDGET = 250_000;

    private static final class Slot<V> {
        final V value;
        final long weight;

        Slot(V value, long weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private final long budget;
    /** pořadí přístupu: první je nejdéle nepoužitý */
    private final LinkedHashMap<K, Slot<V>> slots = new LinkedHashMap<>(16, 0.75f, true);
    private final Set<K> evicted = new HashSet<>();
    private final Set<K> pinned = new HashSet<>();
    private long totalWeight;

    public DetailRetention(long budget) {
        this.budget = budget;
    }

    /**
     * Uloží (nebo nahradí) podrobnosti balíku a případně zahodí nejdéle nepoužité podrobnosti jiných balíků.
     *
     * @param weight váha (počet problémů a pravidel), záporná se bere jako 0
     */
    public void put(K key, V value, long weight) {
        remove(key);
        long w = Math.max(0, weight);
        slots.put(key, new Slot<>(value, w));
        totalWeight += w;
        evicted.remove(key);
        evictOverBudget();
    }

    /** Změní váhu uložených podrobností (balík během běhu přibírá problémy). */
    public void updateWeight(K key, long weight) {
        Slot<V> slot = slots.get(key);
        if (slot == null) {
            return;
        }
        long w = Math.max(0, weight);
        slots.put(key, new Slot<>(slot.value, w));
        totalWeight += w - slot.weight;
        evictOverBudget();
    }

    /** Podrobnosti balíku, null když nejsou (ještě nevznikly nebo byly zahozeny); počítá se jako použití. */
    public V get(K key) {
        Slot<V> slot = slots.get(key);
        return slot == null ? null : slot.value;
    }

    /** Podrobnosti balíku byly kvůli rozpočtu zahozeny (a od té doby se neuložily znovu). */
    public boolean wasEvicted(K key) {
        return evicted.contains(key);
    }

    /** Zahodí podrobnosti balíku (nový běh balíku); nepočítá se jako zahození kvůli paměti. */
    public void remove(K key) {
        Slot<V> slot = slots.remove(key);
        if (slot != null) {
            totalWeight -= slot.weight;
        }
        evicted.remove(key);
    }

    public void clear() {
        slots.clear();
        evicted.clear();
        pinned.clear();
        totalWeight = 0;
    }

    /** Balíky, jejichž podrobnosti se nesmí zahodit (běžící, zobrazený); nahradí předchozí sadu. */
    public void setPinned(Set<K> keys) {
        pinned.clear();
        for (K key : keys) {
            if (key != null) {
                pinned.add(key);
            }
        }
        evictOverBudget();
    }

    public long getTotalWeight() {
        return totalWeight;
    }

    public int size() {
        return slots.size();
    }

    public Set<K> getEvicted() {
        return Collections.unmodifiableSet(evicted);
    }

    private void evictOverBudget() {
        Iterator<Map.Entry<K, Slot<V>>> it = slots.entrySet().iterator();
        while (totalWeight > budget && it.hasNext()) {
            Map.Entry<K, Slot<V>> entry = it.next();
            if (pinned.contains(entry.getKey())) {
                continue;
            }
            totalWeight -= entry.getValue().weight;
            evicted.add(entry.getKey());
            it.remove();
        }
    }
}
