package nkp.pspValidator.gui.exclusions.data;

import java.util.List;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ExclusionsConfiguration {

    private List<ExcludedSection> excludedSections;

    public List<ExcludedSection> getExcludedSections() {
        return excludedSections;
    }

    public void setExcludedSections(List<ExcludedSection> excludedSections) {
        this.excludedSections = excludedSections;
    }


}
