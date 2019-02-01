package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ImageCopy;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.validation.ImageValidator;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nkp.pspValidator.shared.FileUtils.checkDirExistAndReadable;
import static nkp.pspValidator.shared.FileUtils.checkFileExistAndReadable;

/**
 * Created by Martin Řehánek on 9.12.16.
 */
public class FdmfConfiguration {

    private static final String XSD_DIR = "xsd";
    private static final String J2K_PROFILES_DIR = "jpeg2000Profiles";
    private static final String J2K_PROFILES_UC_DIR = "uc";
    private static final String J2K_PROFILES_MC_DIR = "mc";

    private static final String BIBLIO_PROFILES_DIR = "biblioProfiles";
    private static final String BIBLIO_PROFILES_DC_DIR = "dc";
    private static final String BIBLIO_PROFILES_MODS_DIR = "mods";

    private static final String TECH_PROFILES_DIR = "techProfiles";
    private static final String METS_PROFILES_DIR = "metsProfiles";

    private final File fdmfRoot;
    private final File fdmfConfigXsd;
    private final File j2kProfileConfigXsd;
    private final File metadataProfileXsd;

    private final Map<String, File> providedFiles = new HashMap<>();
    private final List<File> fdmfConfigFiles = new ArrayList<>();
    private final List<File> biblioModsProfiles = new ArrayList<>();
    private final List<File> biblioDcProfiles = new ArrayList<>();
    private final List<File> techProfiles = new ArrayList<>();
    private List<File> metsProfiles = new ArrayList<>();

    private ImageValidator imageValidator;


    public FdmfConfiguration(File fdmfRoot, File fdmfConfigXsd, File j2kProfileConfigXsd, File metadataProfileXsd) throws ValidatorConfigurationException {
        this.fdmfRoot = fdmfRoot;
        this.fdmfConfigXsd = fdmfConfigXsd;
        this.j2kProfileConfigXsd = j2kProfileConfigXsd;
        this.metadataProfileXsd = metadataProfileXsd;
        init();
    }

    private void init() throws ValidatorConfigurationException {
        checkDirExistAndReadable(fdmfRoot);

        //nacteni xsd pro metadata
        File xsdRoot = new File(fdmfRoot, XSD_DIR);
        checkDirExistAndReadable(xsdRoot);
        providedFiles.put("INFO_XSD_FILE", findXsdFile(xsdRoot, "INFO(DMF)", "info_(mon|per)[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("ALTO_XSD_FILE", findXsdFile(xsdRoot, "ALTO", "alto_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("CMD_XSD_FILE", findXsdFile(xsdRoot, "copyrightMD", "cmd_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("DC_XSD_FILE", findXsdFile(xsdRoot, "Dublin Core", "dc_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("METS_XSD_FILE", findXsdFile(xsdRoot, "METS", "mets_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("MIX_XSD_FILE", findXsdFile(xsdRoot, "MIX", "mix_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("MODS_XSD_FILE", findXsdFile(xsdRoot, "MODS", "mods_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("PREMIS_XSD_FILE", findXsdFile(xsdRoot, "PREMIS", "premis_[0-9]+(\\.([0-9])+)*\\.xsd"));

        //nacteni konfiguracnich souboru - jmennych prostoru, vzdoru, promennych, pravidel
        validateAndRegisterFdmfConfig(fdmfRoot, "namespaces.xml");
        validateAndRegisterFdmfConfig(fdmfRoot, "patterns.xml");
        validateAndRegisterFdmfConfig(fdmfRoot, "variables.xml");
        validateAndRegisterFdmfConfig(fdmfRoot, "rules.xml");

        //inicializace profilu pro validaci metadat
        initBiblioProfiles();
        initTechProfiles();
        initMetsProfiles();
    }

    private void initBiblioProfiles() throws ValidatorConfigurationException {
        File biblioProfilesDir = new File(fdmfRoot, BIBLIO_PROFILES_DIR);
        checkDirExistAndReadable(biblioProfilesDir);
        //dc profiles
        File dcProfilesDir = new File(biblioProfilesDir, BIBLIO_PROFILES_DC_DIR);
        checkDirExistAndReadable(dcProfilesDir);
        File[] dcProfiles = dcProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profile : dcProfiles) {
            //xsd validation of profile
            validateConfigFile(profile, metadataProfileXsd);
            biblioDcProfiles.add(profile);
        }
        //mods profiles
        File modsProfilesDir = new File(biblioProfilesDir, BIBLIO_PROFILES_MODS_DIR);
        checkDirExistAndReadable(modsProfilesDir);
        File[] modsProfiles = modsProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profile : modsProfiles) {
            //xsd validation of profile
            validateConfigFile(profile, metadataProfileXsd);
            biblioModsProfiles.add(profile);
        }
    }

    private void initTechProfiles() throws ValidatorConfigurationException {
        File techProfilesDir = new File(fdmfRoot, TECH_PROFILES_DIR);
        checkDirExistAndReadable(techProfilesDir);
        File[] techProfileFiles = techProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profileFile : techProfileFiles) {
            validateConfigFile(profileFile, metadataProfileXsd);
            techProfiles.add(profileFile);
        }
    }

    private void initMetsProfiles() throws ValidatorConfigurationException {
        File metsProfilesDir = new File(fdmfRoot, METS_PROFILES_DIR);
        checkDirExistAndReadable(metsProfilesDir);
        File[] metProfileFiles = metsProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profileFile : metProfileFiles) {
            validateConfigFile(profileFile, metadataProfileXsd);
            metsProfiles.add(profileFile);
        }
    }

    private void validateAndRegisterFdmfConfig(File fdmfRoot, String fileName) throws ValidatorConfigurationException {
        File configFile = new File(fdmfRoot, fileName);
        checkFileExistAndReadable(configFile);
        //TODO: v produkci povolit validaci konfiguracniho souboru podle xsd
        //validateConfigFile(configFile, fdmfConfigXsd);
        fdmfConfigFiles.add(configFile);
    }

    private void validateConfigFile(File configFile, File xsd) throws ValidatorConfigurationException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(xsd);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new StreamSource(configFile));
        } catch (Exception e) {
            throw new ValidatorConfigurationException("konfigurační soubor %s není validní: %s", configFile.getAbsolutePath(), e.getMessage());
        }
    }

    public void initJ2kProfiles(ImageUtilManager imageUtilManager) throws ValidatorConfigurationException {
        imageValidator = new ImageValidator(imageUtilManager);
        for (ImageCopy copy : ImageCopy.values()) {
            for (ImageUtil util : ImageUtil.values()) {
                if (imageUtilManager.isUtilAvailable(util)) {
                    validateAndProcessJ2kProfileConfig(fdmfRoot, imageValidator, copy, util);
                }
            }
        }
    }

    private void validateAndProcessJ2kProfileConfig(File fdmfRoot, ImageValidator validator, ImageCopy copy, ImageUtil util) throws ValidatorConfigurationException {
        File rootFile = new File(fdmfRoot, J2K_PROFILES_DIR);
        checkDirExistAndReadable(rootFile);
        File copyFile = buildImageCopyDir(rootFile, copy);
        checkDirExistAndReadable(copyFile);
        File configFile = new File(copyFile, util.getProfileFileName());
        checkFileExistAndReadable(configFile);
        //TODO: implementovat xsd, povolit validaci v produkci
        //validateConfigFile(configFile, j2kProfileConfigXsd);
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

    private static File findXsdFile(File xsdDir, String formatName, String filePattern) throws ValidatorConfigurationException {
        File[] files = xsdDir.listFiles((dir, name) -> name.matches(filePattern));
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

    public ImageValidator getImageValidator() {
        if (imageValidator == null) {
            throw new IllegalStateException(ImageValidator.class.getSimpleName() + " not initialized yet");
        }
        return imageValidator;
    }

    public Map<String, File> getProvidedFiles() {
        return providedFiles;
    }

    public List<File> getFdmfConfigFiles() {
        return fdmfConfigFiles;
    }

    public List<File> getBiblioModsProfiles() {
        return biblioModsProfiles;
    }

    public List<File> getBiblioDcProfiles() {
        return biblioDcProfiles;
    }

    public List<File> getTechProfiles() {
        return techProfiles;
    }

    public List<File> getMetsProfiles() {
        return metsProfiles;
    }
}
