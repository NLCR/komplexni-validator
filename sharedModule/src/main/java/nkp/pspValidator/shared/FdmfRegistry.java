package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class FdmfRegistry {

    private final Map<String, FdmfConfiguration> monographFdmfByVersion = new HashMap<>();
    private final Map<String, FdmfConfiguration> periodicalFdmfByVersion = new HashMap<>();

    public FdmfRegistry(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        init(validatorConfigManager);
    }

    public void initJ2kProfiles(ImageUtilManager imageUtilManager) throws ValidatorConfigurationException {
        for (FdmfConfiguration fdmfConfig : monographFdmfByVersion.values()) {
            fdmfConfig.initJ2kProfiles(imageUtilManager);
        }
        for (FdmfConfiguration fdmfConfig : periodicalFdmfByVersion.values()) {
            fdmfConfig.initJ2kProfiles(imageUtilManager);
        }
    }

    private void init(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        loadMonographFdmfConfigs(validatorConfigManager);
        loadPeriodicalFdmfConfigs(validatorConfigManager);
    }

    private void loadMonographFdmfConfigs(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        File[] fdmfDirs = validatorConfigManager.getFdmfDir().listFiles((dir, name) -> name.matches("monograph_[0-9]+(\\.([0-9])+)*"));
        for (File fdmfDir : fdmfDirs) {
            String version = fdmfDir.getName().split("_")[1];
            monographFdmfByVersion.put(version, new FdmfConfiguration(fdmfDir, validatorConfigManager.getFdmfConfigXsd(), validatorConfigManager.getJ2kProfileConfigXsd()));
        }
    }

    private void loadPeriodicalFdmfConfigs(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        File[] fdmfDirs = validatorConfigManager.getFdmfDir().listFiles((dir, name) -> name.matches("periodical_[0-9]+(\\.([0-9])+)*"));
        for (File fdmfDir : fdmfDirs) {
            String version = fdmfDir.getName().split("_")[1];
            periodicalFdmfByVersion.put(version, new FdmfConfiguration(fdmfDir, validatorConfigManager.getFdmfConfigXsd(), validatorConfigManager.getJ2kProfileConfigXsd()));
        }
    }

    public Set<String> getMonographFdmfVersions() {
        return monographFdmfByVersion.keySet();
    }

    public Set<String> getPeriodicalFdmfVersions() {
        return periodicalFdmfByVersion.keySet();
    }

    public FdmfConfiguration getMonographFdmfConfig(String dmfVersion) {
        return monographFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getPeriodicalFdmfConfig(String dmfVersion) {
        return periodicalFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getFdmfConfig(Dmf dmf) throws UnknownFdmfException {
        switch (dmf.getType()) {
            case MONOGRAPH: {
                FdmfConfiguration file = monographFdmfByVersion.get(dmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfException(dmf);
                } else {
                    return file;
                }
            }
            case PERIODICAL: {
                FdmfConfiguration file = periodicalFdmfByVersion.get(dmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfException(dmf);
                } else {
                    return file;
                }
            }
            default:
                throw new IllegalStateException();
        }
    }


    public static class UnknownFdmfException extends Exception {

        public UnknownFdmfException(Dmf dmf) {
            super(String.format("Není definována fDMF (formalizovaná DMF) pro: %s", dmf));
        }

    }

}
