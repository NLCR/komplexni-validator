package nkp.pspValidator.shared.metadataProfile;

/**
 * Created by Martin Řehánek on 27.1.17.
 */
public abstract class CheckingResultFail implements CheckingResult {
    @Override
    public boolean matches() {
        return false;
    }

    public abstract String getErrorMessage();
}
