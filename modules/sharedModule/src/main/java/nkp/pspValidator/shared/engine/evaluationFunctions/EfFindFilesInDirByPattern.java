package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.PatternEvaluation;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfFindFilesInDirByPattern extends EvaluationFunction {

    private static final String PARAM_DIR = "dir";
    private static final String PARAM_PATTERN = "pattern";
    private static final String PARAM_ERROR_IF_DIR_NULL = "errorIfDirNull";

    public EfFindFilesInDirByPattern(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.FILE_LIST)
                .withPatternParam(PARAM_PATTERN)
                .withValueParam(PARAM_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_ERROR_IF_DIR_NULL, ValueType.BOOLEAN, 0, 1));
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            boolean errorIfDirNull = true;
            List<ValueParam> paramErrorIfDirNull = valueParams.getParams(PARAM_ERROR_IF_DIR_NULL);
            if (!paramErrorIfDirNull.isEmpty()) {
                errorIfDirNull = (boolean) paramErrorIfDirNull.get(0).getEvaluation().getData();
            }
            ValueEvaluation paramDir = valueParams.getParams(PARAM_DIR).get(0).getEvaluation();
            File dir = (File) paramDir.getData();
            if (dir == null) {
                if (errorIfDirNull) {
                    return errorResultParamNull(PARAM_DIR, paramDir);
                } else {
                    return okResult(Collections.emptyList());
                }
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
            // TODO: 2019-02-23 logging
            /*if (filesMatching.isEmpty()) {
                System.out.println(getName() + " result empty for dir=" + dir.getAbsolutePath() + ", pattern: " + patternParam.toString());
            }*/
            return okResult(filesMatching);

        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }


}
