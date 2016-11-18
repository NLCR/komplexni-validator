package nkp.pspValidator.shared.imageUtils.validation.rules;

import nkp.pspValidator.shared.imageUtils.validation.Constraint;

import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public class MustMatchOneDR extends AbstractDataRule {
    private final List<Constraint> constraints;

    public MustMatchOneDR(String validationName, List<Constraint> constraints) {
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
