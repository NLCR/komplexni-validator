package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import nkp.pspValidator.shared.externalUtils.validation.Constraint;

public class StartsWithConstraint implements Constraint {
    private final String prefix;

    public StartsWithConstraint(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean matches(Object data) {
        //null = hodnota nenalezena, neodpovida
        return data != null && data.toString().startsWith(prefix);
    }

    public String toString() {
        return String.format("musí začínat na '%s'", prefix);
    }
}
