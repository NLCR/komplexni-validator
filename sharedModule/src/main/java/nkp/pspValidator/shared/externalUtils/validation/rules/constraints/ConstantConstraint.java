package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import nkp.pspValidator.shared.externalUtils.validation.Constraint;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class ConstantConstraint implements Constraint {
    private final String constant;

    public ConstantConstraint(String constant) {
        this.constant = constant;
    }

    @Override
    public boolean matches(Object data) {
        String dataStr = data.toString();
        return constant.equals(dataStr);
    }

    public String toString() {
        return String.format("musí být přesně '%s'", constant);
    }
}
