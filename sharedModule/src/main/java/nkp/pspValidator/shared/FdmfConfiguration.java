package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ResourceType;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileValidator;

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
    private static final String DIR_BINARY_FILE_PROFILES = "binaryFileProfiles";
    private static final String DIR_BFP_SOURCE_IMAGE_USER_COPY = "uc";
    private static final String DIR_BFP_SOURCE_IMAGE_MASTER_COPY = "mc";
    private static final String DIR_BFP_SOURCE_AUDIO_SOURCE = "sa";
    private static final String DIR_BFP_SOURCE_AUDIO_MASTER_COPY = "mca";
    private static final String DIR_BFP_SOURCE_AUDIO_USER_COPY = "uca";

    private static final String DIR_BIBLIO_PROFILES_DIR = "biblioProfiles";
    private static final String DIR_BP_TYPE_DC = "dc";
    private static final String DIR_BP_TYPE_MODS = "mods";

    private static final String DIR_TECH_PROFILES = "techProfiles";
    private static final String DIR_METS_PROFILES = "metsProfiles";

    private final File fdmfRoot;
    private final File fdmfConfigXsd;
    private final File j2kProfileXsd;
    private final File metadataProfileXsd;

    private final Map<String, File> providedFiles = new HashMap<>();
    private final List<File> fdmfConfigFiles = new ArrayList<>();
    private final List<File> biblioModsProfiles = new ArrayList<>();
    private final List<File> biblioDcProfiles = new ArrayList<>();
    private final List<File> techProfiles = new ArrayList<>();
    private List<File> metsProfiles = new ArrayList<>();

    private BinaryFileValidator binaryFileValidator;


    public FdmfConfiguration(File fdmfRoot, File fdmfConfigXsd, File j2kProfileXsd, File metadataProfileXsd) throws ValidatorConfigurationException {
        this.fdmfRoot = fdmfRoot;
        this.fdmfConfigXsd = fdmfConfigXsd;
        this.j2kProfileXsd = j2kProfileXsd;
        this.metadataProfileXsd = metadataProfileXsd;
        init();
    }

    private void init() throws ValidatorConfigurationException {
        checkDirExistAndReadable(fdmfRoot);

        //nacteni xsd pro metadata
        File xsdRoot = new File(fdmfRoot, XSD_DIR);
        checkDirExistAndReadable(xsdRoot);
        providedFiles.put("INFO_XSD_FILE", findXsdFile(xsdRoot, "INFO(DMF)", "info_(mon|per|adg)[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("ALTO_XSD_FILE", findXsdFile(xsdRoot, "ALTO", "alto_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("CMD_XSD_FILE", findXsdFile(xsdRoot, "copyrightMD", "cmd_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("DC_XSD_FILE", findXsdFile(xsdRoot, "Dublin Core", "dc_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("METS_XSD_FILE", findXsdFile(xsdRoot, "METS", "mets_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("MIX_XSD_FILE", findXsdFile(xsdRoot, "MIX", "mix_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("MODS_XSD_FILE", findXsdFile(xsdRoot, "MODS", "mods_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("PREMIS_XSD_FILE", findXsdFile(xsdRoot, "PREMIS", "premis_[0-9]+(\\.([0-9])+)*\\.xsd"));
        providedFiles.put("AES57_XSD_FILE", findXsdFile(xsdRoot, "AES57", "aes57_[0-9]+(\\.([0-9])+)*\\.xsd"));

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
        File biblioProfilesDir = new File(fdmfRoot, DIR_BIBLIO_PROFILES_DIR);
        checkDirExistAndReadable(biblioProfilesDir);
        //dc profiles
        File dcProfilesDir = new File(biblioProfilesDir, DIR_BP_TYPE_DC);
        checkDirExistAndReadable(dcProfilesDir);
        File[] dcProfiles = dcProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profile : dcProfiles) {
            //xsd validation of profile
            validateConfigFile(profile, metadataProfileXsd);
            biblioDcProfiles.add(profile);
        }
        //mods profiles
        File modsProfilesDir = new File(biblioProfilesDir, DIR_BP_TYPE_MODS);
        checkDirExistAndReadable(modsProfilesDir);
        File[] modsProfiles = modsProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profile : modsProfiles) {
            //xsd validation of profile
            validateConfigFile(profile, metadataProfileXsd);
            biblioModsProfiles.add(profile);
        }
    }

    private void initTechProfiles() throws ValidatorConfigurationException {
        File techProfilesDir = new File(fdmfRoot, DIR_TECH_PROFILES);
        checkDirExistAndReadable(techProfilesDir);
        File[] techProfileFiles = techProfilesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        for (File profileFile : techProfileFiles) {
            validateConfigFile(profileFile, metadataProfileXsd);
            techProfiles.add(profileFile);
        }
    }

    private void initMetsProfiles() throws ValidatorConfigurationException {
        File metsProfilesDir = new File(fdmfRoot, DIR_METS_PROFILES);
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
        validateConfigFile(configFile, fdmfConfigXsd);
        fdmfConfigFiles.add(configFile);
    }

    private void validateConfigFile(File configFile, File xsd) throws ValidatorConfigurationException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(xsd);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new StreamSource(configFile));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidatorConfigurationException("konfigurační soubor %s není validní: %s", configFile.getAbsolutePath(), e.getMessage());
        }
    }

    public void initBinaryFileProfiles(ExternalUtilManager externalUtilManager) throws ValidatorConfigurationException {
        binaryFileValidator = new BinaryFileValidator(externalUtilManager);
        for (ResourceType type : ResourceType.values()) {
            for (ExternalUtil util : ExternalUtil.values()) {
                if (externalUtilManager.isUtilAvailable(util)) {
                    validateAndProcessBinaryFileProfile(fdmfRoot, binaryFileValidator, type, util);
                }
            }
        }
    }

    private void validateAndProcessBinaryFileProfile(File fdmfRoot, BinaryFileValidator validator, ResourceType type, ExternalUtil util) throws ValidatorConfigurationException {
        File rootFile = new File(fdmfRoot, DIR_BINARY_FILE_PROFILES);
        checkDirExistAndReadable(rootFile);
        File sourceTypeDir = buildSourceTypeDir(rootFile, type);
        if (sourceTypeDir.exists()) {
            checkDirExistAndReadable(sourceTypeDir);
            File profileDefinitionFile = new File(sourceTypeDir, util.getProfileFileName());
            if (profileDefinitionFile.exists()) {
                checkFileExistAndReadable(profileDefinitionFile);
                validateConfigFile(profileDefinitionFile, j2kProfileXsd);
                validator.registerProfile(type, util, profileDefinitionFile);
            }
        }
    }

    private static File buildSourceTypeDir(File rootFile, ResourceType type) {
        switch (type) {
            case IMAGE_USER_COPY:
                return new File(rootFile, DIR_BFP_SOURCE_IMAGE_USER_COPY);
            case IMAGE_MASTER_COPY:
                return new File(rootFile, DIR_BFP_SOURCE_IMAGE_MASTER_COPY);
            case AUDIO_SOURCE:
                return new File(rootFile, DIR_BFP_SOURCE_AUDIO_SOURCE);
            case AUDIO_MASTER_COPY:
                return new File(rootFile, DIR_BFP_SOURCE_AUDIO_MASTER_COPY);
            case AUDIO_USER_COPY:
                return new File(rootFile, DIR_BFP_SOURCE_AUDIO_USER_COPY);
            default:
                throw new IllegalStateException();
        }
    }

    private static File findXsdFile(File xsdDir, String formatName, String filePattern) throws ValidatorConfigurationException {
        File[] files = xsdDir.listFiles((dir, name) -> name.matches(filePattern));
        if (files.length == 0) {
            //throw new ValidatorConfigurationException(String.format("nenalezen soubor XSD pro validaci formátu %s", formatName));
            //System.err.println(String.format("nenalezen soubor XSD pro validaci formátu %s", formatName));
            return null;
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

    public BinaryFileValidator getBinaryFileValidator() {
        if (binaryFileValidator == null) {
            throw new IllegalStateException(BinaryFileValidator.class.getSimpleName() + " not initialized yet");
        }
        return binaryFileValidator;
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
