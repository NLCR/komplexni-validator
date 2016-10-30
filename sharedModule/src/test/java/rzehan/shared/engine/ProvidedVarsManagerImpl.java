package rzehan.shared.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 24.10.16.
 */
public class ProvidedVarsManagerImpl implements Engine.ProvidedVarsManager {

    private final Map<String, File> files = new HashMap<>();
    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, Integer> integers = new HashMap<>();

    @Override
    public File getProvidedFile(String fileId) {
        return files.get(fileId);
    }

    @Override
    public String getProvidedString(String stringId) {
        return strings.get(stringId);
    }

    @Override
    public Integer getProvidedInteger(String intId) {
        return integers.get(intId);
    }

    public void addFile(String fileId, File file) {
        files.put(fileId, file);
    }

    public void addString(String stringId, String value) {
        strings.put(stringId, value);
    }

    public void addInteger(String intId, Integer value) {
        integers.put(intId, value);
    }


}
