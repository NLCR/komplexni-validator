package nkp.pspValidator.shared.imageUtils.validation.rules;

import nkp.pspValidator.shared.imageUtils.validation.Constraint;

import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public class MustMatchDR extends AbstractDataRule {
    private final Constraint constraint;

    public MustMatchDR(String validationName, Constraint constraint) {
        super(validationName);
        this.constraint = constraint;
    }

    @Override
    public List<String> validate(Object data) {
        if (!constraint.matches(data)) {
            return singleError(error(String.format("hodnota \"%s\" neodpovídá omezení: \"%s\"", toString(data), constraint.toString())));
        } else {
            return noErrors();
        }
    }
}
