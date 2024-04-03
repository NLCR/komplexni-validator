package nkp.pspValidator.shared.engine.exceptions;

/**
 * Created by Martin Řehánek on 20.10.16.
 */
public class VariableNotDefinedException extends Exception {

    private final String varName;

    public VariableNotDefinedException(String varName, String message) {
        super(message);
        this.varName = varName;
    }

    public String getVarName() {
        return varName;
    }
}
