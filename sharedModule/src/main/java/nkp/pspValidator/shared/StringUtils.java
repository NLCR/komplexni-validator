package nkp.pspValidator.shared;

/**
 * Created by martin on 11.11.16.
 */
public class StringUtils {

    public static String declineErrorNumber(int errors) {
        switch (errors) {
            case 1:
                return errors + " chyba";
            case 2:
            case 3:
            case 4:
                return errors + " chyby";
            default:
                return errors + " chyb";
        }
    }
}
