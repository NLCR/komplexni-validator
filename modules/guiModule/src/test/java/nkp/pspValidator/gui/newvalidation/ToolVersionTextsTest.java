package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.VerifiedVersions;
import nkp.pspValidator.shared.externalUtils.VersionCheck;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Stav nástroje s ověřením verze v dialogu Externí nástroje, v Nastavení a v dialogu Nová validace: text stavu
 * („nalezen, JHOVE 1.36.0, neověřená verze“), žlutá tečka, tooltip s ověřenými řadami a přesnými verzemi, souhrn.
 */
public class ToolVersionTextsTest {

    private static final VerifiedVersions JHOVE_MAC = new VerifiedVersions(Arrays.asList(
            new VerifiedVersions.Series("1.29", "1.29.0-SNAPSHOT"),
            new VerifiedVersions.Series("1.34", "1.34.0")), null);
    private static final VerifiedVersions IM = new VerifiedVersions(
            Collections.singletonList(new VerifiedVersions.Series("7.1", "7.1.2-35")), null);

    private static ToolStatus found(VerifiedVersions verified, String version) {
        return ToolStatus.found(version, VersionCheck.evaluate(verified, version));
    }

    @Test
    public void unverifiedStatusText() {
        ToolStatus status = found(JHOVE_MAC, "Jhove (Rel. 1.36.0, 2026-01-15)");
        assertTrue(status.isAvailable());
        assertTrue(status.isVersionWarning());
        assertEquals("nalezen, JHOVE 1.36.0, neověřená verze", ToolsOverview.statusText(JHOVE, status));
        assertEquals("neověřená 1.36.0", ToolsOverview.compactStatusText(JHOVE, status));
        assertEquals("status-dot-warning", ToolsOverview.dotStyleClass(status));
        assertNull(ToolsOverview.textStyleClass(status));
        // neovereny nastroj neni nedostupny: bez odkazu na podrobnosti chyby
        assertFalse(ToolsOverview.hasDetails(status));
        assertEquals("nalezen, Jhove (Rel. 1.36.0, 2026-01-15)\n"
                + "Neověřená verze 1.36.0, výsledky kontrol se mohou lišit. Ověřené řady: 1.29 (ověřeno s verzí 1.29.0-SNAPSHOT), "
                + "1.34 (ověřeno s verzí 1.34.0).", ToolsOverview.statusTooltip(status));
    }

    @Test
    public void unverifiedSuffixIsKeptWhenVersionIsTruncated() {
        // zkracuje se jen nazev a verze (MAX_STATUS_LENGTH), oznaceni zustane cele
        ToolStatus status = found(IM, "ImageMagick 7.2.0-12345678901 Q16-HDRI");
        String text = ToolsOverview.statusText(IMAGE_MAGICK, status);
        assertEquals("nalezen, ImageMagick 7.2.0-12…, neověřená verze", text);
        assertEquals(ToolsOverview.MAX_STATUS_LENGTH + ", neověřená verze".length(), text.length());
    }

    @Test
    public void unrecognizedStatusText() {
        ToolStatus status = found(JHOVE_MAC, "Jhove (Rel. unknown)");
        assertTrue(status.isVersionWarning());
        assertEquals("nalezen, Jhove (Rel. unknown), verzi nelze rozpoznat", ToolsOverview.statusText(JHOVE, status));
        // v uzkem sloupci jen oznaceni
        assertEquals("verzi nelze rozpoznat", ToolsOverview.compactStatusText(JHOVE, status));
        assertEquals("status-dot-warning", ToolsOverview.dotStyleClass(status));
        assertEquals("nalezen, Jhove (Rel. unknown)\n"
                + "Verzi nelze rozpoznat, výsledky kontrol se mohou lišit. Ověřené řady: 1.29 (ověřeno s verzí 1.29.0-SNAPSHOT), "
                + "1.34 (ověřeno s verzí 1.34.0).", ToolsOverview.statusTooltip(status));
    }

    @Test
    public void verifiedStatusText() {
        ToolStatus status = found(IM, "ImageMagick 7.1.3-1 Q16-HDRI");
        assertFalse(status.isVersionWarning());
        assertEquals("nalezen, ImageMagick 7.1.3-1", ToolsOverview.statusText(IMAGE_MAGICK, status));
        assertEquals("nalezen, 7.1.3-1", ToolsOverview.compactStatusText(IMAGE_MAGICK, status));
        assertEquals("status-dot-ok", ToolsOverview.dotStyleClass(status));
        assertEquals("nalezen, ImageMagick 7.1.3-1 Q16-HDRI\nOvěřená řada 7.1 (ověřeno s verzí 7.1.2-35).", ToolsOverview.statusTooltip(status));
    }

    @Test
    public void verifiedOnMacIsNoteNotWarning() {
        ToolStatus status = found(JHOVE_MAC.inheritedFrom(OperatingSystem.MAC), "Rel. 1.34.0");
        assertFalse(status.isVersionWarning());
        assertEquals("nalezen, JHOVE 1.34.0", ToolsOverview.statusText(JHOVE, status));
        assertEquals("status-dot-ok", ToolsOverview.dotStyleClass(status));
        assertEquals("nalezen, Rel. 1.34.0\nOvěřená řada 1.34, ověřeno na macOS s verzí 1.34.0 (na této platformě zatím ne).",
                ToolsOverview.statusTooltip(status));
    }

    @Test
    public void unverifiedOnInheritingPlatformTooltip() {
        ToolStatus status = found(IM.inheritedFrom(OperatingSystem.MAC), "ImageMagick 6.9.11-60");
        assertEquals("nalezen, ImageMagick 6.9.11-60, neověřená verze", ToolsOverview.statusText(IMAGE_MAGICK, status));
        assertEquals("nalezen, ImageMagick 6.9.11-60\nNeověřená verze 6.9.11, výsledky kontrol se mohou lišit. "
                + "Ověřená řada (ověřeno na macOS): 7.1 (ověřeno s verzí 7.1.2-35).", ToolsOverview.statusTooltip(status));
    }

    @Test
    public void emptyListTooltip() {
        VerifiedVersions empty = new VerifiedVersions(Collections.<VerifiedVersions.Series>emptyList(), null);
        ToolStatus status = found(empty, "v8.5");
        assertEquals("nalezen, Kakadu 8.5, neověřená verze", ToolsOverview.statusText(KAKADU, status));
        assertEquals("nalezen, v8.5\nNeověřená verze 8.5, výsledky kontrol se mohou lišit. Na této platformě nebyla s validátorem "
                + "ověřena žádná verze.", ToolsOverview.statusTooltip(status));
        assertEquals("Neověřená verze 8.5, výsledky kontrol se mohou lišit. Na macOS nebyla s validátorem ověřena žádná verze.",
                ToolsOverview.versionDetails(VersionCheck.evaluate(empty.inheritedFrom(OperatingSystem.MAC), "v8.5")));
    }

    @Test
    public void seriesWithoutExactVersion() {
        VerifiedVersions plain = new VerifiedVersions(Arrays.asList(new VerifiedVersions.Series("1.29", null),
                new VerifiedVersions.Series("1.34", null)), null);
        assertEquals("Ověřená řada 1.34.", ToolsOverview.versionDetails(VersionCheck.evaluate(plain, "Rel. 1.34.0")));
        assertEquals("Ověřená řada 1.34, ověřeno na macOS (na této platformě zatím ne).",
                ToolsOverview.versionDetails(VersionCheck.evaluate(plain.inheritedFrom(OperatingSystem.MAC), "Rel. 1.34.0")));
        assertEquals("Neověřená verze 1.36.0, výsledky kontrol se mohou lišit. Ověřené řady: 1.29, 1.34.",
                ToolsOverview.versionDetails(VersionCheck.evaluate(plain, "Rel. 1.36.0")));
    }

    @Test
    public void withoutListNothingChanges() {
        // starsi konfigurace bez verifiedVersions, nebo stav bez porovnani verze (ToolStatus.found(version))
        for (ToolStatus status : new ToolStatus[]{
                ToolStatus.found("Jhove (Rel. 1.36.0, 2026-01-15)", VersionCheck.evaluate(null, "Jhove (Rel. 1.36.0, 2026-01-15)")),
                ToolStatus.found("Jhove (Rel. 1.36.0, 2026-01-15)")}) {
            assertFalse(status.isVersionWarning());
            assertEquals("nalezen, JHOVE 1.36.0", ToolsOverview.statusText(JHOVE, status));
            assertEquals("nalezen, 1.36.0", ToolsOverview.compactStatusText(JHOVE, status));
            assertEquals("status-dot-ok", ToolsOverview.dotStyleClass(status));
            assertEquals("nalezen, Jhove (Rel. 1.36.0, 2026-01-15)", ToolsOverview.statusTooltip(status));
        }
        assertNull(ToolsOverview.versionDetails(null));
    }

    @Test
    public void unavailableStatesIgnoreVersion() {
        assertFalse(ToolStatus.notFound("x").isVersionWarning());
        assertFalse(ToolStatus.error("x").isVersionWarning());
        assertFalse(ToolStatus.checking().isVersionWarning());
        assertNull(ToolStatus.notFound("x").getVersionCheck());
    }

    @Test
    public void unverifiedCountsAsAvailableInSummaries() {
        Map<ExternalUtil, ToolStatus> statuses = new EnumMap<>(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            statuses.put(util, ToolStatus.found("1.0"));
        }
        statuses.put(JHOVE, found(JHOVE_MAC, "Jhove (Rel. 1.36.0, 2026-01-15)"));
        statuses.put(KAKADU, ToolStatus.notFound("x"));
        assertEquals("8 z 9 nástrojů dostupných", ToolsOverview.summary(statuses));
        assertEquals(ToolsOverview.State.SOME_UNAVAILABLE, ToolsOverview.state(statuses));
        assertEquals("8 dostupných, 1 nedostupný", Summaries.toolsSummary(null, null, statuses, Collections.<ExternalUtil>emptySet()));
        statuses.put(KAKADU, ToolStatus.found("v8.5"));
        assertEquals(ToolsOverview.State.ALL_AVAILABLE, ToolsOverview.state(statuses));
        assertEquals("pro jednotku fondu se použijí 2 ze 2", Summaries.toolsSummary("jednotku fondu",
                java.util.EnumSet.of(JHOVE, KAKADU), statuses, Collections.<ExternalUtil>emptySet()));
    }

    @Test
    public void checkCarriesVersionCheckFromManager() {
        // ExternalTools.check porovna verzi s overenymi radami manazeru
        Map<ExternalUtil, VerifiedVersions> verified = new EnumMap<>(ExternalUtil.class);
        verified.put(JHOVE, JHOVE_MAC);
        nkp.pspValidator.shared.externalUtils.ExternalUtilManager manager = new nkp.pspValidator.shared.externalUtils.ExternalUtilManager(
                Collections.emptyMap(), Collections.emptyMap(), verified) {
            @Override
            public boolean isVersionDetectionDefined(ExternalUtil util) {
                return true;
            }

            @Override
            public String runUtilVersionDetection(ExternalUtil util) {
                return util == JHOVE ? "Jhove (Rel. 1.36.0, 2026-01-15)" : "Rel. 1.0";
            }
        };
        ToolStatus jhove = ExternalTools.check(manager, JHOVE);
        assertEquals(ToolStatus.State.FOUND, jhove.getState());
        assertEquals(VersionCheck.Status.UNVERIFIED, jhove.getVersionCheck().getStatus());
        assertEquals(VersionCheck.Status.NO_LIST, ExternalTools.check(manager, KAKADU).getVersionCheck().getStatus());
        // kontrola verzi do manazeru nezapisuje (to dela ToolChecks ve vlakne UI)
        assertNull(manager.getUtilVersion(JHOVE));
    }
}
