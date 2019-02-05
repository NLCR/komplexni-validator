package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.dev.XsdValidator;
import org.junit.Test;

import java.io.File;

/**
 * Created by Martin Řehánek on 4.11.16.
 */
public class XsdTest {

    @Test
    public void xsdTest() {

        File root = new File("src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2");
        File xmlFile = new File(root, "fdmf.xml");
        File xsdFile = new File(root, "fdmfConfig.xsd");
        XsdValidator.validate("INFO", xsdFile, xmlFile);
    }
}
