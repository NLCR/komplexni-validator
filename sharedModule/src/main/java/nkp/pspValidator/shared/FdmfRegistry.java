package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class FdmfRegistry {

    private final Map<String, FdmfConfiguration> monographFdmfByVersion = new HashMap<>();
    private final Map<String, FdmfConfiguration> periodicalFdmfByVersion = new HashMap<>();
    private final Map<String, FdmfConfiguration> soundRecordingFdmfByVersion = new HashMap<>();

    public FdmfRegistry(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        init(validatorConfigManager);
    }

    public void initBinaryFileProfiles(ExternalUtilManager externalUtilManager) throws ValidatorConfigurationException {
        for (FdmfConfiguration fdmfConfig : monographFdmfByVersion.values()) {
            fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        }
        for (FdmfConfiguration fdmfConfig : periodicalFdmfByVersion.values()) {
            fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        }
        for (FdmfConfiguration fdmfConfig : soundRecordingFdmfByVersion.values()) {
            fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        }
    }

    private void init(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        loadFdmfConfigs(validatorConfigManager, "monograph", monographFdmfByVersion);
        loadFdmfConfigs(validatorConfigManager, "periodical", periodicalFdmfByVersion);
        loadFdmfConfigs(validatorConfigManager, "sound_recording", soundRecordingFdmfByVersion);

    }

    private void loadFdmfConfigs(ValidatorConfigurationManager validatorConfigManager, String fdmfDirPefix, Map<String, FdmfConfiguration> mapToStoreResults) throws ValidatorConfigurationException {
        File[] fdmfDirs = validatorConfigManager.getFdmfDir().listFiles((dir, name) -> name.matches(fdmfDirPefix + "_[0-9]+(\\.([0-9])+)*"));
        /*List<File> fdmfDirsSorted = Arrays.asList(fdmfDirs).stream().sorted((first, second) -> {
            String firstVersion = first.getName().split("_")[1];
            String secondVersion = second.getName().split("_")[1];
            String[] firstTokens = firstVersion.split("\\.");
            String[] secondTokens = secondVersion.split("\\.");
            System.out.println(firstVersion + ", " + secondVersion);
            //for (int i = 0; (i < (firstTokens.length - 1)) || (i < (secondTokens.length - 1)); i++) {
            for (int i = 0; true; i++) {
                //System.out.println("i: " + i);
                String firstNumStr = firstTokens.length == i ? null : firstTokens[i];
                String secondNumStr = secondTokens.length == i ? null : secondTokens[i];
                //System.out.println("first: " + firstNumStr + ", second: " + secondNumStr);
                if (firstNumStr == null && secondNumStr == null) {
                    int result = 1;
                    //System.out.println(" zadny rozdil: " + result);
                    //System.out.println();
                    return result;
                } else if (firstNumStr == null && secondNumStr != null) {
                    int result = 1;
                    //System.out.println("prvni null: " + result);
                    //System.out.println(result < 0 ? firstVersion + " < " + secondVersion : secondVersion + " < " + firstVersion);
                    //System.out.println();
                    return result;
                    //return 1;
                } else if (secondNumStr == null && firstNumStr != null) {
                    int result = -1;
                    //System.out.println("druhy null: " + result);
                    //System.out.println(result < 0 ? firstVersion + " < " + secondVersion : secondVersion + " < " + firstVersion);
                    //System.out.println();
                    return result;
                    //return -1;
                } else {
                    //int result = Integer.valueOf(firstNumStr) - Integer.valueOf(secondNumStr);
                    int result = Integer.valueOf(secondNumStr) - Integer.valueOf(firstNumStr);
                    if (result != 0) {
                        //System.out.println("rozdil: " + ": " + result);
                        //System.out.println(result < 0 ? firstVersion + " < " + secondVersion : secondVersion + " < " + firstVersion);
                        //System.out.println();
                        return result;
                    }
                    //return Integer.valueOf(secondNumStr) - Integer.valueOf(firstNumStr);
                    //return Integer.valueOf(firstNumStr) - Integer.valueOf(secondNumStr);
                    //return result;
                }
            }
        }).collect(Collectors.toList());*/
        for (File fdmfDir : fdmfDirs) {
            String versionNumber = fdmfDir.getName().substring(fdmfDirPefix.length() + 1);
            System.out.println(fdmfDir.getName());
            mapToStoreResults.put(versionNumber, new FdmfConfiguration(
                    fdmfDir,
                    validatorConfigManager.getFdmfConfigXsd(),
                    validatorConfigManager.getBinaryFileProfileXsd(),
                    validatorConfigManager.getMetadataProfileXsd()));
        }
    }

    public Set<String> getMonographFdmfVersions() {
        return monographFdmfByVersion.keySet();
    }

    public Set<String> getPeriodicalFdmfVersions() {
        return periodicalFdmfByVersion.keySet();
    }

    public Set<String> getSoundRecordingFdmfVersions() {
        return soundRecordingFdmfByVersion.keySet();
    }

    public FdmfConfiguration getMonographFdmfConfig(String dmfVersion) {
        return monographFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getPeriodicalFdmfConfig(String dmfVersion) {
        return periodicalFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getSoundRecordingFdmfConfig(String dmfVersion) {
        return soundRecordingFdmfByVersion.get(dmfVersion);
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
            case SOUND_RECORDING: {
                FdmfConfiguration file = soundRecordingFdmfByVersion.get(dmf.getVersion());
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
