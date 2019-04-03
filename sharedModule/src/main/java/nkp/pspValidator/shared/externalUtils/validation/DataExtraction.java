package nkp.pspValidator.shared.externalUtils.validation;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public interface DataExtraction {
    Object extract(Object processedOutput) throws ExtractionException;

    class ExtractionException extends Exception {
        public ExtractionException(String message) {
            super(message);
        }

        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ExtractionException(Throwable cause) {
            super(cause);
        }
    }
}
