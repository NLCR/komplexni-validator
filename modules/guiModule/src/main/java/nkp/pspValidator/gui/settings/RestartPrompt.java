package nkp.pspValidator.gui.settings;

/**
 * Dotaz po uložení nového adresáře konfigurace validátoru v Nastavení: konfigurace se použije až po restartu aplikace.
 * Bez JavaFX, jen texty a dostupnost tlačítek podle situace:
 * <ul>
 * <li>restart jde ({@link Kind#RESTART}): „Později“ a „Restartovat teď“,</li>
 * <li>příkaz pro nové spuštění aplikace nejde sestavit ({@link Kind#MANUAL}): „Později“ a „Ukončit aplikaci“,
 * uživatel aplikaci spustí znovu sám,</li>
 * <li>běží validace: hlavní tlačítko je zakázané s vysvětlením, změna se projeví po příštím spuštění.</li>
 * </ul>
 */
public final class RestartPrompt {

    public enum Kind {
        RESTART, MANUAL
    }

    public static final String MESSAGE = "Konfigurace validátoru se použije po restartu aplikace.";
    public static final String LATER = "Později";

    private final Kind kind;
    private final boolean validationRunning;

    private RestartPrompt(Kind kind, boolean validationRunning) {
        this.kind = kind;
        this.validationRunning = validationRunning;
    }

    /**
     * @param commandAvailable  příkaz pro nové spuštění aplikace jde sestavit
     * @param validationRunning běží validace (restart ani ukončení teď nenabízet)
     */
    public static RestartPrompt of(boolean commandAvailable, boolean validationRunning) {
        return new RestartPrompt(commandAvailable ? Kind.RESTART : Kind.MANUAL, validationRunning);
    }

    public Kind getKind() {
        return kind;
    }

    public String getTitle() {
        return kind == Kind.RESTART ? "Restartovat aplikaci?" : "Ukončit aplikaci?";
    }

    public String getMessage() {
        return MESSAGE;
    }

    /** Vysvětlení pod hlavní zprávou. */
    public String getNote() {
        if (validationRunning) {
            return (kind == Kind.RESTART ? "Restartovat" : "Ukončit") + " aplikaci teď nejde, běží validace. "
                    + "Změna se projeví po příštím spuštění aplikace.";
        }
        if (kind == Kind.MANUAL) {
            return "Aplikaci nejde restartovat automaticky. Ukončete aplikaci a spusťte ji znovu. "
                    + "Později: aplikace poběží dál s dosavadní konfigurací.";
        }
        return "Později: aplikace poběží dál s dosavadní konfigurací, nová se použije po příštím spuštění.";
    }

    /** Hlavní tlačítko. */
    public String getPrimaryLabel() {
        return kind == Kind.RESTART ? "Restartovat teď" : "Ukončit aplikaci";
    }

    public boolean isPrimaryEnabled() {
        return !validationRunning;
    }

    public String getLaterLabel() {
        return LATER;
    }
}
