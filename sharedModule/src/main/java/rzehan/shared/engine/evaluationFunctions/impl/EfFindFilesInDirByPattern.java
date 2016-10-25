package rzehan.shared.engine.evaluationFunctions.impl;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;
import rzehan.shared.engine.evaluationFunctions.PatternParam;
import rzehan.shared.engine.evaluationFunctions.ValueParam;

import java.io.File;
import java.util.*;

/**
 * Created by martin on 21.10.16.
 */
public class EfFindFilesInDirByPattern extends EvaluationFunction {

    private static final String PARAM_DIR = "dir";
    private static final String PARAM_PATTERN = "pattern";

    public EfFindFilesInDirByPattern(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.LIST_OF_FILES)
                .withPatternParam(PARAM_PATTERN)
                .withValueParam(PARAM_DIR, ValueType.FILE, 1, 1));
    }

    @Override
    public List<File> evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("nebyly zadány parametry (hodnotové)");
        }
        if (patternParams == null) {
            throw new IllegalStateException("nebyly zadány parametry (vzory)");
        }
        contract.checkComplience(valueParams, patternParams);

        File dir = (File) valueParams.getParams(PARAM_DIR).get(0).getValue();
        if (!dir.exists()) {
            throw new RuntimeException("soubor " + dir.getAbsolutePath() + " neexistuje");
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("soubor " + dir.getAbsolutePath() + " není adresář");
        } else if (!dir.canRead()) {
            throw new RuntimeException("nemám práva číst adresář " + dir.getAbsolutePath());
        } else {
            PatternParam pattern = patternParams.getParam(PARAM_PATTERN);
            File[] files = dir.listFiles();
            List<File> filesMatching = new ArrayList<>(files.length);
            for (File file : files) {
                if (pattern.matches(file.getName())) {
                    filesMatching.add(file);
                }
            }
            return filesMatching;
        }
    }

}
