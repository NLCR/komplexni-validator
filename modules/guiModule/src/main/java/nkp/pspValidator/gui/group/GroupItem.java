package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.shared.Dmf;

import java.io.File;

/**
 * Jeden balík skupiny a souhrn jeho poslední validace (řádek tabulky a CSV). Mění se jen ve vlákně UI přes
 * {@link GroupRun}. Podrobné výsledky (sekce, pravidla, problémy) tu nejsou, drží je hlavní okno zvlášť. Bez JavaFX.
 */
public final class GroupItem {

    private final int order;
    private final GroupPackages.Entry entry;

    private PackageStatus status = PackageStatus.WAITING;
    private Dmf dmf;
    private ProblemCounts counts;
    private Long durationMs;
    private File xmlProtocol;
    private File txtLog;
    private String message;

    /**
     * @param order pořadí ve skupině (od 0), podle něj se balíky validují a řadí při shodě
     */
    public GroupItem(int order, GroupPackages.Entry entry) {
        this.order = order;
        this.entry = entry;
    }

    /** Výsledky předchozí validace pryč, balík čeká (nový běh). */
    void reset() {
        status = PackageStatus.WAITING;
        dmf = null;
        counts = null;
        durationMs = null;
        xmlProtocol = null;
        txtLog = null;
        message = null;
    }

    public int getOrder() {
        return order;
    }

    public GroupPackages.Entry getEntry() {
        return entry;
    }

    public String getName() {
        return entry.getName();
    }

    public File getFile() {
        return entry.getFile();
    }

    public boolean isZip() {
        return entry.isZip();
    }

    /** Cesta pro uživatele (tabulka, CSV, stavový řádek), viz {@link GroupPackages.Entry#getDisplayPath()}. */
    public String getDisplayPath() {
        return entry.getDisplayPath();
    }

    public PackageStatus getStatus() {
        return status;
    }

    void setStatus(PackageStatus status) {
        this.status = status;
    }

    /** Typ a verze, podle kterých se validovalo, null, dokud nejsou známé. */
    public Dmf getDmf() {
        return dmf;
    }

    void setDmf(Dmf dmf) {
        this.dmf = dmf;
    }

    /** „Monografie 2.2“, prázdný text, dokud typ a verze nejsou známé. */
    public String getTypeAndVersion() {
        return dmf == null ? "" : DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion();
    }

    /** Počty problémů (u dokončené validace z jádra, po chybě nebo zrušení z dokončených pravidel), null když nejsou. */
    public ProblemCounts getCounts() {
        return counts;
    }

    void setCounts(ProblemCounts counts) {
        this.counts = counts;
    }

    /** Doba validace balíku včetně rozbalení, null, dokud nedoběhla. */
    public Long getDurationMs() {
        return durationMs;
    }

    void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    /** Protokol XML, pokud se vyžádal a validace došla do zápisu, jinak null. */
    public File getXmlProtocol() {
        return xmlProtocol;
    }

    /** Textový log, pokud se vyžádal a validace došla do zápisu, jinak null. */
    public File getTxtLog() {
        return txtLog;
    }

    void setOutputs(File xmlProtocol, File txtLog) {
        this.xmlProtocol = xmlProtocol;
        this.txtLog = txtLog;
    }

    /** Chybová hláška (stav chyba), jinak null. */
    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return entry.getName() + " " + status + (dmf == null ? "" : " " + dmf) + (counts == null ? "" : " " + counts)
                + (message == null ? "" : " (" + message + ")");
    }
}
