package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.Dmf;

/**
 * Výsledek rozpoznání typu a verze balíku (hodnota, bez vazby na JavaFX).
 */
public class Recognition {

    public enum Status {
        /** cesta není zadaná nebo neexistuje, není to adresář nebo soubor podle zdroje */
        INVALID_PATH,
        /** cesta je v pořádku, ale typ dokumentu nelze zjistit (není balík, víc nebo žádný METS, neznámý TYPE, poškozený ZIP) */
        NOT_RECOGNIZED,
        /** typ rozpoznán; verze z info.xml může chybět ({@link #getVersionInPackage()} je null) */
        RECOGNIZED
    }

    private final Status status;
    private final Dmf.Type type;
    private final String versionInPackage;
    private final String versionProblem;
    private final String message;

    private Recognition(Status status, Dmf.Type type, String versionInPackage, String versionProblem, String message) {
        this.status = status;
        this.type = type;
        this.versionInPackage = versionInPackage;
        this.versionProblem = versionProblem;
        this.message = message;
    }

    public static Recognition invalidPath(String message) {
        return new Recognition(Status.INVALID_PATH, null, null, null, message);
    }

    public static Recognition notRecognized(String message) {
        return new Recognition(Status.NOT_RECOGNIZED, null, null, null, message);
    }

    /**
     * @param versionInPackage verze z info.xml nebo null
     * @param versionProblem   proč verzi nešlo přečíst (chybí soubor info, víc souborů info, chyba XML), jinak null
     */
    public static Recognition recognized(Dmf.Type type, String versionInPackage, String versionProblem) {
        return new Recognition(Status.RECOGNIZED, type, versionInPackage, versionProblem, null);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRecognized() {
        return status == Status.RECOGNIZED;
    }

    /** Typ dokumentu podle METS, null když není rozpoznán. */
    public Dmf.Type getType() {
        return type;
    }

    /** Verze z info.xml (metadataversion), null když ji balík neuvádí nebo ji nešlo přečíst. */
    public String getVersionInPackage() {
        return versionInPackage;
    }

    /** Proč nešlo verzi přečíst (validace pak bez vynucené verze skončí chybou), jinak null. */
    public String getVersionProblem() {
        return versionProblem;
    }

    /** Chyba pro stavy INVALID_PATH a NOT_RECOGNIZED. */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return status + (type == null ? "" : " " + type + " " + versionInPackage) + (message == null ? "" : ": " + message);
    }
}
