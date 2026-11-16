package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VfCheckIdentifiersNoDuplicatesTest {

    private static final String VF_NAME = "checkIdentifiersNoDuplicates";

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
    }

    private ValidationResult validate(List<Identifier> ids, List<String> ignoredTypes) {
        VfCheckIdentifiersNoDuplicates vf = new VfCheckIdentifiersNoDuplicates(VF_NAME, engine);
        return vf.validate(Collections.singletonList(ids), ignoredTypes);
    }

    @Test
    public void noDuplicates() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "b"), new Identifier("urnnbn", "a"));
        assertFalse(validate(ids, Collections.emptyList()).hasProblems());
    }

    @Test
    public void duplicateOfFirstValue() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "b"), new Identifier("uuid", "a"));
        assertEquals(1, validate(ids, Collections.emptyList()).getProblems().size());
    }

    /**
     * Duplicita jine nez prvni hodnoty daneho typu (uuid A, B, B); puvodni implementace ji neodhalila.
     */
    @Test
    public void duplicateOfLaterValue() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "b"), new Identifier("uuid", "b"));
        assertEquals(1, validate(ids, Collections.emptyList()).getProblems().size());
    }

    @Test
    public void sameValueDifferentTypeIsNotDuplicate() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("sysno", "a"));
        assertFalse(validate(ids, Collections.emptyList()).hasProblems());
    }

    @Test
    public void ignoredTypeNotChecked() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("uuid", "a"));
        assertFalse(validate(ids, Collections.singletonList("uuid")).hasProblems());
    }

    @Test
    public void tripleDuplicateReportedTwice() {
        List<Identifier> ids = Arrays.asList(
                new Identifier("uuid", "a"), new Identifier("uuid", "a"), new Identifier("uuid", "a"));
        assertEquals(2, validate(ids, Collections.emptyList()).getProblems().size());
    }

    /**
     * Uroven hlasky: vychozi ERROR (issue #224), volitelny parametr level ji zmeni.
     */
    @Test
    public void levelIsErrorByDefaultAndConfigurable() {
        List<Identifier> ids = Arrays.asList(new Identifier("uuid", "a"), new Identifier("uuid", "a"));
        assertEquals(nkp.pspValidator.shared.engine.Level.ERROR, validate(ids, Collections.emptyList()).getProblems().get(0).getLevel());
        VfCheckIdentifiersNoDuplicates vf = new VfCheckIdentifiersNoDuplicates(VF_NAME, engine);
        ValidationResult result = vf.validate(Collections.singletonList(ids), Collections.emptyList(), nkp.pspValidator.shared.engine.Level.WARNING);
        assertEquals(1, result.getProblems().size());
        assertEquals(nkp.pspValidator.shared.engine.Level.WARNING, result.getProblems().get(0).getLevel());
    }

    /**
     * fDMF e-publikaci hlasi duplicitni identifikatory jako WARNING (chovani Validatoru e-publikaci), ostatni fDMF
     * parametr level nemaji a plati vychozi ERROR.
     */
    @Test
    public void ePublicationFdmfUseWarning() throws Exception {
        java.io.File fdmfDir = new java.io.File("src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF");
        java.util.regex.Pattern call = java.util.regex.Pattern.compile(
                "<validation functionName=\"checkIdentifiersNoDuplicates\">\\s*<params>\\s*(<!--.*?-->\\s*)?<value name=\"level\" type=\"LEVEL\">WARNING</value>", java.util.regex.Pattern.DOTALL);
        for (String name : new String[]{"emonograph_2.3", "emonograph_2.4", "emonograph_3.0", "emonograph_3.1",
                "eperiodical_2.3", "eperiodical_2.4", "eperiodical_2.5", "eperiodical_2.6", "composed_perio_1.3"}) {
            String rules = new String(java.nio.file.Files.readAllBytes(new java.io.File(fdmfDir, name + "/rules.xml").toPath()), java.nio.charset.StandardCharsets.UTF_8);
            int calls = rules.split("functionName=\"checkIdentifiersNoDuplicates\"", -1).length - 1;
            java.util.regex.Matcher m = call.matcher(rules);
            int withWarning = 0;
            while (m.find()) withWarning++;
            assertEquals(name, 2, calls);
            assertEquals(name, calls, withWarning);
        }
        for (String name : new String[]{"monograph_2.3", "periodical_2.2", "fund_unit_0.2"}) {
            String rules = new String(java.nio.file.Files.readAllBytes(new java.io.File(fdmfDir, name + "/rules.xml").toPath()), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = call.matcher(rules);
            assertFalse(name, m.find());
        }
    }
}
