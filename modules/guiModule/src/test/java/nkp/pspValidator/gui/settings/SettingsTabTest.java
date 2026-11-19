package nkp.pspValidator.gui.settings;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/** Záložky okna Nastavení: pořadí a názvy podle výkresu Nastaveni, výběr podle klíče (snímky). */
public class SettingsTabTest {

    @Test
    public void orderAndLabelsAsInDrawing() {
        assertEquals(Arrays.asList(SettingsTab.VERSIONS, SettingsTab.TOOLS, SettingsTab.SKIPPING, SettingsTab.DICTIONARIES, SettingsTab.DIRS),
                Arrays.asList(SettingsTab.values()));
        assertEquals("Výchozí verze DMF", SettingsTab.VERSIONS.getLabel());
        assertEquals("Externí nástroje", SettingsTab.TOOLS.getLabel());
        assertEquals("Přeskakovaná pravidla", SettingsTab.SKIPPING.getLabel());
        assertEquals("Slovníky", SettingsTab.DICTIONARIES.getLabel());
        assertEquals("Adresáře a protokoly", SettingsTab.DIRS.getLabel());
    }

    @Test
    public void fromKey() {
        assertSame(SettingsTab.VERSIONS, SettingsTab.fromKey("versions"));
        assertSame(SettingsTab.TOOLS, SettingsTab.fromKey("tools"));
        assertSame(SettingsTab.SKIPPING, SettingsTab.fromKey("skipping"));
        assertSame(SettingsTab.DICTIONARIES, SettingsTab.fromKey("dictionaries"));
        assertSame(SettingsTab.DIRS, SettingsTab.fromKey("dirs"));
        assertSame(SettingsTab.DIRS, SettingsTab.fromKey("  DIRS "));
    }

    @Test
    public void fromKeyUnknown() {
        assertNull(SettingsTab.fromKey(null));
        assertNull(SettingsTab.fromKey(""));
        assertNull(SettingsTab.fromKey("   "));
        assertNull(SettingsTab.fromKey("version"));
        assertNull(SettingsTab.fromKey("Výchozí verze DMF"));
    }
}
