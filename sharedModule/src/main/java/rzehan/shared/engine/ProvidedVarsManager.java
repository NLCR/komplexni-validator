package rzehan.shared.engine;

import java.io.File;

/**
 * Created by martin on 24.10.16.
 */
public interface ProvidedVarsManager {

    public File getProvidedFile(String fileId);

    public String getProvidedString(String stringId);

}
