package nkp.pspValidator.shared.imageUtils.validation.rules;

import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public class MustNotExistDR extends AbstractDataRule {
    public MustNotExistDR(String validationName) {
        super(validationName);
    }

    @Override
    public List<String> validate(Object data) {
        if (data != null && !data.toString().isEmpty()) {
            return singleError(error("zakázaná hodnota nalezena"));
        } else {
            return noErrors();
        }
    }
}
