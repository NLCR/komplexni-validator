package nkp.pspValidator.shared.imageUtils.validation;

import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public interface DataRule {

    List<String> validate(Object data);
}
