package nkp.pspValidator.shared.externalUtils.validation.rules;

import nkp.pspValidator.shared.externalUtils.validation.Constraint;

import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class MustMatchAnyDR extends AbstractDataRule {
    private final List<Constraint> constraints;

    public MustMatchAnyDR(String validationName, List<Constraint> constraints) {
        super(validationName);
        this.constraints = constraints;
    }

    @Override
    public List<String> validate(Object data) {
        for (Constraint constraint : constraints) {
            if (constraint.matches(data)) {
                return noErrors();
            }
        }
        return singleError(error(String.format("hodnota \"%s\" neodpovídá žádnému z omezení: %s", toString(data), toString(constraints))));
    }
}
