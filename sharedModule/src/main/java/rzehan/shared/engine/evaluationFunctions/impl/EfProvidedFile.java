package rzehan.shared.engine.evaluationFunctions.impl;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.evaluationFunctions.ValueParam;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 20.10.16.
 */
public class EfProvidedFile extends EvaluationFunction {

    private static final String PARAM_FILE_ID = "file_id";


    public EfProvidedFile(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.FILE)
                .withValueParam(PARAM_FILE_ID, ValueType.STRING, 1, 1));
    }

    @Override
    public File evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("nebyly zadány parametry");
        }
        contract.checkComplience(valueParams, null);

        String fileId = (String) valueParams.getParams(PARAM_FILE_ID).get(0).getValue();
        File file = engine.getProvidedVarsManager().getProvidedFile(fileId);
        if (file == null) {
            throw new RuntimeException("soubor s id " + fileId + " není poskytován");
        } else {
            return file;
        }
    }

}
