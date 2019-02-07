package nkp.pspValidator.gui.skipping;

import nkp.pspValidator.shared.engine.RulesSection;

import java.util.*;

/**
 * Created by Martin Řehánek
 */
public class Skipped {

    private List<RulesSection> allSections;

    private transient Map<String, RulesSection> sectionNameToSectionMap;

    public List<RulesSection> getAllSections() {
        return allSections;
    }

    public void setAllSections(List<RulesSection> allSections) {
        this.allSections = allSections;
        this.sectionNameToSectionMap = initSectionNameToSectionMap();
    }

    private Map<String, RulesSection> initSectionNameToSectionMap() {
        Map<String, RulesSection> result = new HashMap<>();
        if (allSections != null) {
            for (RulesSection section : allSections) {
                result.put(section.getName(), section);
            }
        }
        return result;
    }

    public RulesSection getSectionByName(String name) {
        return sectionNameToSectionMap == null ? null : sectionNameToSectionMap.get(name);
    }

    public Set<String> getNamesOfSkippedSections() {
        Set<String> result = new HashSet<>();
        for (RulesSection section : allSections) {
            if (!section.isEnabled()) {
                result.add(section.getName());
            }
        }
        return result;
    }

}
