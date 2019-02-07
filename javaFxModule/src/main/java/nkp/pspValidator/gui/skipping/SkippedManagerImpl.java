package nkp.pspValidator.gui.skipping;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.gui.validation.Utils;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.util.*;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class SkippedManagerImpl implements SkippedManager {

    private final ConfigurationManager configurationManager;
    private final List<Dmf> dmfList;
    private final Map<Dmf, Skipped> dmfToSkipped;

    public SkippedManagerImpl(ConfigurationManager configurationManager, ValidationDataManager mgr) {
        this.configurationManager = configurationManager;
        dmfList = buildDmfList(mgr);
        dmfToSkipped = buildDmfToSkipped(mgr);
    }

    // TODO: 2019-02-07 usporadat od poslednich verzi k starsim
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

    private Map<Dmf, Skipped> buildDmfToSkipped(ValidationDataManager mgr) {
        Map<Dmf, Skipped> result = new HashMap<>();
        //load from engine
        for (Dmf dmf : dmfList) {
            result.put(dmf, buildSkipped(mgr, dmf));
        }
        //disable sections from config
        for (Dmf dmf : result.keySet()) {
            Skipped skipped = result.get(dmf);
            Set<String> skippedSections = configurationManager.getStringSet(configurationManager.propSkippedValidationSections(dmf));
            for (String skippedSectionName : skippedSections) {
                RulesSection section = skipped.getSectionByName(skippedSectionName);
                if (section != null) {
                    section.setEnabled(false);
                }
            }
        }
        return result;
    }

    private Skipped buildSkipped(ValidationDataManager mgr, Dmf dmf) {
        try {
            Skipped result = new Skipped();
            //inicializace fake validatoru pro ziskani pravidel
            Validator validator = Utils.buildValidator(mgr, dmf, null);
            List<RulesSection> sections = validator.getEngine().getRuleSections();
            //pro jistotu hard copy to avoid memory leaks from references to Engine etc
            List<RulesSection> sectionsCopy = new ArrayList<>(sections.size());
            for (RulesSection section : sections) {
                sectionsCopy.add(section.copy());
            }
            result.setAllSections(sectionsCopy);
            return result;
        } catch (FdmfRegistry.UnknownFdmfException e) {
            //should never happen
            throw new RuntimeException(e);
        } catch (ValidatorConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Skipped getSkippedForDmf(Dmf dmf) {
        return dmfToSkipped.get(dmf);
    }

    @Override
    public void setSkippedForDmf(Dmf dmf, Skipped skipped) {
        dmfToSkipped.put(dmf, skipped);
    }

    @Override
    public List<Dmf> getDmfList() {
        return dmfList;
    }

    @Override
    public void save() {
        for (Dmf dmf : dmfList) {
            Skipped skipped = dmfToSkipped.get(dmf);
            Set<String> namesOfDisabledSections = skipped.getNamesOfSkippedSections();
            configurationManager.setStringSet(configurationManager.propSkippedValidationSections(dmf), namesOfDisabledSections);
        }
    }
}
