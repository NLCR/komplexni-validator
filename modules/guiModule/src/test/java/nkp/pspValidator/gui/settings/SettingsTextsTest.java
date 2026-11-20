package nkp.pspValidator.gui.settings;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/** Texty okna Nastavení (očekávané hodnoty napsané ručně podle české mluvnice). */
public class SettingsTextsTest {

    @Test
    public void unsavedNote() {
        assertNull(SettingsTexts.unsavedNote(null));
        assertNull(SettingsTexts.unsavedNote(Collections.<SettingsTexts.Part>emptyList()));
        assertEquals("Neuložené změny: výchozí verze DMF.",
                SettingsTexts.unsavedNote(Collections.singletonList(SettingsTexts.Part.VERSIONS)));
        assertEquals("Neuložené změny: výchozí verze DMF, přeskakovaná pravidla, adresáře a protokoly.",
                SettingsTexts.unsavedNote(Arrays.asList(SettingsTexts.Part.VERSIONS, SettingsTexts.Part.SKIPPING, SettingsTexts.Part.DIRS)));
        assertEquals("Neuložené změny: cesty k externím nástrojům.",
                SettingsTexts.unsavedNote(Collections.singletonList(SettingsTexts.Part.TOOLS)));
    }

    /** Části v patičce jsou v pořadí záložek Nastavení (nástroje za verzemi, před přeskakováním). */
    @Test
    public void partsInTabOrder() {
        assertEquals(Arrays.asList(SettingsTexts.Part.VERSIONS, SettingsTexts.Part.TOOLS, SettingsTexts.Part.SKIPPING,
                SettingsTexts.Part.DIRS), Arrays.asList(SettingsTexts.Part.values()));
    }

    @Test
    public void skippingVersionLabel() {
        assertEquals("2.3", SettingsTexts.skippingVersionLabel("2.3", 0));
        assertEquals("2.3", SettingsTexts.skippingVersionLabel("2.3", -1));
        assertEquals("2.3 (přeskakuje se 1)", SettingsTexts.skippingVersionLabel("2.3", 1));
        assertEquals("2.3 (přeskakují se 2)", SettingsTexts.skippingVersionLabel("2.3", 2));
        assertEquals("2.3 (přeskakují se 4)", SettingsTexts.skippingVersionLabel("2.3", 4));
        assertEquals("2.3 (přeskakuje se 5)", SettingsTexts.skippingVersionLabel("2.3", 5));
    }

    @Test
    public void skippingSummary() {
        assertEquals("Tato verze nemá žádné sekce.", SettingsTexts.skippingSummary(0, 0));
        assertEquals("Validují se všechny sekce (14).", SettingsTexts.skippingSummary(0, 14));
        assertEquals("Přeskakuje se 1 z 1 sekce.", SettingsTexts.skippingSummary(1, 1));
        assertEquals("Přeskakuje se 1 z 5 sekcí.", SettingsTexts.skippingSummary(1, 5));
        assertEquals("Přeskakují se 2 ze 14 sekcí.", SettingsTexts.skippingSummary(2, 14));
        assertEquals("Přeskakují se 4 z 12 sekcí.", SettingsTexts.skippingSummary(4, 12));
        assertEquals("Přeskakuje se 5 ze 7 sekcí.", SettingsTexts.skippingSummary(5, 7));
        assertEquals("Přeskakuje se 14 ze 14 sekcí.", SettingsTexts.skippingSummary(14, 14));
    }

    @Test
    public void fromPreposition() {
        //ze pred s, s, z, z a ct: ze dvou, tri, ctyr, sesti, sedmi, ctrnacti, sestnacti, sedmnacti, ctyriceti, sedesati, sedmdesati, sta
        String[] ze = {"2", "3", "4", "6", "7", "14", "16", "17", "40", "49", "60", "69", "70", "79", "100", "199", "200", "499", "600", "799"};
        String[] z = {"0", "1", "5", "8", "9", "10", "11", "12", "13", "15", "18", "19", "20", "39", "50", "59", "80", "99", "500", "599", "800", "999", "1000", "-1"};
        for (String n : ze) {
            assertEquals(n, "ze", SettingsTexts.fromPreposition(Integer.parseInt(n)));
        }
        for (String n : z) {
            assertEquals(n, "z", SettingsTexts.fromPreposition(Integer.parseInt(n)));
        }
    }

    @Test
    public void dictionaryTitle() {
        assertEquals("Kódy institucí (Sigla)", SettingsTexts.dictionaryTitle("kódy institucí (Sigla)", "siglaInstitutionCodes"));
        assertEquals("Čísla", SettingsTexts.dictionaryTitle("  čísla ", "x"));
        assertEquals("Kódy", SettingsTexts.dictionaryTitle("Kódy", "x"));
        assertEquals("marcRelatorCodes", SettingsTexts.dictionaryTitle(null, "marcRelatorCodes"));
        assertEquals("marcRelatorCodes", SettingsTexts.dictionaryTitle("  ", "marcRelatorCodes"));
    }

    @Test
    public void dictionaryValues() {
        assertEquals("0 hodnot", SettingsTexts.dictionaryValues(0));
        assertEquals("1 hodnota", SettingsTexts.dictionaryValues(1));
        assertEquals("2 hodnoty", SettingsTexts.dictionaryValues(2));
        assertEquals("4 hodnoty", SettingsTexts.dictionaryValues(4));
        assertEquals("5 hodnot", SettingsTexts.dictionaryValues(5));
        assertEquals("8024 hodnot", SettingsTexts.dictionaryValues(8024));
    }

    @Test
    public void dictionaryLastSync() {
        assertNull(SettingsTexts.dictionaryLastSync(null, "07. 01. 2019"));
        assertNull(SettingsTexts.dictionaryLastSync(" ", null));
        assertEquals("zatím neaktualizováno", SettingsTexts.dictionaryLastSync("https://x/y.dict", null));
        assertEquals("zatím neaktualizováno", SettingsTexts.dictionaryLastSync("https://x/y.dict", ""));
        assertEquals("aktualizováno 07. 01. 2019", SettingsTexts.dictionaryLastSync("https://x/y.dict", "07. 01. 2019"));
    }

    @Test
    public void dictionaryLinkLabel() {
        assertEquals("Seznam na GitHubu", SettingsTexts.dictionaryLinkLabel(
                "https://github.com/NLCR/komplexni-validator/blob/master/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/dictionaries/siglaInstitutionCodes.dict"));
        assertEquals("Seznam na GitHubu", SettingsTexts.dictionaryLinkLabel(" HTTPS://GitHub.com/NLCR/x "));
        assertEquals("Specifikace", SettingsTexts.dictionaryLinkLabel("https://www.loc.gov/marc/relators/relaterm.html"));
        //adresa, ktera github.com jen obsahuje, neni seznam na GitHubu
        assertEquals("Specifikace", SettingsTexts.dictionaryLinkLabel("https://example.org/github.com/"));
        assertEquals("Specifikace", SettingsTexts.dictionaryLinkLabel(null));
    }

    @Test
    public void validatorConfigNote() {
        assertNull(SettingsTexts.validatorConfigNote(DirsDraft.ValidatorConfigState.IN_USE, "/kv/validatorConfig"));
        assertEquals("Použije se po restartu aplikace. Do restartu se používá: /kv/validatorConfig",
                SettingsTexts.validatorConfigNote(DirsDraft.ValidatorConfigState.UNSAVED, "/kv/validatorConfig"));
        assertEquals("Uloženo, použije se po restartu aplikace. Do restartu se používá: C:\\Program Files\\KV\\app\\validatorConfig",
                SettingsTexts.validatorConfigNote(DirsDraft.ValidatorConfigState.SAVED_PENDING_RESTART, "C:\\Program Files\\KV\\app\\validatorConfig"));
        assertEquals("Tento adresář aplikace právě používá; po uložení se bude používat i po restartu.",
                SettingsTexts.validatorConfigNote(DirsDraft.ValidatorConfigState.UNSAVED_BACK_TO_RUNNING, "/kv/validatorConfig"));
    }

    @Test
    public void validatorConfigDirError() {
        assertEquals("Adresář /Users/uživatel/Stažené nelze použít. Adresář je prázdný.",
                SettingsTexts.validatorConfigDirError("/Users/uživatel/Stažené", "Adresář je prázdný."));
        assertEquals("Ověřuji konfiguraci ve vybraném adresáři…", SettingsTexts.VALIDATOR_CONFIG_CHECKING);
    }
}
