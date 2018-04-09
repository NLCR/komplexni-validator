package nkp.pspValidator.gui.exclusions;

import nkp.pspValidator.gui.exclusions.data.ExclusionsConfiguration;
import nkp.pspValidator.shared.Dmf;

import java.util.List;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public interface ExclusionsManager {
    ExclusionsConfiguration getConfiguration(Dmf dmf);

    void setConfiguration(Dmf dmf, ExclusionsConfiguration config);

    List<Dmf> getDmfList();
}
