package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;

/**
 * Created by martin on 20.10.16.
 */
public class EfGetProvidedFile extends EvaluationFunction {

    private static final String PARAM_FILE_ID = "file_id";


    public EfGetProvidedFile(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.FILE)
                .withValueParam(PARAM_FILE_ID, ValueType.STRING, 1, 1));
    }

    @Override
    public String getName() {
        return "getProvidedFile";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        }

        ValueEvaluation paramFileId = valueParams.getParams(PARAM_FILE_ID).get(0).getEvaluation();
        String fileId = (String) paramFileId.getData();
        if (fileId == null) {
            return errorResultParamNull(PARAM_FILE_ID, paramFileId);
        } else if (fileId.isEmpty()) {
            return errorResult(String.format("hodnota parametru %s je prázdná", PARAM_FILE_ID));
        }

        File file = engine.getProvidedVarsManager().getProvidedFile(fileId);
        if (file == null) {
            return errorResult(String.format("soubor s id %s není poskytován", fileId));
        } else {
            return okResult(file);
        }
    }


}
