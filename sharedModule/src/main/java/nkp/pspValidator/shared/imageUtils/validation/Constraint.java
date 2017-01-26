package nkp.pspValidator.shared.imageUtils.validation;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public interface Constraint {
    boolean matches(Object data);
}
