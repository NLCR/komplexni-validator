package nkp.pspValidator.gui.validationsConfiguration;

import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.shared.Dmf;

import java.util.List;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public interface ConfigurationManager {
    ValidationsConfiguration getConfiguration(Dmf dmf);

    void setConfiguration(Dmf dmf, ValidationsConfiguration config);

    List<Dmf> getDmfList();
}
