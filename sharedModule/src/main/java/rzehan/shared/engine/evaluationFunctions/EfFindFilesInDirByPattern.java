package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.PatternEvaluation;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfFindFilesInDirByPattern extends EvaluationFunction {

    private static final String PARAM_DIR = "dir";
    private static final String PARAM_PATTERN = "pattern";

    public EfFindFilesInDirByPattern(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.FILE_LIST)
                .withPatternParam(PARAM_PATTERN)
                .withValueParam(PARAM_DIR, ValueType.FILE, 1, 1));
    }

    @Override
    public String getName() {
        return "findFilesInDirByPattern";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramDir = valueParams.getParams(PARAM_DIR).get(0).getEvaluation();
            File dir = (File) paramDir.getData();
            if (dir == null) {
                return errorResultParamNull(PARAM_DIR, paramDir);
            } else if (!dir.exists()) {
                return errorResultFileDoesNotExist(dir);
            } else if (!dir.isDirectory()) {
                return errorResultFileIsNotDir(dir);
            } else if (!dir.canRead()) {
                return errorResultCannotReadDir(dir);
            }

            PatternEvaluation patternParam = patternParams.getParam(PARAM_PATTERN).getEvaluation();
            if (!patternParam.isOk()) {
                return errorResultPatternParamNull(PARAM_PATTERN, patternParam);
            }

            File[] files = dir.listFiles();
            List<File> filesMatching = new ArrayList<>(files.length);
            for (File file : files) {
                if (patternParam.matches(file.getName())) {
                    filesMatching.add(file);
                }
            }
            return okResult(filesMatching);

        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }


}
