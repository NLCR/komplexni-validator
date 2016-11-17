package nkp.pspValidator.shared.imageUtils.validation.rules;

import nkp.pspValidator.shared.imageUtils.validation.Constraint;

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
    public String validate(Object data) {
        if (!constraint.matches(data)) {
            return error(String.format("hodnota \"%s\" neodpovídá omezení: \"%s\"", toString(data), constraint.toString()));
        } else {
            return null;
        }
    }
}
