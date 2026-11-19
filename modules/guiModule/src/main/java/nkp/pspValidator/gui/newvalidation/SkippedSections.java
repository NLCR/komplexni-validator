package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Přeskakované sekce pravidel pro konkrétní fDMF, uložené ve stejném klíči jako u dialogu přeskakování
 * ({@link ConfigurationManager#propSkippedValidationSections(Dmf)}). Validace přeskočí jen sekce, které fDMF skutečně
 * má (MainController), proto se i tady zobrazují jen ty.
 */
public class SkippedSections {

    private final ConfigurationManager config;
    private final FdmfRegistry registry;

    public SkippedSections(ConfigurationManager config, FdmfRegistry registry) {
        this.config = config;
        this.registry = registry;
    }

    /**
     * Názvy přeskakovaných sekcí v pořadí sekcí fDMF; null, když fDMF neexistuje nebo ji nelze přečíst.
     */
    public List<String> skipped(Dmf dmf) {
        List<RulesSection> sections;
        try {
            sections = registry.getRulesSections(dmf);
        } catch (FdmfRegistry.UnknownFdmfException | ValidatorConfigurationException e) {
            return null;
        }
        Set<String> inConfig = config.getStringSet(ConfigurationManager.propSkippedValidationSections(dmf));
        List<String> result = new ArrayList<>();
        for (RulesSection section : sections) {
            if (inConfig.contains(section.getName())) {
                result.add(section.getName());
            }
        }
        return result;
    }

    /** Sekce se znovu bude validovat. */
    public void unskip(Dmf dmf, String sectionName) {
        String key = ConfigurationManager.propSkippedValidationSections(dmf);
        Set<String> names = new HashSet<>(config.getStringSet(key));
        if (names.remove(sectionName)) {
            config.setStringSet(key, names);
        }
    }

    /** Žádná sekce fDMF se nepřeskočí (stejně jako uložení dialogu přeskakování bez vybraných sekcí). */
    public void unskipAll(Dmf dmf) {
        config.setStringSet(ConfigurationManager.propSkippedValidationSections(dmf), new HashSet<>());
    }
}
