package nkp.pspValidator.gui.results;

import nkp.pspValidator.gui.newvalidation.ValidationRequest;

/**
 * „Validovat znovu“ (issue #87): pamatuje si požadavek naposledy spuštěné validace a říká, kdy ho lze spustit znovu.
 * Požadavek je neměnná hodnota (zdroj a cesta, parametry {@code DmfDetector}, výstupy, podrobnost logu, vypnuté
 * nástroje), takže opakovaná validace dostane stejné nastavení bez znovuotevření dialogu; ZIP se při ní znovu rozbalí,
 * protože se spouští stejnou cestou jako poprvé. Bez JavaFX.
 */
public class RerunState {

    private ValidationRequest lastRequest;
    private boolean running;

    /**
     * Validace balíku začala (u ZIPu až po rozbalení).
     *
     * @param request požadavek, ze kterého validace vznikla; null, když nevznikla z požadavku (pak ji nejde opakovat)
     */
    public void validationStarted(ValidationRequest request) {
        this.lastRequest = request;
        this.running = true;
    }

    /** Validace skončila (dokončena, zrušena nebo s chybou). */
    public void validationEnded() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    /** Akce je dostupná jen po skončené validaci, která vznikla z požadavku, a ne během běhu. */
    public boolean canRerun() {
        return lastRequest != null && !running;
    }

    /**
     * @throws IllegalStateException když akce není dostupná
     */
    public ValidationRequest requestForRerun() {
        if (!canRerun()) {
            throw new IllegalStateException(running ? "validace běží" : "žádná validace neproběhla");
        }
        return lastRequest;
    }
}
