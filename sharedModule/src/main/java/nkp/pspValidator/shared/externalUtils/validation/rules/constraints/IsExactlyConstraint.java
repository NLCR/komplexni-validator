package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import nkp.pspValidator.shared.externalUtils.validation.Constraint;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class IsExactlyConstraint implements Constraint {
    private final String value;

    public IsExactlyConstraint(String value) {
        this.value = value;
    }

    @Override
    public boolean matches(Object data) {
        String dataStr = data.toString();
        return value.equals(dataStr);
    }

    public String toString() {
        return String.format("musí být přesně '%s'", value);
    }
}
