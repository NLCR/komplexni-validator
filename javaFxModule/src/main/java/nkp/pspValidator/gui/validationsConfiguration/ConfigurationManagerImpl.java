package nkp.pspValidator.gui.validationsConfiguration;

import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.gui.validation.Utils;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ConfigurationManagerImpl implements ConfigurationManager {

    private final List<Dmf> dmfList;
    private final Map<Dmf, ValidationsConfiguration> configurationMap = new HashMap<>();

    public ConfigurationManagerImpl(ValidationDataManager mgr) {
        dmfList = buildDmfList(mgr);
        for (Dmf dmf : dmfList) {
            configurationMap.put(dmf, buildConfiguration(mgr, dmf));
        }
        //potom zmerguju s vlastni perzistentni konfiguraci pravidel
    }

    private ValidationsConfiguration buildConfiguration(ValidationDataManager mgr, Dmf dmf) {
        try {
            ValidationsConfiguration result = new ValidationsConfiguration();
            //inicializace fake validatoru pro ziskani pravidel
            Validator validator = Utils.buildValidator(mgr, dmf, null);
            List<RulesSection> ruleSections = validator.getEngine().getRuleSections();
            List<Section> sections = new ArrayList<>(ruleSections.size());
            for (RulesSection section : ruleSections) {
                sections.add(new Section(section));
            }
            result.setSections(sections);
            return result;
        } catch (FdmfRegistry.UnknownFdmfException e) {
            //should never happen
            throw new RuntimeException(e);
        } catch (ValidatorConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Dmf> buildDmfList(ValidationDataManager mgr) {
        List<Dmf> result = new ArrayList<>();
        for (String monVersion : mgr.getFdmfRegistry().getMonographFdmfVersions()) {
            result.add(new Dmf(Dmf.Type.MONOGRAPH, monVersion));
        }
        for (String perVersion : mgr.getFdmfRegistry().getPeriodicalFdmfVersions()) {
            result.add(new Dmf(Dmf.Type.PERIODICAL, perVersion));
        }
        return result;
    }

    @Override
    public ValidationsConfiguration getConfiguration(Dmf dmf) {
        return configurationMap.get(dmf);
    }

    @Override
    public void setConfiguration(Dmf dmf, ValidationsConfiguration config) {
        // TODO: 10.4.18 nejak perzistentne uchovavat v souboru. A pozdeji tahat a mergovat s konfiguraci z enginu
    }

    @Override
    public List<Dmf> getDmfList() {
        return dmfList;
    }
}
