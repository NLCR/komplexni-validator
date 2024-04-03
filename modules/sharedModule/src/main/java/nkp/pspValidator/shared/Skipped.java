package nkp.pspValidator.shared;

import java.util.*;

public class Skipped {

    private final Map<Dmf, Set<String>> sections = new HashMap<>();

    public void setSections(Dmf dmf, Collection<String> sectionsOfDmf) {
        Set<String> toBeStored = new HashSet<>();
        toBeStored.addAll(sectionsOfDmf);
        sections.put(dmf, toBeStored);
    }

    public Set<String> getSections(Dmf dmf) {
        Set<String> result = sections.get(dmf);
        return result == null ? Collections.emptySet() : result;
    }

}
