package nkp.pspValidator.shared;

import nkp.pspValidator.shared.metadataProfile.biblio.BibliographicMetadataProfilesManager;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;
import nkp.pspValidator.shared.metadataProfile.mets.MetsProfilesManager;
import nkp.pspValidator.shared.metadataProfile.tech.TechnicalMetadataProfilesManager;
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
        Engine engine = new Engine(fdmfConfiguration.getBinaryFileValidator());
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
        BibliographicMetadataProfilesManager biblioMgr = new BibliographicMetadataProfilesManager(new MetadataProfileParser(dictionaryManager));
        for (File profileFile : fdmfConfiguration.getBiblioDcProfiles()) {
            biblioMgr.processFile(profileFile, MetadataFormat.DC);
        }
        for (File profileFile : fdmfConfiguration.getBiblioModsProfiles()) {
            biblioMgr.processFile(profileFile, MetadataFormat.MODS);
        }
        engine.setBibliographicMetadataProfilesManager(biblioMgr);

        //process technical profile files
        TechnicalMetadataProfilesManager techMgr = new TechnicalMetadataProfilesManager(new MetadataProfileParser(dictionaryManager));
        for (File profileFile : fdmfConfiguration.getTechProfiles()) {
            techMgr.processFile(profileFile);
        }
        engine.setTechnicalMetadataProfilesManager(techMgr);

        //process mets profile files
        MetsProfilesManager metsMgr = new MetsProfilesManager(new MetadataProfileParser(dictionaryManager));
        for (File profileFile : fdmfConfiguration.getMetsProfiles()) {
            metsMgr.processFile(profileFile);
        }
        engine.setMetsProfilesManager(metsMgr);

        return new Validator(engine);
    }

}
