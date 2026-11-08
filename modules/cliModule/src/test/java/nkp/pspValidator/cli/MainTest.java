package nkp.pspValidator.cli;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Zakladni test vstupu CLI: napoveda, verze a neplatne parametry nesmi vyhodit vyjimku ani zastavit JVM.
 * Validace balicku se testuje jinde (sharedModule); puvodni MainTest byl vyvojarsky runner s cestami mimo repo.
 */
public class MainTest {

    private PrintStream origOut;
    private PrintStream origErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @Before
    public void redirect() throws Exception {
        origOut = System.out;
        origErr = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, "UTF-8"));
        System.setErr(new PrintStream(err, true, "UTF-8"));
    }

    @After
    public void restore() {
        System.setOut(origOut);
        System.setErr(origErr);
    }

    private String stdout() throws Exception {
        return out.toString("UTF-8");
    }

    private String stderr() throws Exception {
        return err.toString("UTF-8");
    }

    @Test
    public void helpPrintsUsage() throws Exception {
        Main.main(null, new String[]{"--" + Params.HELP});
        assertTrue(stdout().contains("--" + Params.ACTION));
        assertTrue(stdout().contains("--" + Params.CONFIG_DIR));
        assertTrue(stderr().isEmpty());
    }

    @Test
    public void versionPrintsVersion() throws Exception {
        Main.main(null, new String[]{"--" + Params.VERSION});
        assertTrue(stdout().contains("Komplexni validator CLI verze"));
    }

    @Test
    public void missingActionReportsErrorAndHelp() throws Exception {
        Main.main(null, new String[]{});
        assertTrue(stderr().contains(Params.ACTION));
        assertTrue(stdout().contains("--" + Params.ACTION));
    }

    @Test
    public void unknownActionReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "NEEXISTUJICI_AKCE"});
        assertTrue(stderr().contains("NEEXISTUJICI_AKCE"));
    }

    @Test
    public void validatePspWithoutPspReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_PSP", "--" + Params.CONFIG_DIR,
                "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig"});
        assertTrue(stderr().contains("--" + Params.PSP));
    }

    @Test
    public void validatePspWithoutConfigDirReportsError() throws Exception {
        Main.main(null, new String[]{"--" + Params.ACTION, "VALIDATE_PSP"});
        assertTrue(stderr().contains("--" + Params.CONFIG_DIR));
    }
}
