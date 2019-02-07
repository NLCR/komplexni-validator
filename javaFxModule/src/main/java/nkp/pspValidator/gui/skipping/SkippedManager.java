package nkp.pspValidator.gui.skipping;

import nkp.pspValidator.shared.Dmf;

import java.util.List;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public interface SkippedManager {

    void setSkippedForDmf(Dmf dmf, Skipped skipped);

    Skipped getSkippedForDmf(Dmf dmf);

    List<Dmf> getDmfList();

    void save();
}
