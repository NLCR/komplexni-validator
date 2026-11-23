package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ověřené řady verzí jednoho externího nástroje na jedné platformě (externalUtils.xml, element verifiedVersions).
 * Seznam může být převzatý z jiné platformy (Linux přebírá macOS, dokud ho nikdo neověří): pak je
 * {@link #getInheritedFrom()} platforma, na které se ověřovalo. Prázdný seznam znamená, že na platformě není ověřena
 * žádná verze (každá verze je neověřená). Hodnota, neměnná.
 */
public final class VerifiedVersions {

    /** Ověřená řada, např. „1.34“, a přesná verze, na které se ověřovalo (jen pro zobrazení), např. „1.34.0“. */
    public static final class Series {
        private final String series;
        private final String verifiedWith;

        public Series(String series, String verifiedWith) {
            this.series = series;
            this.verifiedWith = verifiedWith;
        }

        public String getSeries() {
            return series;
        }

        /** Přesná verze, na které se řada ověřovala, nebo null, když není uvedená. */
        public String getVerifiedWith() {
            return verifiedWith;
        }

        @Override
        public String toString() {
            return series + (verifiedWith == null ? "" : " (" + verifiedWith + ")");
        }
    }

    private final List<Series> series;
    private final OperatingSystem inheritedFrom;

    /**
     * @param inheritedFrom platforma, jejíž seznam se převzal, nebo null pro vlastní seznam platformy
     */
    public VerifiedVersions(List<Series> series, OperatingSystem inheritedFrom) {
        this.series = Collections.unmodifiableList(new ArrayList<>(series));
        this.inheritedFrom = inheritedFrom;
    }

    public List<Series> getSeries() {
        return series;
    }

    /** Platforma, z níž je seznam převzatý, nebo null. */
    public OperatingSystem getInheritedFrom() {
        return inheritedFrom;
    }

    public boolean isInherited() {
        return inheritedFrom != null;
    }

    /** Stejný seznam převzatý jinou platformou ze zdrojové platformy {@code from}. */
    public VerifiedVersions inheritedFrom(OperatingSystem from) {
        return new VerifiedVersions(series, from);
    }

    /** První řada, do které verze patří ({@link UtilVersion#isInSeries}), nebo null. */
    public Series findSeries(String version) {
        for (Series candidate : series) {
            if (UtilVersion.isInSeries(version, candidate.getSeries())) {
                return candidate;
            }
        }
        return null;
    }

    /** Řady oddělené čárkou, např. „1.29, 1.34“; prázdný řetězec pro prázdný seznam. */
    public String seriesList() {
        StringBuilder text = new StringBuilder();
        for (Series candidate : series) {
            text.append(text.length() == 0 ? "" : ", ").append(candidate.getSeries());
        }
        return text.toString();
    }

    @Override
    public String toString() {
        return series + (inheritedFrom == null ? "" : " z " + inheritedFrom);
    }
}
