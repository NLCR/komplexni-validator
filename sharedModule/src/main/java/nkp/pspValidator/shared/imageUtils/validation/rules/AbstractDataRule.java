package nkp.pspValidator.shared.imageUtils.validation.rules;

import nkp.pspValidator.shared.imageUtils.validation.DataRule;

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

    public String toString(Object data) {
        if (data instanceof List) {
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
