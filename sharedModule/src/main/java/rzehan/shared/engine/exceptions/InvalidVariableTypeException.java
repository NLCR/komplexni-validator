package rzehan.shared.engine.exceptions;

/**
 * Created by martin on 20.10.16.
 */
public class InvalidVariableTypeException extends RuntimeException {

    public InvalidVariableTypeException(String varName, String expectedType) {
        super(String.format("Proměnná %s není očekávaného typu %s", varName, expectedType));
    }
}
