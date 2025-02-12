package nkp.pspValidator.shared.metadataProfile.biblio;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.types.MetadataFormat;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static nkp.pspValidator.shared.engine.types.MetadataFormat.DC;
import static nkp.pspValidator.shared.engine.types.MetadataFormat.MODS;

/**
 * Created by Martin Řehánek on 31.1.17.
 */
public class BibliographicMetadataProfilesManager {

    private final Map<MetadataFormat, Map<CatalogingConventions, Map<String, File>>> data = new HashMap<>();
    private final MetadataProfileParser parser;

    public BibliographicMetadataProfilesManager(MetadataProfileParser parser) {
        this.parser = parser;
        data.put(DC, new HashMap<>());
        data.put(MODS, new HashMap<>());
    }

    public void registerProfileFile(File profileFile, MetadataFormat format) throws ValidatorConfigurationException {
        //System.err.println("BibliographicMetadataProfilesManager: registering File: " + profileFile.getAbsoluteFile() + ", " + format);
        //just test-parsing so that we reveal potential errors before actually processing it
        parser.parseProfile(profileFile);
        Map<CatalogingConventions, Map<String, File>> conventionsMapMap = data.get(format);
        String filename = profileFile.getName();
        String filenameWithoutSuffix = profileFile.getName().substring(0, filename.length() - ".xml".length());
        CatalogingConventions conventions = detectCatalogingConventions(filenameWithoutSuffix, profileFile);
        Map<String, File> filenameFileMap = conventionsMapMap.get(conventions);
        if (filenameFileMap == null) {
            filenameFileMap = new HashMap<>();
        }
        conventionsMapMap.put(conventions, filenameFileMap);
        String fileId = buildFileId(filenameWithoutSuffix, conventions);
        filenameFileMap.put(fileId, profileFile);
    }

    private CatalogingConventions detectCatalogingConventions(String filenameWithoutSuffix, File file) throws ValidatorConfigurationException {
        if (filenameWithoutSuffix.endsWith("_aacr2")) {
            return CatalogingConventions.AACR2;
        } else if (filenameWithoutSuffix.endsWith("_rda")) {
            return CatalogingConventions.RDA;
        } else {
            throw new ValidatorConfigurationException("neznámá katalogizační pravidlo souboru %s", file.getAbsolutePath());
        }
    }

    private String buildFileId(String filenameWithoutSuffix, CatalogingConventions conventions) throws ValidatorConfigurationException {
        switch (conventions) {
            case AACR2:
                return filenameWithoutSuffix.substring(0, filenameWithoutSuffix.length() - "_aacr2".length());
            case RDA:
                return filenameWithoutSuffix.substring(0, filenameWithoutSuffix.length() - "_rda".length());
            default:
                throw new ValidatorConfigurationException(filenameWithoutSuffix);
        }
    }

    public MetadataProfile buildProfile(String fileId, MetadataFormat format, CatalogingConventions conventions) {
        File file = getProfileFile(fileId, format, conventions);
        if (file == null) {
            System.err.println("file is null");
            return null;
        } else {
            try {
                return parser.parseProfile(file);
            } catch (ValidatorConfigurationException e) {
                //should never happen, profile has already been parsed
                throw new IllegalStateException(e);
            }
        }
    }

    private File getProfileFile(String fileId, MetadataFormat format, CatalogingConventions conventions) {
        Map<CatalogingConventions, Map<String, File>> conventionsMapMap = data.get(format);
        if (conventionsMapMap == null) {
            return null;
        } else {
            Map<String, File> stringFileMap = conventionsMapMap.get(conventions);
            if (stringFileMap == null) {
                return null;
            } else {
                return stringFileMap.get(fileId);
            }
        }
    }

}
