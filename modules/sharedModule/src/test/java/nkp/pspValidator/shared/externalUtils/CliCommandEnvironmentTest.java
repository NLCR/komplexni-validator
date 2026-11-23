package nkp.pspValidator.shared.externalUtils;

import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Proměnné prostředí předané spouštěnému procesu navíc: proces je dostane, prostředí validátoru se nemění. */
public class CliCommandEnvironmentTest {

    private static final String VARIABLE = "KV_TEST_PROMENNA";

    private static String[] echoCommand() {
        return File.separatorChar == '\\'
                ? new String[]{"cmd", "/c", "echo %" + VARIABLE + "%"}
                : new String[]{"sh", "-c", "echo $" + VARIABLE};
    }

    @Test
    public void extraEnvironmentReachesTheProcess() throws Exception {
        CliCommand.Result result = new CliCommand(echoCommand(), Collections.singletonMap(VARIABLE, "hodnota 1")).execute();
        assertEquals("hodnota 1", result.getStdout().trim());
        assertEquals(0, result.getExitValue());
    }

    @Test
    public void withoutExtraEnvironmentTheVariableIsNotSet() throws Exception {
        CliCommand.Result result = new CliCommand(echoCommand()).execute();
        //sh vypise prazdny radek, cmd nerozvinuty nazev promenne
        assertFalse(result.getStdout(), result.getStdout().contains("hodnota 1"));
        assertEquals(null, System.getenv(VARIABLE));
    }
}
