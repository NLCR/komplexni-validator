package rzehan.shared.engine.evaluationFunctions;

import org.junit.Test;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by martin on 21.10.16.
 */
public class EfProvidedFileTest {

    @Test
    public void ok() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED_FILE");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("file_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, "PSP_DIR"));
        evFunction.setValueParams(params);
        assertEquals(new File("/home/martin/zakazky/NKP-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52"), evFunction.evaluate());
    }

    @Test
    public void paramsNotSet() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED_FILE");
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //nebyly zadány parametry
        }
    }

    @Test
    public void missingParam() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED_FILE");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //chybí parametr file_id
        }
    }

    @Test
    public void duplicateParam() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED_FILE");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("file_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, "PSP_DIR"));
        params.addParam("file_id", new EvaluationFunction.ValueParamConstant(ValueType.STRING, "XYZ_DIR"));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            //fail();
        } catch (RuntimeException e) {
            //parametr file_id musí být jen jeden
        }
    }


    @Test
    public void invalidParamType() {
        Engine mgr = new Engine();
        EvaluationFunction evFunction = mgr.getEvaluationFunction("PROVIDED_FILE");
        EvaluationFunction.ValueParams params = new EvaluationFunction.ValueParams();
        params.addParam("file_id", new EvaluationFunction.ValueParamConstant(ValueType.FILE, "PSP_DIR"));
        evFunction.setValueParams(params);
        try {
            evFunction.evaluate();
            fail();
        } catch (RuntimeException e) {
            //parametr file_id není očekávaného typu STRING
        }
    }
}
