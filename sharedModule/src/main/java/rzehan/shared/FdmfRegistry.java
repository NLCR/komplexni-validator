package rzehan.shared;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by martin on 2.11.16.
 */
public class FdmfRegistry {

    private final Map<String, File> monographFdmfByVersion = new HashMap<>();
    private final Map<String, File> periodicalFdmfByVersion = new HashMap<>();

    public FdmfRegistry(File fdmfsRootDir) {
        init(fdmfsRootDir);
    }

    private void init(File fdmfsRootDir) {
        loadMonographFdmfs(fdmfsRootDir);
        loadPeriodicalFdmfs(fdmfsRootDir);
    }

    private void loadMonographFdmfs(File fdmfsRootDir) {
        File[] fdmfDirs = fdmfsRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("monograph_[0-9]+(\\.([0-9])+)*");
            }
        });
        for (File fdmfDir : fdmfDirs) {
            String version = fdmfDir.getName().split("_")[1];
            monographFdmfByVersion.put(version, fdmfDir);
        }
    }

    private void loadPeriodicalFdmfs(File fdmfsRootDir) {
        File[] fdmfDirs = fdmfsRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("periodical_[0-9]+(\\.([0-9])+)*");
            }
        });
        for (File fdmfDir : fdmfDirs) {
            String version = fdmfDir.getName().split("_")[1];
            periodicalFdmfByVersion.put(version, fdmfDir);
        }
    }

    public Set<String> getMonographFdmfVersions() {
        return monographFdmfByVersion.keySet();
    }

    public Set<String> getPeriodicalFdmfVersions() {
        return periodicalFdmfByVersion.keySet();
    }

    public File getMonographFdmfDir(String dmfVersion) {
        return monographFdmfByVersion.get(dmfVersion);
    }

    public File getPeriodicalFdmfDir(String dmfVersion) {
        return periodicalFdmfByVersion.get(dmfVersion);
    }

    public File getFdmfDir(Fdmf fdmf) throws UnknownFdmfVersionException {
        switch (fdmf.getType()) {
            case MONOGRAPH: {
                File file = monographFdmfByVersion.get(fdmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfVersionException(fdmf);
                } else {
                    return file;
                }
            }
            case PERIODICAL: {
                File file = periodicalFdmfByVersion.get(fdmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfVersionException(fdmf);
                } else {
                    return file;
                }
            }
            default:
                throw new IllegalStateException();
        }
    }


    public static class UnknownFdmfVersionException extends Exception {

        public UnknownFdmfVersionException(Fdmf fdmf) {
            super(String.format("neznámá verze pro DMF %s: %s", fdmf.getType(), fdmf.getVersion()));
        }


    }

}
