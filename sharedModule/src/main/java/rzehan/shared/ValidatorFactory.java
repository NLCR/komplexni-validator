package rzehan.shared;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by martin on 2.11.16.
 */
public class ValidatorFactory {

    public static Validator buildValidator(File fdmfRoot, File pspRootDir) throws ValidatorConfigurationException {
        Engine engine = new Engine();
        //root dir
        engine.setProvidedFile("PSP_DIR", pspRootDir);
        //provided xsd
        File xsdRoot = buildXsdDir(fdmfRoot);
        engine.setProvidedFile("INFO_XSD_FILE", findXsdFile(xsdRoot, "DMF-info", "info_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("ALTO_XSD_FILE", findXsdFile(xsdRoot, "ALTO", "alto_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("CMD_XSD_FILE", findXsdFile(xsdRoot, "copyrightMD", "cmd_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("DC_XSD_FILE", findXsdFile(xsdRoot, "Dublin Core", "dc_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("METS_XSD_FILE", findXsdFile(xsdRoot, "METS", "mets_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("MIX_XSD_FILE", findXsdFile(xsdRoot, "MIX", "mix_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("MODS_XSD_FILE", findXsdFile(xsdRoot, "MODS", "mods_[0-9]+(\\.([0-9])+)*\\.xsd"));
        engine.setProvidedFile("PREMIS_XSD_FILE", findXsdFile(xsdRoot, "PREMIS", "premis_[0-9]+(\\.([0-9])+)*\\.xsd"));
        //TODO: jeste namespacy
        //nacteni patternu, promennych, pravidel etc.
        //TODO: kontroly, ze jsou soubory dostupne, daji se cist, etc
        engine.processConfigFile(new File(fdmfRoot, "patterns.xml"));
        engine.processConfigFile(new File(fdmfRoot, "variables.xml"));
        engine.processConfigFile(new File(fdmfRoot, "rules.xml"));
        return new Validator(engine);
    }

    private static File findXsdFile(File xsdDir, String formatName, String filePattern) throws ValidatorConfigurationException {
        File[] files = xsdDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(filePattern);
            }
        });
        if (files.length == 0) {
            throw new ValidatorConfigurationException(String.format("nenalezen soubor XSD pro validaci formátu %s", formatName));
        } else if (files.length > 1) {
            throw new ValidatorConfigurationException(String.format("nalezeno více souborů XSD pro validaci formátu %s a není jasné, který použít", formatName));
        } else {
            File file = files[0];
            if (file.isDirectory()) {
                throw new ValidatorConfigurationException(String.format("soubor %s je adresář", file.getAbsolutePath()));
            } else if (!file.canRead()) {
                throw new ValidatorConfigurationException(String.format("nelze číst obsah souboru %s", file.getAbsolutePath()));
            }
            return file;
        }
    }

    private static File buildXsdDir(File fdmfRoot) throws ValidatorConfigurationException {
        File dir = new File(fdmfRoot, "xsd");
        if (!dir.exists()) {
            throw new ValidatorConfigurationException(String.format("soubor %s neexistuje", dir.getAbsolutePath()));
        } else if (!dir.isDirectory()) {
            throw new ValidatorConfigurationException(String.format("soubor %s není adresář", dir.getAbsolutePath()));
        } else if (!dir.canRead()) {
            throw new ValidatorConfigurationException(String.format("nelze číst obsah adresáře %s", dir.getAbsolutePath()));
        }
        return dir;
    }


}
