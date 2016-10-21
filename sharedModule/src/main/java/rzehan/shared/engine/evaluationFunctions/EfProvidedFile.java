package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

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
        if (fileId == "PSP_DIR") {
            return new File("/home/martin/zakazky/NKP-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        } else {
            throw new RuntimeException("soubor s id " + fileId + " není poskytován");
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
