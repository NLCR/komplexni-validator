package nkp.pspValidator.gui.skipping;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.gui.VersionComparator;
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

    private List<Dmf> buildDmfList(ValidationDataManager mgr) {
        List<Dmf> result = new ArrayList<>();
        //monograph
        List<String> monVersions = new ArrayList<>();
        monVersions.addAll(mgr.getFdmfRegistry().getMonographFdmfVersions());
        Collections.sort(monVersions, new VersionComparator());
        for (String monVersion : monVersions) {
            result.add(new Dmf(Dmf.Type.MONOGRAPH, monVersion));
        }
        //periodical
        List<String> perVersions = new ArrayList<>();
        perVersions.addAll(mgr.getFdmfRegistry().getPeriodicalFdmfVersions());
        Collections.sort(perVersions, new VersionComparator());
        for (String perVersion : perVersions) {
            result.add(new Dmf(Dmf.Type.PERIODICAL, perVersion));
        }
        //audio gramophone
        List<String> adgVersions = new ArrayList<>();
        adgVersions.addAll(mgr.getFdmfRegistry().getAudioGramFdmfVersions());
        Collections.sort(adgVersions, new VersionComparator());
        for (String adgVersion : adgVersions) {
            result.add(new Dmf(Dmf.Type.AUDIO_GRAM, adgVersion));
        }
        //audio fonographic cylinder
        List<String> adfVersions = new ArrayList<>();
        adfVersions.addAll(mgr.getFdmfRegistry().getAudioFonoFdmfVersions());
        Collections.sort(adfVersions, new VersionComparator());
        for (String adfVersion : adfVersions) {
            result.add(new Dmf(Dmf.Type.AUDIO_FONO, adfVersion));
        }
        //audio disc
        List<String> adiVersions = new ArrayList<>();
        adiVersions.addAll(mgr.getFdmfRegistry().getAudioDiscFdmfVersions());
        Collections.sort(adiVersions, new VersionComparator());
        for (String adiVersion : adiVersions) {
            result.add(new Dmf(Dmf.Type.AUDIO_DISC, adiVersion));
        }        
        //audio no carrier
        List<String> adnVersions = new ArrayList<>();
        adnVersions.addAll(mgr.getFdmfRegistry().getAudioDiscFdmfVersions());
        Collections.sort(adnVersions, new VersionComparator());
        for (String adnVersion : adnVersions) {
            result.add(new Dmf(Dmf.Type.AUDIO_NO_CARRIER, adnVersion));
        }        
        //data disc
        List<String> daDVersions = new ArrayList<>();
        daDVersions.addAll(mgr.getFdmfRegistry().getDataDiscFdmfVersions());
        Collections.sort(daDVersions, new VersionComparator());
        for (String dadVersion : daDVersions) {
            result.add(new Dmf(Dmf.Type.DATA_DISC, dadVersion));
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
