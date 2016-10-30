package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfReturnFirstFileFromFileList extends EvaluationFunction {

    private static final String PARAM_FILE_LIST = "files";

    public EfReturnFirstFileFromFileList(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.FILE)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public Object evaluate() {
        checkContractCompliance();

        List<File> files = (List<File>) valueParams.getParams(PARAM_FILE_LIST).get(0).getValue();
        if (files.isEmpty()) {
            throw new IllegalStateException("Seznam souborů je prázdný");
        } else {
            return files.get(0);
        }
    }

    @Override
    public String getName() {
        return "RETURN_FIRST_FILE_FROM_LIST";
    }

}
