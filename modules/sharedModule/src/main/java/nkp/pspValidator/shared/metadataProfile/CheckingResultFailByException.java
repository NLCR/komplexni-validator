package nkp.pspValidator.shared.metadataProfile;

public class CheckingResultFailByException implements CheckingResult {

    private final Exception e;

    public CheckingResultFailByException(Exception e) {
        this.e = e;
    }

    @Override
    public boolean matches() {
        return false;
    }

    public String getErrorMessage() {
        return e.getMessage();
    }

    @Override
    public String getSimpleErrorMessage() {
        return e.getMessage();
    }

    @Override
    public String getActualValue() {
        return null;
    }

    @Override
    public String getValueSpecification() {
        return null;
    }
}
