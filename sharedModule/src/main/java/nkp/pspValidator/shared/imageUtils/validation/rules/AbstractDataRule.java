package nkp.pspValidator.shared.imageUtils.validation.rules;

import nkp.pspValidator.shared.imageUtils.validation.DataRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public abstract class AbstractDataRule implements DataRule {
    private final String validationName;

    public AbstractDataRule(String validationName) {
        this.validationName = validationName;
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
            List list = (List) data;
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(list.get(i));
            }
            builder.append(']');
            return builder.toString();
        } else {
            return data.toString();
        }
    }
}
