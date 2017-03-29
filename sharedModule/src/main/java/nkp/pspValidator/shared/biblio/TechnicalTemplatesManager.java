package nkp.pspValidator.shared.biblio;

import nkp.pspValidator.shared.biblio.biblioValidator.BiblioTemplate;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin Řehánek on 31.1.17.
 */
public class TechnicalTemplatesManager {

    private final Map<String, File> data = new HashMap<>();
    private final BiblioTemplatesParser parser;

    public TechnicalTemplatesManager(BiblioTemplatesParser parser) {
        this.parser = parser;
    }

    public void processFile(File file) throws ValidatorConfigurationException {
        //System.err.println("processing: " + file.getName() + ", " + format);
        //just test-parsing so that we reveal potential errors before actually processing it
        parser.parseTemplate(file);
        String filenameWithoutSuffix = file.getName().substring(0, file.getName().length() - ".xml".length());
        data.put(filenameWithoutSuffix, file);
    }

    public BiblioTemplate buildTemplate(String profileId) {
        File file = data.get(profileId);
        if (file == null) {
            return null;
        } else {
            try {
                return parser.parseTemplate(file);
            } catch (ValidatorConfigurationException e) {
                //should never happen, template has already been parsed
                throw new IllegalStateException(e);
            }
        }
    }

}
