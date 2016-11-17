package nkp.pspValidator.shared.imageUtils.validation.rules;

/**
 * Created by martin on 17.11.16.
 */
public class MustNotExistDR extends AbstractDataRule {
    public MustNotExistDR(String validationName) {
        super(validationName);
    }

    @Override
    public String validate(Object data) {
        if (data != null && !data.toString().isEmpty()) {
            return error("zakázaná hodnota nalezena");
        } else {
            return null;
        }
    }
}
