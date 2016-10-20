package rzehan.shared.engine.exceptions;

/**
 * Created by martin on 20.10.16.
 */
public class VariableNotDefinedException extends RuntimeException {

    public VariableNotDefinedException(String name) {
        super(String.format("Proměnná %s není definována", name));
    }
}
