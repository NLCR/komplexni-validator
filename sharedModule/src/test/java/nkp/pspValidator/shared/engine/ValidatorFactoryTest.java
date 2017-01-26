package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.junit.Test;

import java.io.File;

/**
 * Created by Martin Řehánek on 4.11.16.
 */
public class ValidatorFactoryTest {

    @Test
    public void test() throws ValidatorConfigurationException {
        File fdmfRootDir = new File("src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2");
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        /*Validator validator = ValidatorFactory.buildValidator(fdmfRootDir, pspRootDir);
        validator.run(false);*/
    }
}
