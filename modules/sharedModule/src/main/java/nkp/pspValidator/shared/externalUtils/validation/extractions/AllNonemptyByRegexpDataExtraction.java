package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.externalUtils.ExtractionResultType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 18.11.16.
 */
public class AllNonemptyByRegexpDataExtraction extends TextDataExtraction {

    final ExtractionResultType extractionResultType;
    private final List<String> regexps;


    public AllNonemptyByRegexpDataExtraction(ExtractionResultType extractionResultType, List<String> regexps) {
        this.extractionResultType = extractionResultType;
        this.regexps = regexps;
    }

    @Override
    public List<String> extract(Object processedOutput) throws ExtractionException {
        try {
            List<String> allMatches = new ArrayList<>();
            String text = processedOutput.toString();
            if (text != null) {
                text = text.trim();
                if (!text.isEmpty()) {
                    for (String regexp : regexps) {
                        List<String> matchesForRegexp = findAllMatches(regexp, text);
                        allMatches.addAll(matchesForRegexp);
                    }
                }
            }
            /*for (String match : allMatches) {
                System.out.println("match: " + match);
            }*/
            return allMatches;
        } catch (Throwable e) {
            throw new ExtractionException(e);
        }
    }

}
