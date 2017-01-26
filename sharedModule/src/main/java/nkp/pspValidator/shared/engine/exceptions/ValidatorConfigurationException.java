package nkp.pspValidator.shared.engine.exceptions;

/**
 * Created by Martin Řehánek on 1.11.16.
 */
public class ValidatorConfigurationException extends Exception {


    //TODO: asi prejmenovat na FdmfConfigurationException
    public ValidatorConfigurationException(String message) {
        super(message);
    }

    public ValidatorConfigurationException(String format, Object... args) {
        this(String.format(format, args));
    }
}
