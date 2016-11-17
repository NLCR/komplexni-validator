package nkp.pspValidator.shared.imageUtils.validation.rules;

/**
 * Created by martin on 17.11.16.
 */
public class MustExistDR extends AbstractDataRule {

    public MustExistDR(String validationName) {
        super(validationName);
    }

    @Override
    public String validate(Object data) {
        if (data == null || data.toString().isEmpty()) {
            return error("požadovaná hodnota nenalezena");
        } else {
            return null;
        }
    }

}
