package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;

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
        checkContractCompliance();

        String fileId = (String) valueParams.getParams(PARAM_FILE_ID).get(0).getValue();
        File file = engine.getProvidedVarsManager().getProvidedFile(fileId);
        if (file == null) {
            throw new RuntimeException("soubor s id " + fileId + " není poskytován");
        } else {
            return file;
        }
    }

    @Override
    public String getName() {
        return "PROVIDED_FILE";
    }

}
