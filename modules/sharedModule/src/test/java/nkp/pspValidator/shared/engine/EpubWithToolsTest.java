package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Syntetické balíčky e-publikací (EpubSmokeTest) se skutečnými nástroji veraPDF a EPUBCheck; ostatní nástroje jsou
 * vypnuté. Nástroj se hledá v PATH nebo v adresáři ze systémové vlastnosti verapdf.path / epubcheck.path (EPUBCheck
 * jako adresář s epubcheck.jar), např.
 * {@code ./gradlew :modules:sharedModule:test --tests '*EpubWithToolsTest' -Dverapdf.path=$(brew --prefix)/bin -Depubcheck.path=$(brew --prefix epubcheck)/libexec}.
 * Když nástroj chybí, je test přeskočený (Assume). Ověřeno s veraPDF 1.30 a EPUBCheck 5.4.0 (12. 11. 2026).
 */
public class EpubWithToolsTest {

    private static final String RES = "src/test/resources/epublikace/";

    /**
     * dummy_PDFA-1B.pdf je PDF/A-1b: veraPDF ani profil fDMF nic nehlásí.
     */
    @Test
    public void emo30PdfIsValidByVeraPdf() throws Exception {
        String log = runWithTools(new File(RES + "emonograph_3.0/valid_pdf/aba007-p5m301"), ExternalUtil.VERAPDF);
        assertNoProblemInRule(log, "ORIGINAL-COPY-PDF_FILES_VALID_BY_VERAPDF");
        assertValid(log);
    }

    /**
     * Archivní kopie není PDF/A (synteticky_bez_pdfa.pdf): bez nástrojů je balíček validní (EpubSmokeTest ho nemá
     * mezi pozitivními jen proto, že rozdíl dělá veraPDF), s veraPDF ERROR "Soubor musí být validní".
     */
    @Test
    public void emo30PdfNotPdfaIsInvalidByVeraPdf() throws Exception {
        File dir = new File(RES + "emonograph_3.0/pdf_not_pdfa/aba007-p5m305");
        assertValid(FdmfTestSupport.run(new DmfDetector().resolveDmf(dir, new DmfDetector.Params()), dir));
        String log = runWithTools(dir, ExternalUtil.VERAPDF);
        assertHasLineInRule(log, "ORIGINAL-COPY-PDF_FILES_VALID_BY_VERAPDF", "ERROR:", "Soubor musí být validní");
        assertTrue("balik ma byt nevalidni", log.contains("balík je: nevalidní"));
    }

    /**
     * synteticky_epub3.epub je EPUB 3: profil EPUBCheck z VEP očekává verzi 2.0.1 a hlásí jen WARNING (úmyslné
     * chování převzaté z VEP, viz plán P7 release notes), balíček zůstává validní.
     */
    @Test
    public void emo30EpubIsValidByEpubCheck() throws Exception {
        String log = runWithTools(new File(RES + "emonograph_3.0/valid_epub/aba007-p5m302"), ExternalUtil.EPUBCHECK);
        assertHasLineInRule(log, "ORIGINAL-COPY-EPUB_FILES_VALID_BY_EPUBCHECK", "WARNING:", "'2.0.1'");
        assertValid(log);
    }

    // ---- pomocné metody

    private static String runWithTools(File dir, ExternalUtil enabledUtil) throws Exception {
        ValidatorConfigurationManager configManager = FdmfTestSupport.sharedConfigManager();
        ExternalUtilManager utilManager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            if (util != enabledUtil) {
                utilManager.setUtilDisabled(util);
            }
        }
        String pathProperty = enabledUtil == ExternalUtil.VERAPDF ? "verapdf.path" : "epubcheck.path";
        String path = System.getProperty(pathProperty);
        if (path != null && !path.trim().isEmpty()) {
            utilManager.setPath(enabledUtil, new File(path.trim()));
        }
        try {
            utilManager.runUtilVersionDetection(enabledUtil);
            utilManager.setUtilAvailable(enabledUtil, true);
        } catch (CliCommand.CliCommandException e) {
            Assume.assumeTrue(enabledUtil.getUserFriendlyName() + " není dostupný (" + e.getMessage() + "), nastav PATH nebo -D" + pathProperty, false);
        }
        Dmf dmf = new DmfDetector().resolveDmf(dir, new DmfDetector.Params());
        FdmfConfiguration fdmfConfig = FdmfTestSupport.sharedRegistry().getFdmfConfig(dmf);
        // initBinaryFileProfiles přepíše BinaryFileValidator sdílené konfigurace, FdmfTestSupport.run ho před každým
        // během nastaví znovu s vypnutými nástroji
        fdmfConfig.initBinaryFileProfiles(utilManager);
        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, dir, configManager.getDictionaryManager());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, "UTF-8");
        validator.run(dir, null, out, 3, null, null, null, null, dmf);
        String log = buffer.toString("UTF-8");
        System.out.println(log);
        return log;
    }

    private static void assertValid(String log) {
        for (String line : log.split("\n")) {
            assertTrue("neocekavany ERROR: " + line.trim(), !line.trim().startsWith("ERROR:"));
        }
        assertTrue("balik ma byt validni", log.contains("balík je: validní"));
    }

    private static void assertHasLineInRule(String log, String rule, String prefix, String fragment) {
        String currentRule = null;
        for (String line : log.split("\n")) {
            if (line.startsWith("Pravidlo ")) {
                int colon = line.indexOf(':');
                currentRule = colon < 0 ? line.substring("Pravidlo ".length()).trim() : line.substring("Pravidlo ".length(), colon);
                continue;
            }
            if (rule.equals(currentRule) && line.trim().startsWith(prefix) && line.contains(fragment)) {
                return;
            }
        }
        assertTrue("ocekavan " + prefix + " pravidla " + rule + " obsahujici " + fragment, false);
    }

    private static void assertNoProblemInRule(String log, String rule) {
        assertTrue("pravidlo " + rule + " ma byt bez problemu", log.contains("Pravidlo " + rule + ": 0 problémů"));
    }
}
