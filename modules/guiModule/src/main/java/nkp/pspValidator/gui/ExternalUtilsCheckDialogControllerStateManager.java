package nkp.pspValidator.gui;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import java.util.*;

public class ExternalUtilsCheckDialogControllerStateManager {

    private Map<ExternalUtil, ExternalUtilsCheckDialogController.UtilCheckResult> utilsResult = new HashMap<>();

    public synchronized void registerUtil(ExternalUtil util) {
        utilsResult.put(util, null);
    }

    public synchronized DialogState getState() {
        boolean someRegistered = false;
        boolean someError = false;
        boolean someRunning = false;
        for (ExternalUtil util : utilsResult.keySet()) {
            someRegistered = true;
            UtilCheckingState utilCheckingState = toUtilCheckingState(utilsResult.get(util));
            if (utilCheckingState == UtilCheckingState.ERROR) {
                someError = true;
            } else if (utilCheckingState == UtilCheckingState.RUNNING) {
                someRunning = true;
            }
        }
        if (!someRegistered) {
            return DialogState.RUNNING;
        } else if (someRunning) {
            return DialogState.RUNNING;
        } else if (someError) {
            return DialogState.ERROR;
        } else {
            return DialogState.FINISHED;
        }
    }

    private UtilCheckingState toUtilCheckingState(ExternalUtilsCheckDialogController.UtilCheckResult result) {
        if (result == null) {
            return UtilCheckingState.RUNNING;
        } else if (result.isAvailable()) {
            return UtilCheckingState.CHECKED;
        } else {
            return UtilCheckingState.ERROR;
        }
    }

    public synchronized Map<ExternalUtil, ExternalUtilsCheckDialogController.UtilCheckResult> getUtilsResult() {
        return Collections.unmodifiableMap(utilsResult);
    }

    public synchronized void setUtilsResult(ExternalUtil util, ExternalUtilsCheckDialogController.UtilCheckResult result) {
        utilsResult.put(util, result);
    }


    private enum UtilCheckingState {
        RUNNING, CHECKED, ERROR;
    }

    public enum DialogState {
        RUNNING, FINISHED, ERROR;
    }
}
