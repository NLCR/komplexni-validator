package nkp.pspValidator.gui.exclusions;

import nkp.pspValidator.gui.exclusions.data.ExcludedSection;
import nkp.pspValidator.gui.exclusions.data.ExclusionsConfiguration;
import nkp.pspValidator.shared.Dmf;

import java.util.*;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class MockExclusionsManager implements ExclusionsManager {

    private static ExclusionsManager instance = new MockExclusionsManager();

    public static ExclusionsManager getInstance() {
        return instance;
    }

    private Map<Dmf, ExclusionsConfiguration> data = new HashMap<>();

    @Override
    public ExclusionsConfiguration getConfiguration(Dmf dmf) {
        if (data.get(dmf) == null) {
            data.put(dmf, buildMockConfiguration());
        }
        return createCopy(data.get(dmf));
    }

    @Override
    public void setConfiguration(Dmf dmf, ExclusionsConfiguration config) {
        this.data.put(dmf, createCopy(config));
    }

    @Override
    public List<Dmf> getDmfList() {
        List<Dmf> result = new ArrayList<>();
        result.add(new Dmf(Dmf.Type.MONOGRAPH, "1.0"));
        result.add(new Dmf(Dmf.Type.MONOGRAPH, "1.2"));
        result.add(new Dmf(Dmf.Type.PERIODICAL, "1.4"));
        result.add(new Dmf(Dmf.Type.PERIODICAL, "1.6"));
        return result;
    }

    private ExclusionsConfiguration createCopy(ExclusionsConfiguration originalConfig) {
        ExclusionsConfiguration newConfig = new ExclusionsConfiguration();
        List<ExcludedSection> newExcludedSection = new ArrayList<>(originalConfig.getExcludedSections().size());
        for (ExcludedSection originalExcludedSection : originalConfig.getExcludedSections()) {
            newExcludedSection.add(new ExcludedSection(originalExcludedSection));
        }
        newConfig.setExcludedSections(newExcludedSection);
        return newConfig;
    }

    private ExclusionsConfiguration buildMockConfiguration() {
        Random random = new Random();
        ExclusionsConfiguration configuration = new ExclusionsConfiguration();
        List<ExcludedSection> excludedSectionList = new ArrayList<>();
        configuration.setExcludedSections(excludedSectionList);
        addSection(excludedSectionList, randomState(random), "Struktura souborů", "Struktura PSP balicku na urovni soboru");
        addSection(excludedSectionList, randomState(random), "Soubor CHECKSUM", "Kontrola souboru CHECKSUM, odkazů na další soubory a shody kontrolních součtů.");
        addSection(excludedSectionList, randomState(random), "Soubor INFO", "Validace souboru INFO - odkazování na ostatní soubory, kontrolní součet pro soubor CHECKSUM, atd.");
        addSection(excludedSectionList, randomState(random), "Identifikátory", null);
        addSection(excludedSectionList, randomState(random), "METS hlavičky", "Kontrola hlaviček primárního a sekundárních METS záznamů");
        addSection(excludedSectionList, randomState(random), "Primární METS filesec", null);
        addSection(excludedSectionList, randomState(random), "Sekundární METS filesec", "Sekce mets:fileSec v sekundárních METS souborech musí obsahovat korektní odkazy na soubory s popisy jednotlivých stran (master a user kopie, textové a alto ocr)");
        addSection(excludedSectionList, randomState(random), "Strukturální mapy", "Kontrola fyzických a logických strukturálních map v METS záznamech");

        addSection(excludedSectionList, randomState(random), "Struktura souborů", "Struktura PSP balicku na urovni soboru");
        addSection(excludedSectionList, randomState(random), "Soubor CHECKSUM", "Kontrola souboru CHECKSUM, odkazů na další soubory a shody kontrolních součtů.");
        addSection(excludedSectionList, randomState(random), "Soubor INFO", "Validace souboru INFO - odkazování na ostatní soubory, kontrolní součet pro soubor CHECKSUM, atd.");
        addSection(excludedSectionList, randomState(random), "Identifikátory", null);
        addSection(excludedSectionList, randomState(random), "METS hlavičky", "Kontrola hlaviček primárního a sekundárních METS záznamů");
        addSection(excludedSectionList, randomState(random), "Primární METS filesec", null);
        addSection(excludedSectionList, randomState(random), "Sekundární METS filesec", "Sekce mets:fileSec v sekundárních METS souborech musí obsahovat korektní odkazy na soubory s popisy jednotlivých stran (master a user kopie, textové a alto ocr)");
        addSection(excludedSectionList, randomState(random), "Strukturální mapy", "Kontrola fyzických a logických strukturálních map v METS záznamech");

        addSection(excludedSectionList, randomState(random), "Struktura souborů", "Struktura PSP balicku na urovni soboru");
        addSection(excludedSectionList, randomState(random), "Soubor CHECKSUM", "Kontrola souboru CHECKSUM, odkazů na další soubory a shody kontrolních součtů.");
        addSection(excludedSectionList, randomState(random), "Soubor INFO", "Validace souboru INFO - odkazování na ostatní soubory, kontrolní součet pro soubor CHECKSUM, atd.");
        addSection(excludedSectionList, randomState(random), "Identifikátory", null);
        addSection(excludedSectionList, randomState(random), "METS hlavičky", "Kontrola hlaviček primárního a sekundárních METS záznamů");
        addSection(excludedSectionList, randomState(random), "Primární METS filesec", null);
        addSection(excludedSectionList, randomState(random), "Sekundární METS filesec", "Sekce mets:fileSec v sekundárních METS souborech musí obsahovat korektní odkazy na soubory s popisy jednotlivých stran (master a user kopie, textové a alto ocr)");
        addSection(excludedSectionList, randomState(random), "Strukturální mapy", "Kontrola fyzických a logických strukturálních map v METS záznamech");

        return configuration;
    }

    private static void addSection(List<ExcludedSection> excludedSectionList, ExcludedSection.State state, String name, String description) {
        ExcludedSection excludedSection = new ExcludedSection();
        excludedSection.setName(name);
        excludedSection.setDescription(description);
        excludedSection.setState(state);
        excludedSectionList.add(excludedSection);
    }

    private static ExcludedSection.State randomState(Random random) {
        return ExcludedSection.State.values()[random.nextInt(ExcludedSection.State.values().length)];
    }

}
