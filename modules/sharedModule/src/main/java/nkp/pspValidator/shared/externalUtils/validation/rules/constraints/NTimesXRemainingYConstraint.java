package nkp.pspValidator.shared.externalUtils.validation.rules.constraints;

import nkp.pspValidator.shared.externalUtils.validation.Constraint;

import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class NTimesXRemainingYConstraint implements Constraint {
    private final Integer n;
    private final String x;
    private final String y;

    public NTimesXRemainingYConstraint(Integer n, String x, String y) {
        this.n = n;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean matches(Object data) {
        if (data == null) {
            return false;
        } else {
            List<String> list = (List<String>) data;
            int xCounter = 0;
            for (String string : list) {
                if (string.equals(x)) {
                    if (xCounter == n) {
                        return false;
                    } else {
                        xCounter++;
                    }
                } else if (string.equals(y)) {
                    //ok
                } else {
                    //unknown value
                    return false;
                }
            }
            return true;
        }
    }
}
