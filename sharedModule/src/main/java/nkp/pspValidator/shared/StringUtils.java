package nkp.pspValidator.shared;

/**
 * Created by martin on 11.11.16.
 */
public class StringUtils {

    public static String declineProblemNumber(int errors) {
        switch (errors) {
            case 1:
                return errors + " problém";
            case 2:
            case 3:
            case 4:
                return errors + " problémy";
            default:
                return errors + " problémů";
        }
    }
}
