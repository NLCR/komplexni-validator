package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Pattern;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfFindFilesInDirByPattern extends EvaluationFunction {

    private static final String PARAM_DIR = "dir";
    private static final String PARAM_PATTERN = "pattern";

    public EfFindFilesInDirByPattern(Engine engine) {
        super(engine, ValueType.LIST_OF_FILES);
    }

    @Override
    public List<File> evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("nebyly zadány parametry (hodnotové)");
        }
        if (patternParams == null) {
            throw new IllegalStateException("nebyly zadány parametry (vzory)");
        }

        File dir = getDirFromValueParams();
        if (!dir.exists()) {
            throw new RuntimeException("soubor " + dir.getAbsolutePath() + " neexistuje");
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("soubor " + dir.getAbsolutePath() + " není adresář");
        } else if (!dir.canRead()) {
            throw new RuntimeException("nemám práva číst adresář " + dir.getAbsolutePath());
        } else {
            PatternParam pattern = getPatternFromPatternParams();
            //TODO: use pattern
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

    public File getDirFromValueParams() {
        List<ValueParam> varNameValues = valueParams.getParams(PARAM_DIR);
        if (varNameValues == null || varNameValues.size() == 0) {
            throw new RuntimeException("chybí parametr " + PARAM_DIR);
        } else if (varNameValues.size() > 1) {
            throw new RuntimeException("parametr " + PARAM_DIR + " musí být jen jeden");
        }

        ValueParam param = varNameValues.get(0);
        //TODO: tohle se opakuje, abstraktni metodu
        //kontrola typu
        if (param.getType() != ValueType.FILE) {
            throw new RuntimeException(String.format("parametr %s není očekávaného typu %s", PARAM_DIR, ValueType.FILE.toString()));
        }
        File result = (File) param.getValue();
        return result;
    }

    public PatternParam getPatternFromPatternParams() {
        PatternParam param = patternParams.getParam(PARAM_PATTERN);
        if (param == null) {
            throw new RuntimeException("parametr " + PARAM_PATTERN + " není definován");
        }
        return param;
    }
}
