package nkp.pspValidator.shared.externalUtils.validation.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class MustNotExistDR extends AbstractDataRule {
    public MustNotExistDR(String validationName) {
        super(validationName);
    }

    @Override
    public List<String> validate(Object data) {
        if (data != null && !data.toString().isEmpty()) {
            if (data instanceof List) {
                List dataLst = (List) data;
                List<String> errors = new ArrayList<>();
                for (Object item : dataLst) {
                    errors.add(error("zakázaná hodnota nalezena: " + item.toString()));
                }
                return errors;
            } else {
                return singleError(error("zakázaná hodnota nalezena: " + data.toString()));
            }
        } else {
            return noErrors();
        }
    }
}
