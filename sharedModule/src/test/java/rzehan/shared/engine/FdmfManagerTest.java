package rzehan.shared.engine;

import org.junit.Test;
import rzehan.shared.engine.exceptions.InvalidXPathExpressionException;
import rzehan.shared.engine.exceptions.PspDataException;
import rzehan.shared.engine.exceptions.XmlParsingException;

import java.io.File;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by martin on 2.11.16.
 */
public class FdmfManagerTest {


    @Test
    public void detectDmfTypeMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        FdmfManager fdmfManager = new FdmfManager();
        try {
            FdmfManager.DmfType dmfType = fdmfManager.detectDmfType(pspRootDir);
            assertEquals(FdmfManager.DmfType.MONOGRAPH, dmfType);
        } catch (PspDataException e) {
            e.printStackTrace();
        } catch (XmlParsingException e) {
            e.printStackTrace();
        } catch (InvalidXPathExpressionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void detectDmfTypePeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        FdmfManager fdmfManager = new FdmfManager();
        try {
            FdmfManager.DmfType dmfType = fdmfManager.detectDmfType(pspRootDir);
            assertEquals(FdmfManager.DmfType.PERIODICAL, dmfType);
        } catch (PspDataException e) {
            e.printStackTrace();
        } catch (XmlParsingException e) {
            e.printStackTrace();
        } catch (InvalidXPathExpressionException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void detectDmfTypeInvalid() {
        File pspRootDir = new File("src/test/resources/monograph_wrongType/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        FdmfManager fdmfManager = new FdmfManager();
        try {
            FdmfManager.DmfType dmfType = fdmfManager.detectDmfType(pspRootDir);
            System.out.println(dmfType);
            fail();
        } catch (PspDataException e) {
            e.printStackTrace();
        } catch (XmlParsingException e) {
            e.printStackTrace();
        } catch (InvalidXPathExpressionException e) {
            e.printStackTrace();
        }
    }

}
