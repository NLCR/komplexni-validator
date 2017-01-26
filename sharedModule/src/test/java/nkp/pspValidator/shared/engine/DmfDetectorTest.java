package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class DmfDetectorTest {


    @Test
    public void detectDmfTypeMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        DmfDetector dmfDetector = new DmfDetector();
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            assertEquals(Dmf.Type.MONOGRAPH, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfTypePeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        DmfDetector dmfDetector = new DmfDetector();
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            assertEquals(Dmf.Type.PERIODICAL, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void detectDmfTypeInvalid() {
        File pspRootDir = new File("src/test/resources/monograph_wrongType/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        DmfDetector dmfDetector = new DmfDetector();
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            System.out.println(dmfType);
            fail();
        } catch (PspDataException e) {
            //ok
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        DmfDetector dmfDetector = new DmfDetector();
        try {
            String version = dmfDetector.detectDmfVersionFromInfoOrDefault(Dmf.Type.MONOGRAPH, pspRootDir);
            assertEquals("1.2", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionPeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        DmfDetector dmfDetector = new DmfDetector();
        try {
            String version = dmfDetector.detectDmfVersionFromInfoOrDefault(Dmf.Type.PERIODICAL, pspRootDir);
            assertEquals("1.6", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }


}
