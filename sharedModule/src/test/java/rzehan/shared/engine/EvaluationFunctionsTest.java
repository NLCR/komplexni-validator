package rzehan.shared.engine;

import org.junit.Test;
import rzehan.shared.engine.evaluationFunctions.EvaluationFunction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by martin on 21.10.16.
 */
public class EvaluationFunctionsTest {


    @Test
    public void efProvidedString() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED_STRING");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("string_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, "PSP_ID"));
        //todo: chyba pokud vice string_name, nebo kdyz chybi
        //todo: mozna chyba, pokud pritomen necekany parametr
        //params.addParam("string_name", new EvaluationFunction.ValueParamConstant("PSP_ID"));
        evFunction.setValueParams(params);
        assertEquals("b50eb6b0-f0a4-11e3-b72e-005056827e52", evFunction.evaluate());
    }


    @Test
    public void efReturnFirstFileFromList() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("RETURN_FIRST_FILE_FROM_LIST");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();

        List<File> files = new ArrayList<>();
        files.add(new File("/first"));
        files.add(new File("/second"));

        params.addParam("file_list", new EvaluationFunction.ValueParamConstant(ValueType.LIST_OF_FILES, files));
        //todo: chyba pokud vice string_name, nebo kdyz chybi
        //todo: mozna chyba, pokud pritomen necekany parametr
        //params.addParam("string_name", new EvaluationFunction.ValueParamConstant("PSP_ID"));
        evFunction.setValueParams(params);
        assertEquals(new File("/first"), evFunction.evaluate());
    }


    @Test
    public void efFindFilesInDirByPattern() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("FIND_FILES_IN_DIR_BY_PATTERN");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();

        params.addParam("dir", new EvaluationFunction.ValueParamConstant(ValueType.FILE, new File("/home/martin/zakazky/NKP-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52")));
        //todo: chyba pokud vice string_name, nebo kdyz chybi
        //todo: mozna chyba, pokud pritomen necekany parametr
        //params.addParam("string_name", new EvaluationFunction.ValueParamConstant("PSP_ID"));
        evFunction.setValueParams(params);
        List<File> files = (List<File>) evFunction.evaluate();
        assertEquals(8, files.size());
    }







    /*@Test
    public void efProvidedMultipleValues() {
        Engine mgr = new Engine();

        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("var_name", new EvaluationFunction.ValueParamConstant("PSP_ID"));
        params.addParam("var_name", new EvaluationFunction.ValueParamConstant("OTHER"));
        //todo: zkontrolovat, ze spravna vyjimka
    }*/

    /*@Test
    public void efProvidedInvalidParamType() {
        Engine mgr = new Engine();

        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("var_name", new EvaluationFunction.ValueParamConstant(Integer.valueOf(3)));
        //todo: zkontrolovat, ze spravna vyjimka
    }*/


}
