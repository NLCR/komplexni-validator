package nkp.pspValidator.gui;

import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.ValidatorConfigurationManager;

import java.io.File;
import java.io.IOException;

/**
 * Společné pomůcky testů GUI bez JavaFX: ConfigurationManager nad dočasným adresářem (jako by aplikace běžela
 * z instalace jpackage, nastavení v config.properties v dočasném adresáři) a registr fDMF z repozitáře.
 */
public final class GuiTestSupport {

    public static final File VALIDATOR_CONFIG_DIR = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig");
    public static final File SHARED_TEST_RESOURCES = new File("../sharedModule/src/test/resources");

    private GuiTestSupport() {
    }

    /** Nový ConfigurationManager s prázdným config.properties v daném (dočasném) adresáři. */
    public static ConfigurationManager configurationManager(File root) throws IOException {
        File jarDir = new File(root, "KomplexniValidator/app");
        if (!new File(jarDir, "validatorConfig").mkdirs()) {
            throw new IOException("nelze vytvořit " + jarDir);
        }
        File home = new File(root, "home");
        File cwd = new File(root, "cwd");
        home.mkdirs();
        cwd.mkdirs();
        ConfigurationManager.Locations locations = ConfigurationManager.Locations.resolve(OperatingSystem.WINDOWS, true, jarDir,
                home, new File(home, "AppData/Roaming").getPath(), null, cwd);
        return new ConfigurationManager(Platform.detectOs(), locations);
    }

    public static FdmfRegistry registry() throws Exception {
        return new FdmfRegistry(new ValidatorConfigurationManager(VALIDATOR_CONFIG_DIR));
    }
}
