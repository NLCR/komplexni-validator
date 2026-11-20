package nkp.pspValidator.gui.settings;

import org.junit.Test;

import static org.junit.Assert.*;

/** Dotaz na restart po uložení nového adresáře konfigurace validátoru (texty napsané ručně). */
public class RestartPromptTest {

    @Test
    public void restartAvailable() {
        RestartPrompt prompt = RestartPrompt.of(true, false);
        assertEquals(RestartPrompt.Kind.RESTART, prompt.getKind());
        assertEquals("Restartovat aplikaci?", prompt.getTitle());
        assertEquals("Konfigurace validátoru se použije po restartu aplikace.", prompt.getMessage());
        assertEquals("Později: aplikace poběží dál s dosavadní konfigurací, nová se použije po příštím spuštění.", prompt.getNote());
        assertEquals("Restartovat teď", prompt.getPrimaryLabel());
        assertTrue(prompt.isPrimaryEnabled());
        assertEquals("Později", prompt.getLaterLabel());
    }

    @Test
    public void restartDuringValidation() {
        RestartPrompt prompt = RestartPrompt.of(true, true);
        assertEquals(RestartPrompt.Kind.RESTART, prompt.getKind());
        assertEquals("Restartovat aplikaci?", prompt.getTitle());
        assertEquals("Konfigurace validátoru se použije po restartu aplikace.", prompt.getMessage());
        assertEquals("Restartovat aplikaci teď nejde, běží validace. Změna se projeví po příštím spuštění aplikace.", prompt.getNote());
        assertEquals("Restartovat teď", prompt.getPrimaryLabel());
        assertFalse(prompt.isPrimaryEnabled());
        assertEquals("Později", prompt.getLaterLabel());
    }

    @Test
    public void noCommand() {
        RestartPrompt prompt = RestartPrompt.of(false, false);
        assertEquals(RestartPrompt.Kind.MANUAL, prompt.getKind());
        assertEquals("Ukončit aplikaci?", prompt.getTitle());
        assertEquals("Konfigurace validátoru se použije po restartu aplikace.", prompt.getMessage());
        assertEquals("Aplikaci nejde restartovat automaticky. Ukončete aplikaci a spusťte ji znovu. "
                + "Později: aplikace poběží dál s dosavadní konfigurací.", prompt.getNote());
        assertEquals("Ukončit aplikaci", prompt.getPrimaryLabel());
        assertTrue(prompt.isPrimaryEnabled());
        assertEquals("Později", prompt.getLaterLabel());
    }

    @Test
    public void noCommandDuringValidation() {
        RestartPrompt prompt = RestartPrompt.of(false, true);
        assertEquals(RestartPrompt.Kind.MANUAL, prompt.getKind());
        assertEquals("Ukončit aplikaci?", prompt.getTitle());
        assertEquals("Ukončit aplikaci teď nejde, běží validace. Změna se projeví po příštím spuštění aplikace.", prompt.getNote());
        assertEquals("Ukončit aplikaci", prompt.getPrimaryLabel());
        assertFalse(prompt.isPrimaryEnabled());
    }
}
