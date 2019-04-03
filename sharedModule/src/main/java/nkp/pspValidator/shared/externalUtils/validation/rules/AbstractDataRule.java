package nkp.pspValidator.shared.externalUtils.validation.rules;

import nkp.pspValidator.shared.engine.Utils;
import nkp.pspValidator.shared.externalUtils.validation.DataRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public abstract class AbstractDataRule implements DataRule {
    private final String validationName;

    public AbstractDataRule(String validationName) {
        this.validationName = validationName;
    }

    public String getValidationName() {
        return validationName;
    }

    String error(String message) {
        return String.format("%s: %s", validationName, message);
    }

    List<String> singleError(String error) {
        List<String> list = new ArrayList<>();
        list.add(error);
        return list;
    }

    List<String> noErrors() {
        return Collections.emptyList();
    }

    public String toString(Object data) {
        if (data == null) {
            return null;
        } else if (data instanceof List) {
            return Utils.listToString((List) data);
        } else {
            return data.toString();
        }
    }
}
