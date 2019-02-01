package nkp.pspValidator.shared.metadataProfile.tech;

import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin Řehánek on 31.1.17.
 */
public class TechnicalMetadataProfilesManager {

    private final Map<String, File> data = new HashMap<>();
    private final MetadataProfileParser parser;

    public TechnicalMetadataProfilesManager(MetadataProfileParser parser) {
        this.parser = parser;
    }

    public void processFile(File file) throws ValidatorConfigurationException {
        //System.err.println("processing: " + file.getName() + ", " + format);
        //just test-parsing so that we reveal potential errors before actually processing it
        parser.parseProfile(file);
        String filenameWithoutSuffix = file.getName().substring(0, file.getName().length() - ".xml".length());
        data.put(filenameWithoutSuffix, file);
    }

    public MetadataProfile buildProfile(String profileId) {
        File file = data.get(profileId);
        if (file == null) {
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

}
