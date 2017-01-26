package nkp.pspValidator.shared;

/**
 * Created by Martin Řehánek on 11.11.16.
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

    public static String formatMilliseconds(long ms) {
        long hours = 0;
        long remains = ms;
        if (remains >= 3600000) { //hours
            hours = remains / 3600000;
            remains = remains % 3600000;
        }
        long minutes = 0;
        if (remains >= 60000) { //minutes
            minutes = remains / 60000;
            remains = remains % 60000;
        }
        long seconds = 0;
        if (remains >= 1000) {//seconds
            seconds = remains / 1000;
            remains = remains % 1000;
        }

        StringBuilder builder = new StringBuilder();
        if (hours != 0) {
            builder.append(hours).append(" h, ");
        }
        if (minutes != 0 || hours != 0) {
            builder.append(minutes).append(" m, ");
        }
        if (seconds != 0 || minutes != 0 || hours != 0) {
            builder.append(seconds).append(" s, ");
        }
        builder.append(remains).append(" ms");
        return builder.toString();
    }

}
