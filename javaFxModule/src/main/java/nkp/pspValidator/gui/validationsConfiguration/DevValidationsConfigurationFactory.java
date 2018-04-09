package nkp.pspValidator.gui.validationsConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class DevValidationsConfigurationFactory {

    private static DevValidationsConfigurationFactory instance = new DevValidationsConfigurationFactory();

    public static DevValidationsConfigurationFactory getInstance() {
        return instance;
    }

    private ValidationsConfiguration data = init();

    public ValidationsConfiguration getTestConfiguration() {
        return createCopy(data);
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

    public void setTestConfiguration(ValidationsConfiguration data) {
        this.data = data;
    }

    private ValidationsConfiguration init() {
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
