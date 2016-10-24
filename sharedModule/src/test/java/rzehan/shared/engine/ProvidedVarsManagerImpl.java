package rzehan.shared.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 24.10.16.
 */
public class ProvidedVarsManagerImpl implements ProvidedVarsManager {

    private final Map<String, File> files = new HashMap<>();
    private final Map<String, String> strings = new HashMap<>();

    @Override
    public File getProvidedFile(String fileId) {
        return files.get(fileId);
    }

    @Override
    public String getProvidedString(String stringId) {
        return strings.get(stringId);
    }


    public void addFile(String fileId, File file) {
        files.put(fileId, file);
    }

    public void addString(String stringId, String value) {
        strings.put(stringId, value);
    }


}
