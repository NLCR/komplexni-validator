package nkp.pspValidator.shared;

import nkp.pspValidator.shared.biblio.BiblioTemplatesManager;
import nkp.pspValidator.shared.biblio.BiblioTemplatesParser;
import nkp.pspValidator.shared.biblio.DictionaryManager;
import nkp.pspValidator.shared.biblio.TechnicalTemplatesManager;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.types.MetadataFormat;

import java.io.File;
import java.util.Map;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class ValidatorFactory {

    public static Validator buildValidator(FdmfConfiguration fdmfConfiguration, File pspRootDir, DictionaryManager dictionaryManager) throws ValidatorConfigurationException {
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

        // process bibliographic profile files
        BiblioTemplatesManager biblioMgr = new BiblioTemplatesManager(new BiblioTemplatesParser(dictionaryManager));
        for (File templateFile : fdmfConfiguration.getBiblioDcTemplates()) {
            biblioMgr.processFile(templateFile, MetadataFormat.DC);
        }
        for (File templateFile : fdmfConfiguration.getBiblioModsTemplates()) {
            biblioMgr.processFile(templateFile, MetadataFormat.MODS);
        }
        engine.setBiblioMgr(biblioMgr);

        //process technical profile files
        TechnicalTemplatesManager techMgr = new TechnicalTemplatesManager(new BiblioTemplatesParser(dictionaryManager));
        for (File templateFile : fdmfConfiguration.getTechProfiles()) {
            techMgr.processFile(templateFile);
        }
        engine.setTechnicalTemplatesManager(techMgr);

        return new Validator(engine);
    }

}
