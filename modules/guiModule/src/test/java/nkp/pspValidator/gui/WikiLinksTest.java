package nkp.pspValidator.gui;

import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.shared.Version;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Odkazy z aplikace na wiki. Po dobu bety 3.0 míří dočasně na stránky s příponou {@code -3.0-beta} (hlavní stránky
 * wiki popisují verzi 2.9). Test připomene vrácení odkazů: u finální verze (bez přípony předvydání) selže, dokud
 * odkazy na stránky bety v kódu zůstávají.
 */
public class WikiLinksTest {

    private static List<String> wikiLinks() {
        List<String> links = new ArrayList<>();
        links.add(MainController.URL_ONLINE_HELP);
        links.add(ExternalTools.INSTALLATION_HELP_URL);
        for (ExternalUtil util : ExternalUtil.values()) {
            links.add(ExternalTools.installationUrl(util));
        }
        return links;
    }

    /** Verze bez přípony za pomlčkou (3.0, 3.0.1) je finální; 3.0-beta, 3.0-rc1 nebo vývojová 0.0.0-dev ne. */
    static boolean isFinalVersion(String version) {
        return version != null && !version.contains("-");
    }

    @Test
    public void finalVersionDetection() {
        assertTrue(isFinalVersion("3.0"));
        assertTrue(isFinalVersion("3.0.1"));
        assertFalse(isFinalVersion("3.0-beta"));
        assertFalse(isFinalVersion("3.0-rc1"));
        assertFalse(isFinalVersion("0.0.0-dev"));
        assertFalse(isFinalVersion(null));
    }

    @Test
    public void finalVersionDoesNotLinkToBetaWikiPages() {
        if (!isFinalVersion(Version.VERSION_CODE)) {
            return; // předvydání smí odkazovat na stránky bety
        }
        for (String link : wikiLinks()) {
            assertFalse("Finální verze " + Version.VERSION_CODE + " odkazuje na stránku wiki pro betu: " + link
                    + " (vrátit odkazy na hlavní stránky wiki, viz .local/doc/wiki-revize/plan-a-postup.md)", link.contains("-beta"));
        }
    }

    @Test
    public void allLinksPointToProjectWiki() {
        for (String link : wikiLinks()) {
            assertTrue(link, link.startsWith("https://github.com/NLCR/komplexni-validator/wiki"));
        }
    }
}
