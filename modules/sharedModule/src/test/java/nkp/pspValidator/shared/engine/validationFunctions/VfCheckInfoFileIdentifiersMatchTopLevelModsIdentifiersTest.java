package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VfCheckInfoFileIdentifiersMatchTopLevelModsIdentifiersTest {

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }

    private ValidationResult validate(List<Identifier> infoIds, List<Identifier> modsIds, boolean checkModsIdsInInfo) {
        VfCheckInfoFileIdentifiersMatchTopLevelModsIdentifiers vf =
                new VfCheckInfoFileIdentifiersMatchTopLevelModsIdentifiers("checkInfoFileIdentifiersMatchTopLevelModsIdentifiers", engine);
        return vf.validate(infoIds, Collections.singletonList(modsIds), null, checkModsIdsInInfo);
    }

    @Test
    public void infoIdMissingInModsIsError() {
        ValidationResult result = validate(
                Arrays.asList(new Identifier("uuid", "a"), new Identifier("sysno", "1")),
                Collections.singletonList(new Identifier("uuid", "a")), false);
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
    }

    /**
     * Vychozi chovani (ostatni fDMF): identifikator z MODS, ktery v INFO chybi, se nehlasi.
     */
    @Test
    public void modsIdMissingInInfoIgnoredByDefault() {
        ValidationResult result = validate(
                Collections.singletonList(new Identifier("uuid", "a")),
                Arrays.asList(new Identifier("uuid", "a"), new Identifier("sysno", "1")), false);
        assertFalse(result.hasProblems());
    }

    @Test
    public void modsIdMissingInInfoIsWarningOnRequest() {
        ValidationResult result = validate(
                Collections.singletonList(new Identifier("uuid", "a")),
                Arrays.asList(new Identifier("uuid", "a"), new Identifier("sysno", "1")), true);
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.WARNING, result.getProblems().get(0).getLevel());
    }
}
