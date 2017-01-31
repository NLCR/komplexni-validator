package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.util.Map;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class ValidatorFactory {

    public static Validator buildValidator(FdmfConfiguration fdmfConfiguration, File pspRootDir) throws ValidatorConfigurationException {
        Engine engine = new Engine(fdmfConfiguration.getImageValidator());
        //psp dir
        engine.setProvidedFile("PSP_DIR", pspRootDir);
        //init with provided files
        Map<String, File> providedFiles = fdmfConfiguration.getProvidedFiles();
        for (String id : providedFiles.keySet()) {
            File file = providedFiles.get(id);
            //System.out.println(String.format("id: %s, provided file: %s", id, file.getAbsolutePath()));
            engine.setProvidedFile(id, file);
        }
        // init configuration files (patterns, variables, rules)
        for (File configFile : fdmfConfiguration.getFdmfConfigFiles()) {
            engine.processConfigFile(configFile);
        }
        //TODO: biblio validator files

        return new Validator(engine);
    }

}
