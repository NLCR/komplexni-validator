package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 9.12.16.
 */
public class FileUtils {

    //TODO: mozna jinou vyjimku, anebo prejmenovat
    //TODO: nahradit duplikovane tyhle metody jinak

    public static void checkDirExistAndReadable(File dir) throws ValidatorConfigurationException {
        if (!dir.exists()) {
            throw new ValidatorConfigurationException(String.format("adresář neexistuje: %s", dir.getAbsolutePath()));
        } else if (!dir.isDirectory()) {
            throw new ValidatorConfigurationException(String.format("soubor není adresář: %s", dir.getAbsolutePath()));
        } else if (!dir.canRead()) {
            throw new ValidatorConfigurationException(String.format("nelze číst adresář: %s", dir.getAbsolutePath()));
        }
    }

    public static void checkFileExistAndReadable(File file) throws ValidatorConfigurationException {
        if (!file.exists()) {
            throw new ValidatorConfigurationException(String.format("chybí konfigurační soubor: %s", file.getAbsolutePath()));
        } else if (file.isDirectory()) {
            throw new ValidatorConfigurationException(String.format("konfigurační soubor je adresář: %s", file.getAbsolutePath()));
        } else if (!file.canRead()) {
            throw new ValidatorConfigurationException(String.format("nelze číst konfigurační soubor: %s", file.getAbsolutePath()));
        }
    }

    public static String toLimitedPath(File file, int parentsNum) {
        //put parents in list
        List<String> parents = new ArrayList<>();
        File current = file;
        while (parentsNum > 0) {
            if (current != null) {
                File parent = current.getParentFile();
                if (parent != null) {
                    parents.add(parent.getName());
                    current = parent;
                    parentsNum--;
                }
            } else {
                break;
            }
        }
        //build path with inverted parent list
        StringBuilder builder = new StringBuilder();
        for (int i = parents.size() - 1; i >= 0; i--) {
            builder.append(parents.get(i));
            builder.append(File.separatorChar);
        }
        //append file name
        builder.append(file.getName());
        return builder.toString();
    }

}
