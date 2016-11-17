package nkp.pspValidator.shared.imageUtils.validation;

import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
class Validation {
    private final String name;
    private final DataExtraction dataExtraction;
    private final List<DataRule> dataRules;

    public Validation(String name, DataExtraction dataExtraction, List<DataRule> dataRules) {
        this.name = name;
        this.dataExtraction = dataExtraction;
        this.dataRules = dataRules;
    }

    public String validate(Object processedOutput) throws DataExtraction.ExtractionException {
        //System.out.println("validating " + name);
        Object data = dataExtraction.extract(processedOutput);
        for (DataRule rule : dataRules) {
            String result = rule.validate(data);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
