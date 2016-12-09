package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ImageCopy;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.validation.ImageValidator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import static nkp.pspValidator.shared.FileUtils.checkDirExistAndReadable;
import static nkp.pspValidator.shared.FileUtils.checkFileExistAndReadable;

/**
 * Created by martin on 2.11.16.
 */
public class ValidatorFactory {

    private static final String FDMF_XSD_FILE = "fdmf.xsd";
    private static final String XSD_DIR = "xsd";
    private static final String J2K_PROFILES_DIR = "jpeg2000Profiles";
    private static final String J2K_PROFILES_UC_DIR = "uc";
    private static final String J2K_PROFILES_MC_DIR = "mc";


    public static Validator buildValidator(FdmfConfiguration fdmfConfiguration, File pspRootDir, ImageUtilManager imageUtilManager) throws ValidatorConfigurationException {
        Engine engine = new Engine(fdmfConfiguration.getImageValidator());
        //init with provided files
        Map<String, File> providedFiles = fdmfConfiguration.getProvidedFiles();
        for (String id : providedFiles.keySet()) {
            File file = providedFiles.get(id);
            engine.setProvidedFile(id, file);
        }
        // init configuration files (patterns, variables, rules)
        for (File configFile : fdmfConfiguration.getFdmfConfigFiles()) {
            engine.processConfigFile(configFile);
        }
        //psp data
        engine.setProvidedFile("PSP_DIR", pspRootDir);
        return new Validator(engine);
    }

    @Deprecated
    public static Validator buildValidator(File fdmfRoot, File pspRootDir, ImageUtilManager imageUtilManager) throws ValidatorConfigurationException {
        checkDirExistAndReadable(fdmfRoot);
        checkDirExistAndReadable(pspRootDir);

        ImageValidator validator = new ImageValidator(imageUtilManager);
        for (ImageCopy copy : ImageCopy.values()) {
            for (ImageUtil util : ImageUtil.values()) {
                if (imageUtilManager.isUtilAvailable(util)) {
                    validateAndProcessJ2kProfileConfig(fdmfRoot, validator, copy, util);
                }
            }
        }

        Engine engine = new Engine(validator);
        engine.setProvidedFile("PSP_DIR", pspRootDir);
        //provided xsd
        File xsdRoot = new File(fdmfRoot, XSD_DIR);
        checkDirExistAndReadable(xsdRoot);
        engine.setProvidedFile("INFO_XSD_FILE", findXsdFile(xsdRoot, "DMF-info", "info_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("ALTO_XSD_FILE", findXsdFile(xsdRoot, "ALTO", "alto_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("CMD_XSD_FILE", findXsdFile(xsdRoot, "copyrightMD", "cmd_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("DC_XSD_FILE", findXsdFile(xsdRoot, "Dublin Core", "dc_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("METS_XSD_FILE", findXsdFile(xsdRoot, "METS", "mets_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("MIX_XSD_FILE", findXsdFile(xsdRoot, "MIX", "mix_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("MODS_XSD_FILE", findXsdFile(xsdRoot, "MODS", "mods_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("PREMIS_XSD_FILE", findXsdFile(xsdRoot, "PREMIS", "premis_[0-9]+(\\.([0-9])+)*\\.xsd"));

        //nacteni patternu, promennych, pravidel etc.
        File fdmfXsdFile = new File(fdmfRoot, FDMF_XSD_FILE);
        checkFileExistAndReadable(fdmfXsdFile);
        //validateAndProcessFdmfConfig(engine, fdmfXsdFile, fdmfRoot, "fdmf.xml");
        validateAndProcessFdmfConfig(engine, fdmfXsdFile, fdmfRoot, "namespaces.xml");
        validateAndProcessFdmfConfig(engine, fdmfXsdFile, fdmfRoot, "patterns.xml");
        validateAndProcessFdmfConfig(engine, fdmfXsdFile, fdmfRoot, "variables.xml");
        validateAndProcessFdmfConfig(engine, fdmfXsdFile, fdmfRoot, "rules.xml");

        //nacteni sablon pro validaci biblio metadata
        //TODO: poradne
        //TODO: validovat pomoci xsd
        ///home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/biblioProfiles/dc/title.xml
        //engine.setProvidedFile("BIBLIO_PROFILE_DC_TITLE", new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/biblioProfiles/dc/title.xml"));

        return new Validator(engine);
    }

    private static void validateAndProcessJ2kProfileConfig(File fdmfRoot, ImageValidator validator, ImageCopy copy, ImageUtil util) throws ValidatorConfigurationException {
        File rootFile = new File(fdmfRoot, J2K_PROFILES_DIR);
        checkDirExistAndReadable(rootFile);
        File copyFile = buildImageCopyDir(rootFile, copy);
        checkDirExistAndReadable(copyFile);
        //TODO: xsd pro validaci tohohle souboru podle xsd
        File configFile = new File(copyFile, util.getProfileFileName());
        checkFileExistAndReadable(configFile);
        validator.processProfile(util, copy, configFile);
    }

    private static File buildImageCopyDir(File rootFile, ImageCopy copy) {
        switch (copy) {
            case USER:
                return new File(rootFile, J2K_PROFILES_UC_DIR);
            case MASTER:
                return new File(rootFile, J2K_PROFILES_MC_DIR);
            default:
                throw new IllegalStateException();
        }
    }


    private static void validateAndProcessFdmfConfig(Engine engine, File fdmfXsdFile, File fdmfRoot, String fileName) throws ValidatorConfigurationException {
        File configFile = new File(fdmfRoot, fileName);
        checkFileExistAndReadable(configFile);
        //TODO: v produkci povolit validaci konfiguracniho souboru podle xsd
        //XsdValidator.validate(fileName, fdmfXsdFile, configFile);
        engine.processConfigFile(configFile);
    }


    private static File findXsdFile(File xsdDir, String formatName, String filePattern) throws ValidatorConfigurationException {
        File[] files = xsdDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(filePattern);
            }
        });
        if (files.length == 0) {
            throw new ValidatorConfigurationException(String.format("nenalezen soubor XSD pro validaci formátu %s", formatName));
        } else if (files.length > 1) {
            throw new ValidatorConfigurationException(String.format("nalezeno více souborů XSD pro validaci formátu %s a není jasné, který použít", formatName));
        } else {
            File file = files[0];
            if (file.isDirectory()) {
                throw new ValidatorConfigurationException(String.format("soubor %s je adresář", file.getAbsolutePath()));
            } else if (!file.canRead()) {
                throw new ValidatorConfigurationException(String.format("nelze číst obsah souboru %s", file.getAbsolutePath()));
            }
            checkFileExistAndReadable(file);
            return file;
        }
    }


}
