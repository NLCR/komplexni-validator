package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.newvalidation.Summaries;
import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.gui.results.ResultTexts;
import nkp.pspValidator.shared.Dmf;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stav validace skupiny balíků: balíky a jejich stavy, který balík právě běží, souhrnné počty a texty horní lišty
 * a panelu průběhu. Balíky se validují postupně; chyba jednoho balíku běh nezastaví (balík dostane stav „chyba“),
 * zrušení zruší běžící balík a zbylé označí „nevalidováno“. Jeden běh („sezení“) validuje buď celou skupinu, nebo jen
 * vybrané balíky („Validovat znovu jen tento balík“); výsledky ostatních balíků zůstávají.
 * <p>
 * Mění se jen z vlákna UI. Bez JavaFX.
 */
public class GroupRun {

    private final File groupDir;
    private final File source;
    private final List<GroupItem> items;
    private final List<String> ignored;

    /** balíky aktuálního (nebo posledního) běhu v pořadí validace */
    private List<GroupItem> session = Collections.emptyList();
    private GroupItem current;
    private boolean running;
    private boolean canceled;
    private long sessionStartedAt;
    /** součet dob všech běhů nad touto skupinou */
    private long totalElapsedMs;
    private LocalDateTime finishedAt;

    public GroupRun(GroupPackages.Listing listing) {
        this.groupDir = listing.getGroupDir();
        this.source = listing.getSource();
        List<GroupItem> list = new ArrayList<>();
        for (GroupPackages.Entry entry : listing.getPackages()) {
            list.add(new GroupItem(list.size(), entry));
        }
        this.items = Collections.unmodifiableList(list);
        this.ignored = listing.getIgnored();
    }

    /** Adresář, ve kterém se balíky validují (u skupiny v ZIPu v rozbaleném archivu). */
    public File getGroupDir() {
        return groupDir;
    }

    /** Co uživatel vybral: adresář skupiny, nebo soubor ZIP se skupinou (nadpis, stavový řádek, bublina). */
    public File getSource() {
        return source;
    }

    /** Skupina ze souboru ZIP. */
    public boolean isFromZip() {
        return !source.equals(groupDir);
    }

    /** Název adresáře skupiny, nebo souboru ZIP se skupinou (nadpis horní lišty). */
    public String getTitle() {
        return source.getName();
    }

    /** Bublina u nadpisu: „Adresář skupiny: …“, „Soubor ZIP se skupinou: …“. */
    public String sourceDescription() {
        return (isFromZip() ? "Soubor ZIP se skupinou: " : "Adresář skupiny: ") + source.getAbsolutePath();
    }

    /** Výchozí název souboru exportu CSV: „davka-souhrn.csv“ (u ZIPu bez přípony .zip). */
    public String csvFileName() {
        String name = source.getName();
        if (isFromZip() && GroupPackages.isZipName(name)) {
            name = name.substring(0, name.length() - ".zip".length());
        }
        return name + "-souhrn.csv";
    }

    public List<GroupItem> getItems() {
        return items;
    }

    public List<String> getIgnored() {
        return ignored;
    }

    public int total() {
        return items.size();
    }

    // ===== prubeh =====

    /**
     * Začíná běh nad danými balíky (celá skupina nebo jen některé). Jejich předchozí výsledky se zahodí.
     *
     * @throws IllegalStateException když běh už probíhá
     */
    public void runStarted(List<GroupItem> toRun, long now) {
        if (running) {
            throw new IllegalStateException("běh skupiny už probíhá");
        }
        for (GroupItem item : toRun) {
            if (!items.contains(item)) {
                throw new IllegalArgumentException("balík není ve skupině: " + item);
            }
            item.reset();
        }
        session = Collections.unmodifiableList(new ArrayList<>(toRun));
        current = null;
        running = true;
        canceled = false;
        sessionStartedAt = now;
        finishedAt = null;
    }

    public void packageStarted(GroupItem item) {
        checkInSession(item);
        current = item;
        item.setStatus(PackageStatus.RUNNING);
    }

    /** Typ a verze balíku jsou známé (jádro je rozpoznalo před sestavením validátoru). */
    public void packageResolved(GroupItem item, Dmf dmf) {
        checkInSession(item);
        item.setDmf(dmf);
    }

    /**
     * Validace balíku doběhla; verdikt a počty z jádra.
     */
    public void packageFinished(GroupItem item, Dmf dmf, ProblemCounts counts, boolean valid, long durationMs,
                                File xmlProtocol, File txtLog) {
        end(item, valid ? PackageStatus.VALID : PackageStatus.INVALID, dmf, counts, durationMs, null);
        item.setOutputs(xmlProtocol, txtLog);
    }

    /**
     * Balík nešlo validovat nebo validace skončila výjimkou.
     *
     * @param found počty z pravidel dokončených před chybou, null když se pravidla nenačetla
     */
    public void packageFailed(GroupItem item, Dmf dmf, ProblemCounts found, String message, long durationMs) {
        end(item, PackageStatus.ERROR, dmf, found, durationMs, message == null ? "Validace skončila chybou." : message);
    }

    /** Validace běžícího balíku byla zrušena. */
    public void packageCanceled(GroupItem item, Dmf dmf, ProblemCounts found, long durationMs) {
        end(item, PackageStatus.CANCELED, dmf, found, durationMs, null);
    }

    private void end(GroupItem item, PackageStatus status, Dmf dmf, ProblemCounts counts, long durationMs, String message) {
        checkInSession(item);
        item.setStatus(status);
        if (dmf != null) {
            item.setDmf(dmf);
        }
        item.setCounts(counts);
        item.setDurationMs(durationMs);
        item.setMessage(message);
        if (current == item) {
            current = null;
        }
    }

    /** Uživatel běh zrušil (běžící balík ukončí jádro, ostatní se už nespustí). */
    public void cancelRequested() {
        if (running) {
            canceled = true;
        }
    }

    /**
     * Běh skončil (všechny balíky prošly, nebo byl zrušen). Balíky, na které nedošla řada, dostanou stav
     * „nevalidováno“, stejně tak balík, který zůstal ve stavu „validuje se“ (zrušení před ohlášením výsledku).
     */
    public void runEnded(long now, LocalDateTime finishedAt) {
        for (GroupItem item : session) {
            if (item.getStatus() == PackageStatus.WAITING) {
                item.setStatus(PackageStatus.NOT_VALIDATED);
            } else if (item.getStatus() == PackageStatus.RUNNING) {
                item.setStatus(canceled ? PackageStatus.CANCELED : PackageStatus.NOT_VALIDATED);
            }
        }
        if (running) {
            totalElapsedMs += Math.max(0, now - sessionStartedAt);
        }
        running = false;
        current = null;
        this.finishedAt = finishedAt;
    }

    private void checkInSession(GroupItem item) {
        if (!session.contains(item)) {
            throw new IllegalArgumentException("balík není v aktuálním běhu: " + item);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isCanceled() {
        return canceled;
    }

    /** Právě validovaný balík, null mezi balíky a mimo běh. */
    public GroupItem getCurrent() {
        return current;
    }

    public List<GroupItem> getSession() {
        return session;
    }

    // ===== pocty =====

    public int count(PackageStatus status) {
        int count = 0;
        for (GroupItem item : items) {
            if (item.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    /** Balíky s výsledkem validace (validní, nevalidní nebo s chybou). */
    public int done() {
        return count(PackageStatus.VALID) + count(PackageStatus.INVALID) + count(PackageStatus.ERROR);
    }

    /**
     * Podíl hotové práce aktuálního běhu: hotové balíky běhu plus rozpracovaný podíl běžícího balíku, děleno počtem
     * balíků běhu. Běh bez balíků je hotový (1).
     *
     * @param currentFraction podíl hotových pravidel běžícího balíku (0 až 1), záporný = neznámý (počítá se 0)
     */
    public double fraction(double currentFraction) {
        if (session.isEmpty()) {
            return 1;
        }
        int finished = 0;
        for (GroupItem item : session) {
            if (item.getStatus().isDone()) {
                finished++;
            }
        }
        double partial = current == null || currentFraction < 0 ? 0 : Math.min(1, currentFraction);
        return Math.min(1, (finished + partial) / session.size());
    }

    /** Pořadí běžícího balíku v aktuálním běhu (od 1), 0 když žádný neběží. */
    public int currentPosition() {
        return current == null ? 0 : session.indexOf(current) + 1;
    }

    // ===== texty =====

    /**
     * Podtitul horní lišty: během běhu „Skupina 12 balíků, hotovo 7, běží 1:23“, po skončení „Skupina 12 balíků,
     * validováno 19. 11. 2026 14:02, trvalo 2 min 5 s“, po zrušení „Skupina 12 balíků, validace zrušena, trvalo 41,0 s“.
     *
     * @param now aktuální čas (pro uplynulou dobu během běhu)
     */
    public String subtitle(long now) {
        StringBuilder text = new StringBuilder("Skupina ").append(GroupSummaries.packages(total()));
        if (running) {
            text.append(", hotovo ").append(done()).append(", běží ").append(ResultTexts.elapsed(totalElapsedMs + now - sessionStartedAt));
        } else if (finishedAt == null) {
            text.append(", zatím nevalidováno");
        } else if (canceled) {
            text.append(", validace zrušena, trvalo ").append(ResultTexts.duration(totalElapsedMs));
        } else {
            text.append(", validováno ").append(ResultTexts.dateTime(finishedAt)).append(", trvalo ").append(ResultTexts.duration(totalElapsedMs));
        }
        return text.toString();
    }

    /** Nadpis panelu průběhu: „Balík 7 z 12: aba007-0001w8“, mezi balíky „Validuji skupinu…“. */
    public String progressTitle() {
        if (current == null) {
            return "Validuji skupinu…";
        }
        return "Balík " + currentPosition() + " z " + session.size() + ": " + current.getName();
    }

    /** Počty pro panel průběhu: „hotovo 6 z 12“. */
    public String progressCounts() {
        int finished = 0;
        for (GroupItem item : session) {
            if (item.getStatus().isDone()) {
                finished++;
            }
        }
        return "hotovo " + finished + " z " + session.size();
    }

    /** „9 validních“, „1 validní“. */
    public String validText() {
        int n = count(PackageStatus.VALID);
        return n + " " + Summaries.plural(n, "validní", "validní", "validních");
    }

    /** „2 nevalidní“, „5 nevalidních“. */
    public String invalidText() {
        int n = count(PackageStatus.INVALID);
        return n + " " + Summaries.plural(n, "nevalidní", "nevalidní", "nevalidních");
    }

    /** „1 s chybou“. */
    public String errorText() {
        return count(PackageStatus.ERROR) + " s chybou";
    }

    /** Balíky, které se ještě validovat budou (čekají nebo běží): „1 čeká“, „3 čekají“, „5 čeká“. */
    public String waitingText() {
        int n = count(PackageStatus.WAITING) + count(PackageStatus.RUNNING);
        return n + " " + Summaries.plural(n, "čeká", "čekají", "čeká");
    }

    /** Balíky bez výsledku po zrušení: „4 nevalidováno“ (zrušený běžící balík se počítá sem). */
    public String notValidatedText() {
        return (count(PackageStatus.NOT_VALIDATED) + count(PackageStatus.CANCELED)) + " nevalidováno";
    }

    /** Řádek stavového řádku po skončení běhu. */
    public String statusText() {
        if (canceled) {
            return "Validace skupiny " + source.getAbsolutePath() + " zrušena.";
        }
        return "Validace skupiny " + source.getAbsolutePath() + " hotova.";
    }
}
