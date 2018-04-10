package nkp.pspValidator.gui.exclusions;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.gui.exclusions.data.ExcludedSection;
import nkp.pspValidator.gui.exclusions.data.ExclusionsConfiguration;
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
public class ExclusionsManagerImpl implements ExclusionsManager {

    private final ConfigurationManager configurationManager;
    private final List<Dmf> dmfList;
    private final Map<Dmf, ExclusionsConfiguration> configs;

    public ExclusionsManagerImpl(ConfigurationManager configurationManager, ValidationDataManager mgr) {
        this.configurationManager = configurationManager;
        dmfList = buildDmfList(mgr);
        configs = loadConfigs(mgr);
        save();
    }

    private Map<Dmf, ExclusionsConfiguration> loadConfigs(ValidationDataManager mgr) {
        Map<Dmf, ExclusionsConfiguration> mapFromDmfs = loadFromDmfs(mgr);
        Map<Dmf, ExclusionsConfiguration> mapFromConfig = configurationManager.getExclusionsConfigurations();
        //merging
        Map<Dmf, ExclusionsConfiguration> mapMerged = new HashMap<>();
        for (Dmf dmf : mapFromDmfs.keySet()) {
            //System.out.println("merging for " + dmf);
            ExclusionsConfiguration merged = merge(mapFromDmfs.get(dmf), mapFromConfig.get(dmf));
            if (merged != null) {
                mapMerged.put(dmf, merged);
            }
        }
        return mapMerged;
    }

    private ExclusionsConfiguration merge(ExclusionsConfiguration fromDmfs, ExclusionsConfiguration fromConfig) {
        if (fromDmfs == null) {
            return null;
        } else if (fromConfig == null) {
            return fromDmfs;
        } else {
            ExclusionsConfiguration result = new ExclusionsConfiguration();
            result.setExcludedSections(new ArrayList<>());
            for (ExcludedSection section : fromDmfs.getExcludedSections()) {
                ExcludedSection newSection = new ExcludedSection(section);
                ExcludedSection sectionFromConfig = fromConfig.getSectionByName(section.getName());
                if (sectionFromConfig != null) {
                    System.out.println(sectionFromConfig);
                    newSection.setState(sectionFromConfig.getState());
                }
                result.getExcludedSections().add(newSection);
                //System.out.println(newSection);
            }
            return result;
        }
    }

    private Map<Dmf, ExclusionsConfiguration> loadFromDmfs(ValidationDataManager mgr) {
        Map<Dmf, ExclusionsConfiguration> result = new HashMap<>();
        for (Dmf dmf : dmfList) {
            result.put(dmf, buildConfiguration(mgr, dmf));
        }
        return result;
    }

    private ExclusionsConfiguration buildConfiguration(ValidationDataManager mgr, Dmf dmf) {
        try {
            ExclusionsConfiguration result = new ExclusionsConfiguration();
            //inicializace fake validatoru pro ziskani pravidel
            Validator validator = Utils.buildValidator(mgr, dmf, null);
            List<RulesSection> ruleSections = validator.getEngine().getRuleSections();
            List<ExcludedSection> excludedSections = new ArrayList<>(ruleSections.size());
            for (RulesSection section : ruleSections) {
                excludedSections.add(new ExcludedSection(section));
            }
            result.setExcludedSections(excludedSections);
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
    public ExclusionsConfiguration getConfiguration(Dmf dmf) {
        return configs.get(dmf);
    }

    @Override
    public void setConfiguration(Dmf dmf, ExclusionsConfiguration config) {
        configs.put(dmf, config);
    }

    @Override
    public List<Dmf> getDmfList() {
        return dmfList;
    }

    @Override
    public void save() {
        configurationManager.updateExclusions(configs);
    }
}
