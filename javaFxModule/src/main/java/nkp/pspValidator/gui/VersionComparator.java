package nkp.pspValidator.gui;

import java.util.Comparator;

public class VersionComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null && o2.trim().isEmpty()) {
            return 0;
        } else if (o2 == null && o1.trim().isEmpty()) {
            return 0;
        } else {
            String[] segments1 = o1.split("\\.");
            String[] segments2 = o2.split("\\.");
            for (int i = 0; ; i++) {
                if (segments1.length == i && segments2.length == i) { //no more segments for both
                    return 0;
                } else if (segments1.length > i && segments2.length > i) { //more segments for both
                    int currentSegmentCompared = compareSegments(segments1[i], segments2[i]);
                    if (currentSegmentCompared != 0) {
                        return currentSegmentCompared;
                    }
                } else if (segments1.length == i) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }

    private int compareSegments(String segment1, String segment2) {
        try {
            int first = Integer.valueOf(segment1);
            int second = Integer.valueOf(segment2);
            return first - second;
        } catch (NumberFormatException e) {
            return segment1.compareTo(segment2);
        }
    }

}
