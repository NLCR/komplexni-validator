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
public class PspManagerTest {


    @Test
    public void detectDmfTypeMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        PspManager pspManager = new PspManager();
        try {
            PspManager.DmfType dmfType = pspManager.detectDmfType(pspRootDir);
            assertEquals(PspManager.DmfType.MONOGRAPH, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfTypePeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        PspManager pspManager = new PspManager();
        try {
            PspManager.DmfType dmfType = pspManager.detectDmfType(pspRootDir);
            assertEquals(PspManager.DmfType.PERIODICAL, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void detectDmfTypeInvalid() {
        File pspRootDir = new File("src/test/resources/monograph_wrongType/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        PspManager pspManager = new PspManager();
        try {
            PspManager.DmfType dmfType = pspManager.detectDmfType(pspRootDir);
            System.out.println(dmfType);
            fail();
        } catch (PspDataException e) {
            //ok
        } catch (XmlParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        PspManager pspManager = new PspManager();
        try {
            String version = pspManager.detectDmfVersion(pspRootDir);
            assertEquals("1.2", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionPeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        PspManager pspManager = new PspManager();
        try {
            String version = pspManager.detectDmfVersion(pspRootDir);
            assertEquals("1.6", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }


}
