package nkp.pspValidator.gui.newvalidation;

import org.junit.Test;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Odkaz „Podporované verze nástrojů“ ({@link ExternalTools#SUPPORTED_VERSIONS_URL}) vede na nadpis README.md, který
 * v README opravdu je. Kotva se z nadpisu odvozuje podle pravidel GitHubu, popsaných ručně v {@link #githubAnchor}:
 * malá písmena, vypustit všechno kromě písmen (i s diakritikou), číslic,
 * mezer, „-“ a „_“, mezery nahradit „-“; opakovaný nadpis dostane příponu „-1“, „-2“…
 */
public class SupportedVersionsLinkTest {

    private static final String README_URL_PREFIX = "https://github.com/NLCR/komplexni-validator/blob/master/README.md#";

    @Test
    public void urlAndText() {
        assertEquals("https://github.com/NLCR/komplexni-validator/blob/master/README.md#podporovan%C3%A9-verze-n%C3%A1stroj%C5%AF",
                ExternalTools.SUPPORTED_VERSIONS_URL);
        assertEquals("Podporované verze nástrojů", ExternalTools.SUPPORTED_VERSIONS_LINK_TEXT);
    }

    /** Pravidla ověřená na HTML, které GitHub vykreslil (README větve slouceni-vep 23. 11. 2026, wiki Instalace 23. 11. 2026). */
    @Test
    public void githubAnchorRules() {
        assertEquals("sestavení-a-spuštění", githubAnchor("Sestavení a spuštění"));
        assertEquals("verapdf-a-epubcheck-e-publikace", githubAnchor("veraPDF a EPUBCheck (e-publikace)"));
        assertEquals("podporované-dmf", githubAnchor("Podporované DMF"));
        assertEquals("instalace-nástrojů-pro-validaci-obrazovýchzvukových-souborů",
                githubAnchor("Instalace nástrojů pro validaci obrazových/zvukových souborů"));
        assertEquals("kakadu", githubAnchor("**Kakadu**"));
        assertEquals("nástroje-pro-kontrolu-zvukových-souborů-wav-mp3",
                githubAnchor("**Nástroje pro kontrolu zvukových souborů (WAV, mp3)**"));
    }

    @Test
    public void duplicateHeadingsGetSuffix() {
        Map<String, List<String>> anchors = headingAnchors("# A\n## Windows\ntext\n## Windows\n### Windows\n```\n# Windows\n```\n");
        assertEquals(Arrays.asList("A"), anchors.get("a"));
        assertEquals(Arrays.asList("Windows"), anchors.get("windows"));
        assertEquals(Arrays.asList("Windows"), anchors.get("windows-1"));
        assertEquals(Arrays.asList("Windows"), anchors.get("windows-2"));
        // nadpis v bloku kodu se nepocita
        assertNull(anchors.get("windows-3"));
    }

    @Test
    public void readmeHasHeadingOfSupportedVersionsLink() throws Exception {
        String url = ExternalTools.SUPPORTED_VERSIONS_URL;
        assertTrue(url, url.startsWith(README_URL_PREFIX));
        String anchor = URLDecoder.decode(url.substring(README_URL_PREFIX.length()), "UTF-8");
        File readme = findReadme();
        Map<String, List<String>> anchors = headingAnchors(new String(Files.readAllBytes(readme.toPath()), StandardCharsets.UTF_8));
        List<String> headings = anchors.get(anchor);
        assertNotNull("v " + readme + " není nadpis s kotvou „" + anchor + "“; kotvy README: " + anchors.keySet(), headings);
        assertEquals(Arrays.asList("Podporované verze nástrojů"), headings);
    }

    // ----- pomocné -----

    /**
     * Kořen repozitáře: první nadřazený adresář pracovního adresáře (Gradle spouští testy v adresáři modulu), který
     * obsahuje README.md i settings.gradle. Když se nenajde, test selže.
     */
    private static File findReadme() {
        File start = new File("").getAbsoluteFile();
        for (File dir = start; dir != null; dir = dir.getParentFile()) {
            File readme = new File(dir, "README.md");
            if (readme.isFile() && new File(dir, "settings.gradle").isFile()) {
                return readme;
            }
        }
        fail("README.md kořene repozitáře (adresář s README.md a settings.gradle) nenalezen nad " + start);
        return null;
    }

    /** Kotva nadpisu → texty nadpisů s touto kotvou (nadpisy ATX „#“ mimo bloky kódu ```), včetně přípon opakování. */
    static Map<String, List<String>> headingAnchors(String markdown) {
        Map<String, List<String>> result = new HashMap<>();
        Map<String, Integer> seen = new HashMap<>();
        boolean inFence = false;
        for (String line : markdown.replace("\r\n", "\n").split("\n")) {
            if (line.trim().startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (inFence || !line.matches("#{1,6} .*")) {
                continue;
            }
            String text = line.replaceFirst("^#{1,6} ", "").replaceFirst("\\s+#+\\s*$", "").trim();
            String base = githubAnchor(text);
            int count = seen.getOrDefault(base, 0);
            seen.put(base, count + 1);
            String anchor = count == 0 ? base : base + "-" + count;
            result.computeIfAbsent(anchor, a -> new ArrayList<>()).add(text);
        }
        return result;
    }

    static String githubAnchor(String headingMarkdown) {
        StringBuilder anchor = new StringBuilder();
        for (char c : headingMarkdown.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == ' ') {
                anchor.append('-');
            } else if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                anchor.append(c);
            }
        }
        return anchor.toString();
    }
}
