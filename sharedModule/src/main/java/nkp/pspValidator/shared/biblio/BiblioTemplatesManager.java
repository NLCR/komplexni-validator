package nkp.pspValidator.shared.biblio;

import nkp.pspValidator.shared.biblio.biblioValidator.BiblioTemplate;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.types.MetadataFormat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static nkp.pspValidator.shared.engine.types.MetadataFormat.DC;
import static nkp.pspValidator.shared.engine.types.MetadataFormat.MODS;

/**
 * Created by Martin Řehánek on 31.1.17.
 */
public class BiblioTemplatesManager {

    private final Map<MetadataFormat, Map<CatalogingRules, Map<String, File>>> data = new HashMap<>();
    private final BiblioTemplatesParser parser;

    public BiblioTemplatesManager(BiblioTemplatesParser parser) {
        this.parser = parser;
        data.put(DC, new HashMap<>());
        data.put(MODS, new HashMap<>());
    }

    public void processFile(File file, MetadataFormat format) throws ValidatorConfigurationException {
        //just test-parsing so that we reveal potential errors before actually processing it
        parser.parseTemplate(file);
        Map<CatalogingRules, Map<String, File>> catalogingRulesMapMap = data.get(format);
        String filename = file.getName();
        String filenameWithoutSuffix = file.getName().substring(0, filename.length() - ".xml".length());
        CatalogingRules rules = detectCatalogingRules(filenameWithoutSuffix, file);
        Map<String, File> rulesMap = catalogingRulesMapMap.get(rules);
        if (rulesMap == null) {
            rulesMap = new HashMap<>();
        }
        catalogingRulesMapMap.put(rules, rulesMap);
        String fileId = buildFileId(filenameWithoutSuffix, rules);
        rulesMap.put(fileId, file);
    }

    private CatalogingRules detectCatalogingRules(String filenameWithoutSuffix, File file) throws ValidatorConfigurationException {
        if (filenameWithoutSuffix.endsWith("_aacr2")) {
            return CatalogingRules.AACR2;
        } else if (filenameWithoutSuffix.endsWith("_rda")) {
            return CatalogingRules.RDA;
        } else {
            throw new ValidatorConfigurationException("neznámá katalogizační pravidlo souboru %s", file.getAbsolutePath());
        }
    }

    private String buildFileId(String filenameWithoutSuffix, CatalogingRules rules) throws ValidatorConfigurationException {
        switch (rules) {
            case AACR2:
                return filenameWithoutSuffix.substring(0, filenameWithoutSuffix.length() - "_aacr2".length());
            case RDA:
                return filenameWithoutSuffix.substring(0, filenameWithoutSuffix.length() - "_rda".length());
            default:
                throw new ValidatorConfigurationException(filenameWithoutSuffix);
        }
    }

    public static enum CatalogingRules {
        AACR2, RDA;
    }

    public BiblioTemplate buildTemplate(String fileId, MetadataFormat format, CatalogingRules rules) {
        File file = getTemplateFile(fileId, format, rules);
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

    private File getTemplateFile(String fileId, MetadataFormat format, CatalogingRules rules) {
        Map<CatalogingRules, Map<String, File>> catalogingRulesMapMap = data.get(format);
        if (catalogingRulesMapMap == null) {
            return null;
        } else {
            Map<String, File> stringFileMap = catalogingRulesMapMap.get(rules);
            if (stringFileMap == null) {
                return null;
            } else {
                return stringFileMap.get(fileId);
            }
        }
    }


}
