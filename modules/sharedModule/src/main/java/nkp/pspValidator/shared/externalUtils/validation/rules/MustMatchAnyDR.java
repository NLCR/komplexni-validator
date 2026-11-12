package nkp.pspValidator.shared.externalUtils.validation.rules;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.externalUtils.validation.Constraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 * <p>
 * Hodnota musí splnit aspoň jedno z omezení. Seznam hodnot (extrakce allNonempty) se posuzuje po položkách
 * a každá nevyhovující položka je samostatný problém (např. každý typ média EPUB musí být z povoleného výčtu).
 * Výjimkou jsou omezení, která posuzují seznam jako celek ({@link Constraint#appliesToWholeList()}, nTimesXRemainingY):
 * když je takové mezi omezeními, předá se jim seznam celý jako dřív.
 */
public class MustMatchAnyDR extends AbstractDataRule {
    private final List<Constraint> constraints;

    public MustMatchAnyDR(String validationName, List<Constraint> constraints) {
        this(validationName, null, constraints);
    }

    public MustMatchAnyDR(String validationName, Level level, List<Constraint> constraints) {
        super(validationName, level);
        this.constraints = constraints;
    }

    @Override
    public List<String> validate(Object data) {
        if (data instanceof List && !hasWholeListConstraint()) {
            List<String> errors = new ArrayList<>();
            for (Object item : (List) data) {
                String error = validateItem(item);
                if (error != null) {
                    errors.add(error);
                }
            }
            return errors;
        } else {
            String error = validateItem(data);
            return error == null ? noErrors() : singleError(error);
        }
    }

    private boolean hasWholeListConstraint() {
        for (Constraint constraint : constraints) {
            if (constraint.appliesToWholeList()) {
                return true;
            }
        }
        return false;
    }

    private String validateItem(Object item) {
        for (Constraint constraint : constraints) {
            if (constraint.matches(item)) {
                return null;
            }
        }
        return error(String.format("hodnota \"%s\" neodpovídá žádnému z omezení: %s", toString(item), toString(constraints)));
    }
}
