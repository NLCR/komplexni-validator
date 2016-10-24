package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfFindFilesInDirByPattern extends EvaluationFunction {

    private static final String PARAM_DIR = "dir";
    private static final String PARAM_PATTERN = "pattern";

    //todo:pattern, zatim zadny a vezme vsechny soubory

    public EfFindFilesInDirByPattern(Engine engine) {
        super(engine, ValueType.LIST_OF_FILES);
    }

    @Override
    public List<File> evaluate() {
        if (valueParams == null) {
            throw new IllegalStateException("Nebyly zadány parametry");
        }
        File dir = getDirFromparams();
        if (!dir.exists()) {
            throw new RuntimeException("soubor " + dir.getAbsolutePath() + " neexistuje");
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("soubor " + dir.getAbsolutePath() + " není adresář");
        } else if (!dir.canRead()) {
            throw new RuntimeException("nemám práva číst adresář " + dir.getAbsolutePath());
        } else {
            File[] files = dir.listFiles();
            return Arrays.asList(files);
        }
    }

    public File getDirFromparams() {
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
}
