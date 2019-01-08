package nkp.pspValidator.shared.metadataProfile;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 26.1.17.
 */
public class DictionaryManager {

    public static final String DICT_FILE_SUFFIX = ".dict";

    private final File dictionaryDir;
    private final Map<String, Set<String>> dictionaries = new HashMap<>();

    public DictionaryManager(File dictionaryDir) throws ValidatorConfigurationException {
        this.dictionaryDir = dictionaryDir;
        init();
    }

    private void init() throws ValidatorConfigurationException {
        dictionaries.clear();
        if (!dictionaryDir.exists()) {
            throw new ValidatorConfigurationException("adresář %s neexistuje", dictionaryDir.getAbsolutePath());
        } else if (!dictionaryDir.isDirectory()) {
            throw new ValidatorConfigurationException("soubor %s není adresář", dictionaryDir.getAbsolutePath());
        } else if (!dictionaryDir.canRead()) {
            throw new ValidatorConfigurationException("nelze číst adresář %s", dictionaryDir.getAbsolutePath());
        } else {
            File[] files = dictionaryDir.listFiles((dir, name) -> name.endsWith(DICT_FILE_SUFFIX));
            for (File file : files) {
                String filename = file.getName();
                String dictName = filename.substring(0, filename.length() - DICT_FILE_SUFFIX.length());
                dictionaries.put(dictName, parseDictionaryFile(file));
            }
        }
    }

    private Set<String> parseDictionaryFile(File file) throws ValidatorConfigurationException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            Set<String> result = new HashSet<>();
            while (in.ready()) {
                String line = in.readLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    result.add(line);
                }
            }
            return result;
        } catch (IOException e) {
            throw new ValidatorConfigurationException("chyba v zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Set<String> getDictionaries() {
        return dictionaries.keySet();
    }

    public boolean hasDictionary(String dictionaryName) {
        return dictionaries.containsKey(dictionaryName);
    }

    public Set<String> getDictionaryValues(String dictionaryName) {
        return dictionaries.get(dictionaryName);
    }

    public File getDictionaryFile(String dictionaryName) {
        if (!hasDictionary(dictionaryName)) {
            return null;
        } else {
            return new File(dictionaryDir, dictionaryName + DICT_FILE_SUFFIX);
        }
    }

    public void reload() throws ValidatorConfigurationException {
        init();
    }
}
