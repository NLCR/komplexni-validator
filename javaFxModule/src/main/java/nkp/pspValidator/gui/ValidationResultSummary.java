package nkp.pspValidator.gui;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.Level;

import java.io.File;
import java.util.Map;

/**
 * Created by Martin Řehánek on 19.12.16.
 */
public class ValidationResultSummary {

    private File pspDir;
    private Dmf dmf;
    private int globalProblemsTotal;
    private Map<Level, Integer> globalProblemsByLevel;
    private boolean valid;
    private long totalTime;

    public File getPspDir() {
        return pspDir;
    }

    public void setPspDir(File pspDir) {
        this.pspDir = pspDir;
    }

    public Dmf getDmf() {
        return dmf;
    }

    public void setDmf(Dmf dmf) {
        this.dmf = dmf;
    }

    public int getGlobalProblemsTotal() {
        return globalProblemsTotal;
    }

    public void setGlobalProblemsTotal(int globalProblemsTotal) {
        this.globalProblemsTotal = globalProblemsTotal;
    }

    public Map<Level, Integer> getGlobalProblemsByLevel() {
        return globalProblemsByLevel;
    }

    public void setGlobalProblemsByLevel(Map<Level, Integer> globalProblemsByLevel) {
        this.globalProblemsByLevel = globalProblemsByLevel;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
}
