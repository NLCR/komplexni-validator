package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import nkp.pspValidator.shared.externalUtils.validation.Constraint;

public class EndsWithConstraint implements Constraint {
    private final String suffix;

    public EndsWithConstraint(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean matches(Object data) {
        //null = hodnota nenalezena, neodpovida
        return data != null && data.toString().endsWith(suffix);
    }

    public String toString() {
        return String.format("musí končit na '%s'", suffix);
    }
}
