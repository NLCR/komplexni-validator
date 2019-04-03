package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.externalUtils.validation.DataExtraction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TextDataExtraction implements DataExtraction {

    List<String> findAllMatches(String regexp, String text) {
        List<String> result = new ArrayList<>();
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String match = m.group();
            String processed = reduceWhitespaces(match);
            if (!processed.isEmpty()) {
                result.add(processed);
            }
        }
        return result;
    }

    private String reduceWhitespaces(String string) {
        return string.trim();
/*        if (string == null) {
            return string;
        } else {
            return string.replace("\\s", " ").trim();
        }*/
    }

}
