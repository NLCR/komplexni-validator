package nkp.pspValidator.gui.exclusions.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ExclusionsConfiguration implements Serializable {

    private List<ExcludedSection> excludedSections;

    private transient Map<String, ExcludedSection> map;

    public List<ExcludedSection> getExcludedSections() {
        return excludedSections;
    }

    public void setExcludedSections(List<ExcludedSection> excludedSections) {
        this.excludedSections = excludedSections;
        this.map = initMap();
    }

    private Map<String, ExcludedSection> initMap() {
        Map<String, ExcludedSection> result = new HashMap<>();
        if (excludedSections != null) {
            for (ExcludedSection section : excludedSections) {
                result.put(section.getName(), section);
            }
        }
        return result;
    }

    public ExcludedSection getSectionByName(String name) {
        return map == null ? null : map.get(name);
    }

}
