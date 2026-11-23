package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import nkp.pspValidator.shared.StringUtils;
import nkp.pspValidator.shared.externalUtils.validation.Constraint;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class IsInFloatRangeConstraint implements Constraint {
    private final Float min;
    private final Float max;


    public IsInFloatRangeConstraint(Float min, Float max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean matches(Object data) {
        if (data == null) {
            return false;
        } else {
            Float value = (Float) data;
            if (min != null && value < min) {
                return false;
            } else if (max != null && value > max) {
                return false;
            } else {
                return true;
            }
        }
    }

    public String toString() {
        if (min != null && max != null) {
            return "musí být v intervalu <" + StringUtils.formatDecimal(min) + ", " + StringUtils.formatDecimal(max) + ">";
        } else if (min != null) {
            return "musí být alespoň " + StringUtils.formatDecimal(min);
        } else if (max != null) {
            return "musí být nejvýše " + StringUtils.formatDecimal(max);
        } else {
            return "musí být číslo (float)";
        }
    }

}
