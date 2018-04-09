package nkp.pspValidator.gui.validationsConfiguration;

import nkp.pspValidator.shared.Dmf;

import java.util.*;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class MockConfigurationManager implements ConfigurationManager {

    private static ConfigurationManager instance = new MockConfigurationManager();

    public static ConfigurationManager getInstance() {
        return instance;
    }

    private Map<Dmf, ValidationsConfiguration> data = new HashMap<>();

    @Override
    public ValidationsConfiguration getConfiguration(Dmf dmf) {
        if (data.get(dmf) == null) {
            data.put(dmf, buildMockConfiguration());
        }
        return createCopy(data.get(dmf));
    }

    @Override
    public void setConfiguration(Dmf dmf, ValidationsConfiguration config) {
        this.data.put(dmf, createCopy(config));
    }

    private ValidationsConfiguration createCopy(ValidationsConfiguration originalConfig) {
        ValidationsConfiguration newConfig = new ValidationsConfiguration();
        List<Section> newSection = new ArrayList<>(originalConfig.getSections().size());
        for (Section originalSection : originalConfig.getSections()) {
            newSection.add(new Section(originalSection));
        }
        newConfig.setSections(newSection);
        return newConfig;
    }

    private ValidationsConfiguration buildMockConfiguration() {
        Random random = new Random();
        ValidationsConfiguration configuration = new ValidationsConfiguration();
        List<Section> sectionList = new ArrayList<>();
        configuration.setSections(sectionList);
        addSection(sectionList, randomState(random), "Struktura souborů", "Struktura PSP balicku na urovni soboru");
        addSection(sectionList, randomState(random), "Soubor CHECKSUM", "Kontrola souboru CHECKSUM, odkazů na další soubory a shody kontrolních součtů.");
        addSection(sectionList, randomState(random), "Soubor INFO", "Validace souboru INFO - odkazování na ostatní soubory, kontrolní součet pro soubor CHECKSUM, atd.");
        addSection(sectionList, randomState(random), "Identifikátory", null);
        addSection(sectionList, randomState(random), "METS headers", "Kontrola hlaviček primárního a sekundárních METS záznamů");
        addSection(sectionList, randomState(random), "Primary METS filesec", null);
        addSection(sectionList, randomState(random), "Secondary METS filesec", "Sekce mets:fileSec v sekundárních METS souborech musí obsahovat korektní odkazy na soubory s popisy jednotlivých stran (master a user kopie, textové a alto ocr)");
        addSection(sectionList, randomState(random), "Strukturální mapy", "Kontrola fyzických a logických strukturálních map v METS záznamech");

        addSection(sectionList, randomState(random), "Struktura souborů", "Struktura PSP balicku na urovni soboru");
        addSection(sectionList, randomState(random), "Soubor CHECKSUM", "Kontrola souboru CHECKSUM, odkazů na další soubory a shody kontrolních součtů.");
        addSection(sectionList, randomState(random), "Soubor INFO", "Validace souboru INFO - odkazování na ostatní soubory, kontrolní součet pro soubor CHECKSUM, atd.");
        addSection(sectionList, randomState(random), "Identifikátory", null);
        addSection(sectionList, randomState(random), "METS headers", "Kontrola hlaviček primárního a sekundárních METS záznamů");
        addSection(sectionList, randomState(random), "Primary METS filesec", null);
        addSection(sectionList, randomState(random), "Secondary METS filesec", "Sekce mets:fileSec v sekundárních METS souborech musí obsahovat korektní odkazy na soubory s popisy jednotlivých stran (master a user kopie, textové a alto ocr)");
        addSection(sectionList, randomState(random), "Strukturální mapy", "Kontrola fyzických a logických strukturálních map v METS záznamech");

        addSection(sectionList, randomState(random), "Struktura souborů", "Struktura PSP balicku na urovni soboru");
        addSection(sectionList, randomState(random), "Soubor CHECKSUM", "Kontrola souboru CHECKSUM, odkazů na další soubory a shody kontrolních součtů.");
        addSection(sectionList, randomState(random), "Soubor INFO", "Validace souboru INFO - odkazování na ostatní soubory, kontrolní součet pro soubor CHECKSUM, atd.");
        addSection(sectionList, randomState(random), "Identifikátory", null);
        addSection(sectionList, randomState(random), "METS headers", "Kontrola hlaviček primárního a sekundárních METS záznamů");
        addSection(sectionList, randomState(random), "Primary METS filesec", null);
        addSection(sectionList, randomState(random), "Secondary METS filesec", "Sekce mets:fileSec v sekundárních METS souborech musí obsahovat korektní odkazy na soubory s popisy jednotlivých stran (master a user kopie, textové a alto ocr)");
        addSection(sectionList, randomState(random), "Strukturální mapy", "Kontrola fyzických a logických strukturálních map v METS záznamech");

        return configuration;
    }

    private static void addSection(List<Section> sectionList, Section.State state, String name, String description) {
        Section section = new Section();
        section.setName(name);
        section.setDescription(description);
        section.setState(state);
        sectionList.add(section);
    }

    private static Section.State randomState(Random random) {
        return Section.State.values()[random.nextInt(Section.State.values().length)];
    }

}
