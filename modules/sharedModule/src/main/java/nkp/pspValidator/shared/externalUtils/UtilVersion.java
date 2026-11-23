package nkp.pspValidator.shared.externalUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Číslo verze externího nástroje z textu, který vrátí detekce verze ({@link ExternalUtilManager#runUtilVersionDetection}),
 * a porovnání s ověřenou řadou verzí (externalUtils.xml, element verifiedVersions).
 * <p>
 * Číslo verze je první výskyt aspoň dvou čísel oddělených tečkou, volitelně s „v“ na začátku; nesmí navazovat
 * na písmeno, číslici ani tečku. Z „Jhove (Rel. 1.34.0, 2025-07-02)“ je tak „1.34.0“ (datum tečky nemá),
 * z „ImageMagick 7.1.2-35 Q16-HDRI“ „7.1.2“, z „Current core system version is v8.5“ „8.5“, z „EPUBCheck v5.4.0“
 * „5.4.0“. Přípony za číslem („-35“, „-SNAPSHOT“, „rc2“) se do porovnání nepočítají. Samotné číslo bez tečky
 * („2024“, „v8“) se za verzi nebere, verze se pak nerozpozná.
 */
public final class UtilVersion {

    private static final Pattern VERSION_NUMBER = Pattern.compile("(?<![\\p{L}\\d.])v?(\\d+(?:\\.\\d+)+)");
    private static final Pattern SERIES = Pattern.compile("\\d+(?:\\.\\d+)*");

    private UtilVersion() {
    }

    /**
     * Číslo verze z textu detekce verze, nebo null, když ho text neobsahuje (i pro null a prázdný text).
     */
    public static String parse(String detectionText) {
        if (detectionText == null) {
            return null;
        }
        Matcher matcher = VERSION_NUMBER.matcher(detectionText);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Je text platný zápis řady (čísla oddělená tečkou, např. „7.1“, „0.22“, „8.5“)? */
    public static boolean isValidSeries(String series) {
        return series != null && SERIES.matcher(series).matches();
    }

    /**
     * Patří verze do řady? Porovnává se po složkách jako čísla: řada „7.1“ platí pro „7.1“, „7.1.2“ i „7.01.3“,
     * ale ne pro „7.10“ ani „7.2“; řada „1.3“ neplatí pro „1.34“. Verze s méně složkami než řada do ní nepatří
     * (o „1.34“ nelze říct, že je z řady „1.34.1“).
     *
     * @param version číslo verze ({@link #parse}), null do žádné řady nepatří
     * @param series  řada ({@link #isValidSeries})
     */
    public static boolean isInSeries(String version, String series) {
        if (version == null || !isValidSeries(series) || !SERIES.matcher(version).matches()) {
            return false;
        }
        String[] versionParts = version.split("\\.");
        String[] seriesParts = series.split("\\.");
        if (versionParts.length < seriesParts.length) {
            return false;
        }
        for (int i = 0; i < seriesParts.length; i++) {
            if (!stripLeadingZeros(versionParts[i]).equals(stripLeadingZeros(seriesParts[i]))) {
                return false;
            }
        }
        return true;
    }

    /** Číslo bez úvodních nul („007“ → „7“, „0“ zůstane), aby šlo porovnat bez převodu na číslo (bez přetečení). */
    private static String stripLeadingZeros(String number) {
        int i = 0;
        while (i < number.length() - 1 && number.charAt(i) == '0') {
            i++;
        }
        return number.substring(i);
    }
}
