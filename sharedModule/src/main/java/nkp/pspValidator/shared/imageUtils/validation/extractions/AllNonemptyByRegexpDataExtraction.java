package nkp.pspValidator.shared.imageUtils.validation.extractions;

import nkp.pspValidator.shared.imageUtils.ExtractionResultType;
import nkp.pspValidator.shared.imageUtils.validation.DataExtraction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by martin on 18.11.16.
 */
public class AllNonemptyByRegexpDataExtraction implements DataExtraction {

    final ExtractionResultType extractionResultType;
    private final List<String> regexps;


    public AllNonemptyByRegexpDataExtraction(ExtractionResultType extractionResultType, List<String> regexps) {
        this.extractionResultType = extractionResultType;
        this.regexps = regexps;
    }

    @Override
    public List<String> extract(Object processedOutput) throws ExtractionException {
        String text = (String) processedOutput;
        List<String> result = new ArrayList<>();
        if (processedOutput != null && !((String) processedOutput).isEmpty()) {
            //System.err.println("TOTAL: \"" + processedOutput + "\"");
        }
        for (String regexp : regexps) {
            Pattern p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher m = p.matcher(text);
            while (m.find()) {
                String finding = m.group();
                String processed = reduceWhitespaces(finding);
                //System.err.println("ERROR: \"" + processed + "\"");
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
