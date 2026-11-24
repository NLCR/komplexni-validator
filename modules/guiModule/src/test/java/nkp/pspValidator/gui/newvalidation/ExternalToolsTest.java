package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Externí nástroje v dialogu Nová validace: relevantní nástroje podle pravidel fDMF, výběr nástrojů k vypnutí pro
 * jednu validaci a rozlišení „nenalezen“ / „chyba při spuštění“.
 */
public class ExternalToolsTest {

    @Test
    public void relevantToolsFromFdmfRules() throws Exception {
        FdmfRegistry registry = GuiTestSupport.registry();
        assertEquals(EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU), ExternalTools.relevantFor(registry, new Dmf(Dmf.Type.FUND_UNIT, "0.2")));
        assertEquals(EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU), ExternalTools.relevantFor(registry, new Dmf(Dmf.Type.PERIODICAL, "1.4")));
        assertEquals(EnumSet.of(VERAPDF, EPUBCHECK), ExternalTools.relevantFor(registry, new Dmf(Dmf.Type.EMONOGRAPH, "3.1")));
        assertEquals(EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU, MP3VAL, SHNTOOL, CHECKMATE),
                ExternalTools.relevantFor(registry, new Dmf(Dmf.Type.AUDIO_FONO, "1.0")));
        assertNull(ExternalTools.relevantFor(registry, new Dmf(Dmf.Type.FUND_UNIT, "9.9")));
        assertNull(ExternalTools.relevantFor(registry, null));
    }

    @Test
    public void toDisableOnlyUncheckedNotUnavailable() {
        Map<ExternalUtil, ToolStatus> statuses = new EnumMap<>(ExternalUtil.class);
        statuses.put(JHOVE, ToolStatus.found("1.29"));
        statuses.put(KAKADU, ToolStatus.error("x"));
        statuses.put(JPYLYZER, ToolStatus.notFound("x"));
        statuses.put(IMAGE_MAGICK, ToolStatus.checking());
        assertEquals(EnumSet.of(JHOVE, IMAGE_MAGICK),
                ExternalTools.toDisable(Arrays.asList(JHOVE, KAKADU, JPYLYZER, IMAGE_MAGICK), statuses));
        assertTrue(ExternalTools.toDisable(Collections.<ExternalUtil>emptySet(), statuses).isEmpty());
    }

    @Test
    public void failureClassification() {
        assertEquals(ToolStatus.State.NOT_FOUND,
                ExternalTools.classifyFailure("Cannot run program \"kdu_expand\": error=2, No such file or directory").getState());
        // EPUBCheck bez nastavené cesty: Java se spustí, ale jar nenajde (hláška z Windows 11, 24. 11. 2026)
        ToolStatus missingJar = ExternalTools.classifyFailure(
                "prázdný výstup (stderr: Error: Unable to access jarfile epubcheck.jar, návratový kód 1)");
        assertEquals(ToolStatus.State.NOT_FOUND, missingJar.getState());
        assertEquals("nenalezen", missingJar.getText());
        assertEquals("prázdný výstup (stderr: Error: Unable to access jarfile epubcheck.jar, návratový kód 1)", missingJar.getDetail());
        // jiná chyba Javy zůstává chybou při spuštění
        assertEquals(ToolStatus.State.ERROR,
                ExternalTools.classifyFailure("prázdný výstup (stderr: Error: Invalid or corrupt jarfile epubcheck.jar, návratový kód 1)").getState());
        ToolStatus error = ExternalTools.classifyFailure("prázdný výstup (návratový kód 1)");
        assertEquals(ToolStatus.State.ERROR, error.getState());
        assertEquals("chyba při spuštění", error.getText());
        assertEquals("prázdný výstup (návratový kód 1)", error.getDetail());
        assertEquals("nalezen, 1.29", ToolStatus.found("1.29").getText());
        assertEquals("nenalezen", ToolStatus.notFound(null).getText());
    }

    @Test
    public void everyToolHasDescription() {
        for (ExternalUtil util : ExternalUtil.values()) {
            assertFalse(util.name(), ExternalTools.description(util).isEmpty());
        }
    }

    @Test
    public void configKeysAsInConfigurationManager() {
        //stejne klice cte ValidationDataManager pri startu
        assertEquals("jhove.dir", ExternalTools.configKey(JHOVE));
        assertEquals("jpylyzer.dir", ExternalTools.configKey(JPYLYZER));
        assertEquals("imageMagick.dir", ExternalTools.configKey(IMAGE_MAGICK));
        assertEquals("kakadu.dir", ExternalTools.configKey(KAKADU));
        assertEquals("mp3val.dir", ExternalTools.configKey(MP3VAL));
        assertEquals("shntool.dir", ExternalTools.configKey(SHNTOOL));
        assertEquals("checkmate.dir", ExternalTools.configKey(CHECKMATE));
        assertEquals("verapdf.dir", ExternalTools.configKey(VERAPDF));
        assertEquals("epubcheck.dir", ExternalTools.configKey(EPUBCHECK));
    }

    @Test
    public void installationUrls() {
        String wiki = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#";
        assertEquals(wiki + "jhove", ExternalTools.installationUrl(JHOVE));
        assertEquals(wiki + "jpylyzer", ExternalTools.installationUrl(JPYLYZER));
        assertEquals(wiki + "imagemagick", ExternalTools.installationUrl(IMAGE_MAGICK));
        assertEquals(wiki + "kakadu", ExternalTools.installationUrl(KAKADU));
        assertEquals(wiki + "mp3val", ExternalTools.installationUrl(MP3VAL));
        assertEquals(wiki + "shntool", ExternalTools.installationUrl(SHNTOOL));
        assertEquals(wiki + "checkmate", ExternalTools.installationUrl(CHECKMATE));
        assertEquals(wiki + "verapdf", ExternalTools.installationUrl(VERAPDF));
        assertEquals(wiki + "epubcheck", ExternalTools.installationUrl(EPUBCHECK));
        //id nadpisu „Instalace nástrojů pro validaci obrazových/zvukových souborů“ ve wiki (GitHub lomítko vypouští),
        //ověřeno v HTML stránky 20. 11. 2026
        assertEquals(wiki + "instalace-n%C3%A1stroj%C5%AF-pro-validaci-obrazov%C3%BDchzvukov%C3%BDch-soubor%C5%AF",
                ExternalTools.INSTALLATION_HELP_URL);
    }
}
