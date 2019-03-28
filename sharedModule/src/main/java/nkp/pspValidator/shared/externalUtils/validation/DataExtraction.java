package nkp.pspValidator.shared.externalUtils.validation;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public interface DataExtraction {
    Object extract(Object processedOutput) throws ExtractionException;

    public static class ExtractionException extends Exception {
        public ExtractionException(String message) {
            super(message);
        }
    }
}
