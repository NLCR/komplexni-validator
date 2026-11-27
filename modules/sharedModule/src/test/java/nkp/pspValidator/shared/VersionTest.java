package nkp.pspValidator.shared;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verze aplikace z version.properties a vyber major verze (kontrolu verze konfigurace testuje ConfigManifestTest).
 */
public class VersionTest {

    @Test
    public void versionPropertiesGeneratedByGradle() {
        //testy bezi pres Gradle, version.properties je na classpath; fallback by znamenal rozbity build
        assertNotEquals(Version.FALLBACK_VERSION_CODE, Version.VERSION_CODE);
        assertNotEquals(Version.FALLBACK_BUILD_DATE, Version.BUILD_DATE);
        assertTrue(Version.BUILD_DATE, Version.BUILD_DATE.matches("\\d{1,2}\\. \\d{1,2}\\. \\d{4}"));
        assertNotNull(Version.BUILD_DATE_ISO);
        assertTrue(Version.BUILD_DATE_ISO, Version.BUILD_DATE_ISO.matches("\\d{4}-\\d{2}-\\d{2}"));
        assertNotNull("verze aplikace musi zacinat cislem: " + Version.VERSION_CODE, Version.majorVersion(Version.VERSION_CODE));
    }

    @Test
    public void majorVersion() {
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-alpha"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-alpha2"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-beta"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-beta2"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-rc1"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.1-dev"));
        // dřívější tvar s tečkou v příponě (3.0-alpha.0) konvenci neodpovídá, major se z něj ale přečte
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0-alpha.0"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.0.1"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3.1"));
        assertEquals(Integer.valueOf(3), Version.majorVersion("3"));
        assertEquals(Integer.valueOf(12), Version.majorVersion("12.4"));
        assertNull(Version.majorVersion(null));
        assertNull(Version.majorVersion(""));
        assertNull(Version.majorVersion("x.y"));
        assertNull(Version.majorVersion("3.0beta"));
    }
}
