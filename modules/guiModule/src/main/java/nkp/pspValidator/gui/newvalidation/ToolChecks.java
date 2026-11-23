package nkp.pspValidator.gui.newvalidation;

import javafx.application.Platform;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Kontrola dostupnosti externích nástrojů pro dialogy Nová validace a Externí nástroje. Každý nástroj se zkontroluje
 * v pracovním vlákně detekcí verze ({@link ExternalTools#check}) a výsledek se předá do vlákna UI. Tam se zapíše
 * dostupnost a verze do {@link ExternalUtilManager} (validace u nedostupného nástroje a u neověřené verze hlásí INFO)
 * a ohlásí se posluchači, ale jen pro poslední spuštěnou kontrolu daného nástroje: zastaralý výsledek (nástroj
 * se mezitím kontroluje znovu, například po změně cesty) se zahodí. Do manažeru, který drží dostupnost v obyčejné mapě, se tak zapisuje jen
 * z jednoho vlákna.
 * <p>
 * Metody a stav ({@link #getStatuses()}) používat jen z vlákna UI.
 */
public class ToolChecks {

    /** Posluchač platných výsledků kontroly (volá se ve vlákně UI). */
    public interface Listener {
        void onResult(ExternalUtil util, ToolStatus status);
    }

    private final ExternalUtilManager manager;
    private final Function<ExternalUtil, ToolStatus> checker;
    private final Executor workers;
    private final Executor ui;
    private final Listener listener;
    private final Map<ExternalUtil, ToolStatus> statuses = new EnumMap<>(ExternalUtil.class);
    private final Map<ExternalUtil, Long> generations = new EnumMap<>(ExternalUtil.class);

    /**
     * Pořadí naposledy spuštěné kontroly každého nástroje, společné pro všechny instance nad stejným manažerem.
     * Manažer je sdílený mezi dialogy: bez toho by pomalá kontrola z jednoho dialogu přepsala dostupnost zjištěnou
     * novější kontrolou z druhého (například po změně cesty k nástroji).
     */
    private static final Map<ExternalUtilManager, Map<ExternalUtil, Long>> LATEST_STARTED = new WeakHashMap<>();
    private static long startCounter = 0;

    private static synchronized long registerStart(ExternalUtilManager manager, ExternalUtil util) {
        long order = ++startCounter;
        LATEST_STARTED.computeIfAbsent(manager, m -> new EnumMap<>(ExternalUtil.class)).put(util, order);
        return order;
    }

    private static synchronized boolean isLatestStarted(ExternalUtilManager manager, ExternalUtil util, long order) {
        Map<ExternalUtil, Long> latest = LATEST_STARTED.get(manager);
        return latest != null && latest.get(util) != null && latest.get(util) == order;
    }

    /**
     * Nástroje, jejichž dostupnost už některá kontrola do manažeru zapsala. Manažer jádra má do první kontroly všechny
     * nástroje „dostupné“, podle tohoto seznamu se pozná, že jde jen o výchozí hodnotu.
     */
    private static final Map<ExternalUtilManager, Set<ExternalUtil>> CHECKED = new WeakHashMap<>();

    /** Posluchači zápisu dostupnosti do manažeru (hlavní okno, prázdný stav), volají se ve vlákně UI. */
    private static final Map<ExternalUtilManager, List<Runnable>> AVAILABILITY_LISTENERS = new WeakHashMap<>();

    private static synchronized void markChecked(ExternalUtilManager manager, ExternalUtil util) {
        CHECKED.computeIfAbsent(manager, m -> EnumSet.noneOf(ExternalUtil.class)).add(util);
    }

    /** Zapsala už některá kontrola dostupnost nástroje {@code util} do manažeru? */
    public static synchronized boolean isChecked(ExternalUtilManager manager, ExternalUtil util) {
        Set<ExternalUtil> checked = CHECKED.get(manager);
        return checked != null && checked.contains(util);
    }

    /**
     * Posluchač, který se zavolá (ve vlákně UI) po každém zápisu dostupnosti nástroje do manažeru, z kterékoli
     * kontroly nad ním.
     */
    public static synchronized void addAvailabilityListener(ExternalUtilManager manager, Runnable listener) {
        AVAILABILITY_LISTENERS.computeIfAbsent(manager, m -> new ArrayList<>()).add(listener);
    }

    private static synchronized List<Runnable> availabilityListeners(ExternalUtilManager manager) {
        List<Runnable> listeners = AVAILABILITY_LISTENERS.get(manager);
        return listeners == null ? Collections.<Runnable>emptyList() : new ArrayList<>(listeners);
    }

    /**
     * @param threadName základ názvu pracovních vláken (každá kontrola běží v novém vlákně démona)
     */
    public ToolChecks(ExternalUtilManager manager, String threadName, Listener listener) {
        this(manager, util -> ExternalTools.check(manager, util), daemonThreads(threadName), Platform::runLater, listener);
    }

    ToolChecks(ExternalUtilManager manager, Function<ExternalUtil, ToolStatus> checker, Executor workers, Executor ui,
               Listener listener) {
        this.manager = manager;
        this.checker = checker;
        this.workers = workers;
        this.ui = ui;
        this.listener = listener;
    }

    private static Executor daemonThreads(String threadName) {
        return task -> {
            Thread thread = new Thread(task, threadName);
            thread.setDaemon(true);
            thread.start();
        };
    }

    /** Spustí kontrolu všech nástrojů; jejich stav je do výsledku „zjišťuji“. */
    public void checkAll() {
        for (ExternalUtil util : ExternalUtil.values()) {
            check(util);
        }
    }

    /** Spustí kontrolu jednoho nástroje; výsledek dřív spuštěné a dosud nedokončené kontroly se zahodí. */
    public void check(ExternalUtil util) {
        long generation = generations.getOrDefault(util, 0L) + 1;
        generations.put(util, generation);
        statuses.put(util, ToolStatus.checking());
        long order = registerStart(manager, util);
        workers.execute(() -> {
            ToolStatus status;
            try {
                status = checker.apply(util);
            } catch (Throwable e) {
                //bez vysledku by nastroj zustal navzdy ve stavu "zjistuji"
                e.printStackTrace();
                status = ToolStatus.error(e.getMessage() != null ? e.getMessage() : e.toString());
            }
            ToolStatus result = status;
            ui.execute(() -> deliver(util, generation, order, result));
        });
    }

    private void deliver(ExternalUtil util, long generation, long order, ToolStatus status) {
        if (generations.get(util) != generation) {
            return;
        }
        boolean written = isLatestStarted(manager, util, order);
        if (written) {
            manager.setUtilAvailable(util, status.isAvailable());
            //podle zapsane verze validace u pravidel nastroje ohlasi neoverenou verzi (INFO)
            manager.setUtilVersion(util, status.isAvailable() ? status.getVersion() : null);
            markChecked(manager, util);
        }
        statuses.put(util, status);
        listener.onResult(util, status);
        if (written) {
            for (Runnable availabilityListener : availabilityListeners(manager)) {
                availabilityListener.run();
            }
        }
    }

    /** Stav nástroje; před první kontrolou „zjišťuji“. */
    public ToolStatus getStatus(ExternalUtil util) {
        return statuses.getOrDefault(util, ToolStatus.checking());
    }

    /** Stavy nástrojů, pro které už byla spuštěna kontrola (pohled jen pro čtení, mění se s dalšími výsledky). */
    public Map<ExternalUtil, ToolStatus> getStatuses() {
        return Collections.unmodifiableMap(statuses);
    }
}
