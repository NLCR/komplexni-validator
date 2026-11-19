package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.VersionComparator;
import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.skipping.Skipped;
import nkp.pspValidator.gui.skipping.SkippedManager;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.RulesSection;

import java.util.*;

/**
 * Záložka „Přeskakovaná pravidla“ v Nastavení: přeskakované sekce pro každou fDMF nad {@link SkippedManager} (stejná
 * logika a stejné uložení jako dřívější dialog Přeskakování validací). Změny se drží v sekcích manažeru a uloží až
 * {@link #save()} (tlačítko Uložit); uloží se pak všechny fDMF najednou, stejně jako dřív tlačítkem Uložit v dialogu
 * (u každé fDMF se zapíší jen názvy sekcí, které fDMF má).
 */
public class SkippingDraft {

    private final SkippedManager manager;
    private final Map<Dmf, Set<String>> initial = new HashMap<>();

    public SkippingDraft(SkippedManager manager) {
        this.manager = manager;
        for (Dmf dmf : manager.getDmfList()) {
            initial.put(dmf, new HashSet<>(manager.getSkippedForDmf(dmf).getNamesOfSkippedSections()));
        }
    }

    /** Typy dokumentů, pro které existuje aspoň jedna fDMF, v pořadí skupin tabulky verzí. */
    public List<Dmf.Type> types() {
        Set<Dmf.Type> present = EnumSet.noneOf(Dmf.Type.class);
        for (Dmf dmf : manager.getDmfList()) {
            present.add(dmf.getType());
        }
        List<Dmf.Type> result = new ArrayList<>();
        for (DocumentTypes.Group group : DocumentTypes.Group.values()) {
            for (Dmf.Type type : DocumentTypes.typesOf(group)) {
                if (present.contains(type)) {
                    result.add(type);
                }
            }
        }
        return result;
    }

    /** Verze fDMF typu od nejnovější. */
    public List<String> versions(Dmf.Type type) {
        List<String> result = new ArrayList<>();
        for (Dmf dmf : manager.getDmfList()) {
            if (dmf.getType() == type) {
                result.add(dmf.getVersion());
            }
        }
        result.sort(new VersionComparator(true));
        return result;
    }

    /**
     * fDMF, která se v záložce ukáže po otevření: požadovaná, pokud existuje, jinak první fDMF s přeskočenými sekcemi
     * (v pořadí {@link #types()} a verzí od nejnovější), jinak nejnovější verze prvního typu; null bez fDMF.
     */
    public Dmf initialDmf(Dmf preferred) {
        if (preferred != null && initial.containsKey(preferred)) {
            return preferred;
        }
        Dmf first = null;
        for (Dmf.Type type : types()) {
            for (String version : versions(type)) {
                Dmf dmf = new Dmf(type, version);
                if (first == null) {
                    first = dmf;
                }
                if (skippedCount(dmf) > 0) {
                    return dmf;
                }
            }
        }
        return first;
    }

    /** Sekce fDMF v pořadí z konfigurace (prázdný seznam pro neznámou fDMF). */
    public List<RulesSection> sections(Dmf dmf) {
        Skipped skipped = manager.getSkippedForDmf(dmf);
        return skipped == null || skipped.getAllSections() == null ? Collections.<RulesSection>emptyList() : skipped.getAllSections();
    }

    public int skippedCount(Dmf dmf) {
        int count = 0;
        for (RulesSection section : sections(dmf)) {
            if (!section.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public boolean isSkipped(RulesSection section) {
        return !section.isEnabled();
    }

    public void setSkipped(RulesSection section, boolean skip) {
        section.setEnabled(!skip);
    }

    /** Nepřeskakovat nic u dané fDMF. */
    public void unskipAll(Dmf dmf) {
        for (RulesSection section : sections(dmf)) {
            section.setEnabled(true);
        }
    }

    public boolean isChanged() {
        for (Dmf dmf : manager.getDmfList()) {
            if (!manager.getSkippedForDmf(dmf).getNamesOfSkippedSections().equals(initial.get(dmf))) {
                return true;
            }
        }
        return false;
    }

    public void save() {
        manager.save();
    }
}
