package rzehan.shared.engine.evaluationFunctions.impl;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.evaluationFunctions.ValueParam;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfReturnFirstFileFromFileList extends EvaluationFunction {

    private static final String PARAM_FILE_LIST = "file_list";

    public EfReturnFirstFileFromFileList(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.FILE)
                .withValueParam(PARAM_FILE_LIST, ValueType.LIST_OF_FILES, 1, 1)
        );
    }

    @Override
    public Object evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("Nebyly zadány parametry");
        }
        contract.checkComplience(valueParams, null);

        List<File> files = (List<File>) valueParams.getParams(PARAM_FILE_LIST).get(0).getValue();
        if (files.isEmpty()) {
            throw new IllegalStateException("Seznam souborů je prázdný");
        } else {
            return files.get(0);
        }
    }

}
