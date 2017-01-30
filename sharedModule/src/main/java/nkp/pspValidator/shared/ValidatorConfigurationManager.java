package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;

/**
 * Created by Martin Řehánek on 30.1.17.
 */
public class ValidatorConfigurationManager {

    private final File imageUtilsConfigFile;
    private final File fdmfDir;
    private final File fdmfConfigXsd;
    private final File j2kProfileConfigXsd;

    public ValidatorConfigurationManager(File validatorConfigurationDir) throws ValidatorConfigurationException {
        checkDirExistAndReadable(validatorConfigurationDir);
        fdmfDir = new File(validatorConfigurationDir, "fDMF");
        checkDirExistAndReadable(fdmfDir);
        imageUtilsConfigFile = new File(validatorConfigurationDir, "imageUtils.xml");
        checkFileExistAndReadable(imageUtilsConfigFile);
        File xsdDir = new File(validatorConfigurationDir, "xsd");
        checkDirExistAndReadable(xsdDir);
        fdmfConfigXsd = new File(xsdDir, "fdmfConfig.xsd");
        //fdmfConfigXsd = new File(xsdDir, "info.txt");
        checkFileExistAndReadable(fdmfConfigXsd);
        j2kProfileConfigXsd = new File(xsdDir, "j2kProfile.xsd");
        checkFileExistAndReadable(j2kProfileConfigXsd);
    }

    public File getImageUtilsConfigFile() {
        return imageUtilsConfigFile;
    }

    public File getFdmfDir() {
        return fdmfDir;
    }

    public File getFdmfConfigXsd() {
        return fdmfConfigXsd;
    }

    public File getJ2kProfileConfigXsd() {
        return j2kProfileConfigXsd;
    }

    public void checkDirExistAndReadable(File dir) throws ValidatorConfigurationException {
        if (!dir.exists()) {
            throw new ValidatorConfigurationException(String.format("konfigurační adresář neexistuje: %s", dir.getAbsolutePath()));
        } else if (!dir.isDirectory()) {
            throw new ValidatorConfigurationException(String.format("konfigurační adresář není adresář: %s", dir.getAbsolutePath()));
        } else if (!dir.canRead()) {
            throw new ValidatorConfigurationException(String.format("nelze číst konfigurační adresář: %s", dir.getAbsolutePath()));
        }
    }

    public void checkFileExistAndReadable(File file) throws ValidatorConfigurationException {
        if (!file.exists()) {
            throw new ValidatorConfigurationException(String.format("chybí konfigurační soubor: %s", file.getAbsolutePath()));
        } else if (file.isDirectory()) {
            throw new ValidatorConfigurationException(String.format("konfigurační soubor je adresář: %s", file.getAbsolutePath()));
        } else if (!file.canRead()) {
            throw new ValidatorConfigurationException(String.format("nelze číst konfigurační soubor: %s", file.getAbsolutePath()));
        }
    }

}
