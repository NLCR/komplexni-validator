package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfReturnFirstFileFromList extends EvaluationFunction {

    private static final String PARAM_FILE_LIST = "file_list";

    public EfReturnFirstFileFromList(Engine engine) {
        super(engine, ValueType.FILE);
    }

    @Override
    public Object evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("Nebyly zadány parametry");
        }
        List<File> files = getFilesFromParams();
        if (files.isEmpty()) {
            throw new IllegalStateException("Seznam souborů je prázdný");
        } else {
            return files.get(0);
        }
    }


    public List<File> getFilesFromParams() {
        List<ValueParam> varNameValues = valueParams.getParams(PARAM_FILE_LIST);
        if (varNameValues == null || varNameValues.size() == 0) {
            throw new RuntimeException("chybí parametr " + PARAM_FILE_LIST);
        } else if (varNameValues.size() > 1) {
            throw new RuntimeException("parametr " + PARAM_FILE_LIST + " musí být jen jeden");
        }

        ValueParam param = varNameValues.get(0);
        //todo: kontrola typu
        return (List<File>) param.getValue();
    }

}
