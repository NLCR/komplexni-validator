package nkp.pspValidator.shared;

import java.util.Locale;

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

    /**
     * Desetinné číslo pro text hlášek validace a protokolu: šest desetinných míst (jako {@code %f}), desetinná čárka,
     * bez oddělovače tisíců. Výsledek nezávisí na jazyce systému ani na tom, zda má Java lokalizační data
     * (dřív {@code String.format("%f")} s výchozím locale: čárka v českém prostředí, tečka v anglickém nebo {@code C}).
     * Čárka odpovídá dosavadnímu tvaru v českém prostředí (macOS, Windows), např. „11732,000000“.
     */
    public static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%f", value).replace('.', ',');
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
