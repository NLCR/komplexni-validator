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
    public boolean appliesToWholeList() {
        return true;
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

    /**
     * Popis omezení do hlášky (MustMatchAnyDR): hodnota x smí být v seznamu nejvýše n-krát (n je horní mez, ne přesný
     * počet), všechny ostatní položky musí být y. Při n = 0 hodnota x není povolená vůbec.
     */
    public String toString() {
        if (n == 0) {
            return String.format("musí obsahovat jen hodnoty '%s'", y);
        }
        return String.format("musí obsahovat nejvýše %d× '%s', ostatní hodnoty '%s'", n, x, y);
    }
}
