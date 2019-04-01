package nkp.pspValidator.shared.externalUtils.validation.rules;

import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class MustExistDR extends AbstractDataRule {

    public MustExistDR(String validationName) {
        super(validationName);
    }

    @Override
    public List<String> validate(Object data) {
        if (data == null || data.toString().isEmpty()) {
            return singleError(error("kontrolovaná hodnota nenalezena"));
        } else {
            return noErrors();
        }
    }

}