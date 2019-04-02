package nkp.pspValidator.shared.externalUtils.validation.rules;

import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class MustExistDR extends AbstractDataRule {

    public MustExistDR(String validationName) {
        super(validationName);
    }

    @Override
    public List<String> validate(Object data) {
        if (data == null || data.toString().isEmpty() || isEmptyCollection(data)) {
            return singleError(error("kontrolovaná hodnota nenalezena"));
        } else {
            return noErrors();
        }
    }

    private boolean isEmptyCollection(Object data) {
        if (data instanceof List) {
            return ((List) data).isEmpty();
        }
        if (data instanceof Set) {
            return ((Set) data).isEmpty();
        }
        return false;
    }

}
