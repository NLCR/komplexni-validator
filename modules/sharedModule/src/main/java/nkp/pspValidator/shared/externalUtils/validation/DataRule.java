package nkp.pspValidator.shared.externalUtils.validation;

import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public interface DataRule {

    List<String> validate(Object data);
}
