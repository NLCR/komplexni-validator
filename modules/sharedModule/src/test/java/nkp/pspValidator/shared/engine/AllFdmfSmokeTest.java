package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Smoke test pro kazdou fDMF v validatorConfig/fDMF (aktualne 30 adresaru): konfigurace se nacte, validator se postavi
 * a projde vsechna pravidla nad prazdnym adresarem (typ a verze jsou vynuceny) bez "necekane chyby" a bez porusenych
 * kontraktu funkci. Nad prazdnym balickem je ocekavane, ze je nevalidni; test nic netvrdi o obsahu hlaseni.
 * <p>
 * Vyjimka: EmptyParamEvaluationException (pravidla nad neexistujicimi adresari s daty) je u prazdneho balicku legitimni,
 * stejne jako ve FundUnitSmokeTest.
 */
@RunWith(Parameterized.class)
public class AllFdmfSmokeTest {

    private static ValidatorConfigurationManager configManager;
    private static FdmfRegistry registry;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> fdmfDirNames() {
        List<Object[]> result = new ArrayList<>();
        for (File dir : FdmfTestSupport.fdmfDirs()) {
            result.add(new Object[]{dir.getName()});
        }
        return result;
    }

    private final String fdmfDirName;

    public AllFdmfSmokeTest(String fdmfDirName) {
        this.fdmfDirName = fdmfDirName;
    }

    @BeforeClass
    public static void loadRegistry() throws Exception {
        configManager = FdmfTestSupport.configManager();
        registry = new FdmfRegistry(configManager);
    }

    @Test
    public void buildsAndRunsOverEmptyDirectory() throws Exception {
        Dmf dmf = FdmfTestSupport.dmfFromDirName(fdmfDirName);
        File pspDir = tmp.newFolder("psp");
        String log = FdmfTestSupport.run(registry, configManager, dmf, pspDir);
        assertTrue(fdmfDirName + ": validace nedobehla do zaverecneho souhrnu", log.contains("balík je:"));
        for (String line : log.split("\n")) {
            boolean crash = (line.contains("nečekaná chyba") && !line.contains("EmptyParamEvaluationException"))
                    || line.contains("nesplněn kontrakt");
            assertTrue(fdmfDirName + ": konfigurace fDMF obsahuje chybu nebo funkce spadla: " + line.trim(), !crash);
        }
    }
}
