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
        super(engine, ValueType.FILE);
    }

    @Override
    public File evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("nebyly zadány parametry");
        }
        String fileId = getFileIdFromParams();
        File file = engine.getProvidedVarsManager().getProvidedFile(fileId);
        if (file == null) {
            throw new RuntimeException("soubor s id " + fileId + " není poskytován");
        } else {
            return file;
        }
    }


    public String getFileIdFromParams() {
        List<ValueParam> varNameValues = valueParams.getParams(PARAM_FILE_ID);
        if (varNameValues == null || varNameValues.size() == 0) {
            throw new RuntimeException("chybí parametr " + PARAM_FILE_ID);
        } else if (varNameValues.size() > 1) {
            throw new RuntimeException("parametr " + PARAM_FILE_ID + " musí být jen jeden");
        }
        ValueParam param = varNameValues.get(0);
        //kontrola typu
        if (param.getType() != ValueType.STRING) {
            throw new RuntimeException(String.format("parametr %s není očekávaného typu %s", PARAM_FILE_ID, ValueType.STRING.toString()));
        }
        return (String) param.getValue();
    }
}
