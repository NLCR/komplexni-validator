package rzehan.shared.engine;

import org.junit.Test;
import rzehan.shared.xsdValidation.XsdValidator;

import java.io.File;

/**
 * Created by martin on 4.11.16.
 */
public class XsdTest {

    @Test
    public void xsdTest() {

        File root = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fDMF/monograph_1.2");
        File xmlFile = new File(root, "fdmf.xml");
        File xsdFile = new File(root, "fdmf.xsd");
        XsdValidator.validate("INFO", xsdFile, xmlFile);
    }
}
