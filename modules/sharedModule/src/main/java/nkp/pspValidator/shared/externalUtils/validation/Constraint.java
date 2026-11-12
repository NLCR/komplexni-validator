package nkp.pspValidator.shared.externalUtils.validation;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public interface Constraint {
    boolean matches(Object data);

    /**
     * true, pokud omezení posuzuje seznam hodnot jako celek (např. nTimesXRemainingY). Ostatní omezení se u seznamu
     * (extrakce allNonempty, resultType STRING_LIST) vyhodnocují pro každou položku zvlášť, viz MustMatchAnyDR.
     */
    default boolean appliesToWholeList() {
        return false;
    }
}
