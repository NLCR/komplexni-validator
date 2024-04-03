package nkp.pspValidator.shared.externalUtils.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
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

    public List<String> validate(Object processedOutput) throws DataExtraction.ExtractionException {
        List<String> validationErrors = new ArrayList<>();
        Object data = dataExtraction.extract(processedOutput);
        for (DataRule rule : dataRules) {
            List<String> ruleErros = rule.validate(data);
            //first rule with some errors, other rules will be ignored
            if (ruleErros != null && !ruleErros.isEmpty()) {
                return ruleErros;
            }
        }
        return validationErrors;
    }

    public String getName() {
        return name;
    }
}
