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

    private final DmfDetector dmfDetector = new DmfDetector();

    @Test
    public void detectDmfTypeMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
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
        try {
            String version = dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.MONOGRAPH, pspRootDir);
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
        try {
            String version = dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.PERIODICAL, pspRootDir);
            assertEquals("1.6", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void versions() throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        //monograph
        File mon12Dir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        assertEquals(resolverMon(mon12Dir, null, "123").getVersion(), "123");
        assertEquals(resolverMon(mon12Dir, "1", "123").getVersion(), "123");
        assertEquals(resolverMon(mon12Dir, "1", null).getVersion(), "1.2");

        //periodical
        File per14Dir = new File("src/test/resources/periodical_1.4/ope301-00000v");
        assertEquals(resolverPer(per14Dir, null, "123").getVersion(), "123");
        assertEquals(resolverPer(per14Dir, "1", "123").getVersion(), "123");
        assertEquals(resolverPer(per14Dir, "1", null).getVersion(), "1.4");

        //audio document gramophone
        File adg03Dir = new File("src/test/resources/audio_doc_gram_0.3/1234567890");
        assertEquals(resolverSr(adg03Dir, null, "123").getVersion(), "123");
        assertEquals(resolverSr(adg03Dir, "1", "123").getVersion(), "123");
        assertEquals(resolverSr(adg03Dir, null, null).getVersion(), "0.3");
        assertEquals(resolverSr(adg03Dir, "1", null).getVersion(), "0.3");
    }


    private Dmf resolverMon(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = forced;
        params.forcedDmfPerVersion = null;
        params.forcedDmfAdgVersion = null;
        params.preferredDmfMonVersion = preferred;
        params.preferredDmfPerVersion = null;
        params.preferredDmfAdgVersion = null;
        return dmfDetector.resolveDmf(pspDir, params);
    }

    private Dmf resolverPer(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = null;
        params.forcedDmfPerVersion = forced;
        params.forcedDmfAdgVersion = null;
        params.preferredDmfMonVersion = null;
        params.preferredDmfPerVersion = preferred;
        params.preferredDmfAdgVersion = null;
        return dmfDetector.resolveDmf(pspDir, params);
    }

    private Dmf resolverSr(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = null;
        params.forcedDmfPerVersion = null;
        params.forcedDmfAdgVersion = forced;
        params.preferredDmfMonVersion = null;
        params.preferredDmfPerVersion = null;
        params.preferredDmfAdgVersion = preferred;
        return dmfDetector.resolveDmf(pspDir, params);
    }


}
