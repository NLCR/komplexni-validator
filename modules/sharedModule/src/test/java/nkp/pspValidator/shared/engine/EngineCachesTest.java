package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Cache v Engine platne pro jeden beh validace: MD5 souboru (kontrola souboru CHECKSUM a fileSec primarniho METS
 * cte kazdy soubor jen jednou) a zkompilovana XSD (PREMIS, MIX, dmdSec, ...), ktera se drive kompilovala pro kazdy
 * element. Vysledky validace hlida ProtocolGoldenTest.
 */
public class EngineCachesTest {

    private static final File MON_132 = new File("src/test/resources/monograph_1.3.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
    private static final Dmf DMF_MON_132 = new Dmf(Dmf.Type.MONOGRAPH, "1.3.2");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void md5IsComputedOncePerFileInOneValidation() throws Exception {
        Set<String> checksumFileFiles = filesFromChecksumFile(MON_132);
        Set<String> fileSecFiles = filesFromPrimaryMetsFileSec(MON_132);
        Set<String> intersection = new HashSet<>(checksumFileFiles);
        intersection.retainAll(fileSecFiles);
        assertFalse("CHECKSUM a fileSec maji mit spolecne soubory", intersection.isEmpty());
        Set<String> union = new HashSet<>(checksumFileFiles);
        union.addAll(fileSecFiles);

        Validator validator = buildValidator(DMF_MON_132, MON_132);
        String log = run(validator, DMF_MON_132, MON_132);
        assertTrue(log, log.contains("balík je: validní"));
        //bez cache by to bylo checksumFileFiles.size() + fileSecFiles.size()
        assertEquals(union.size(), validator.getEngine().getMd5ComputationCount());

        //dalsi validace (CLI i GUI stavi pro kazdou validaci novy validator): MD5 se nesdileji napric validacemi
        Validator next = buildValidator(DMF_MON_132, MON_132);
        run(next, DMF_MON_132, MON_132);
        assertEquals(union.size(), next.getEngine().getMd5ComputationCount());
    }

    @Test
    public void md5CacheIsValidUntilCleared() throws Exception {
        Engine engine = new Engine(null);
        File file = tmp.newFile("data.txt");
        Files.write(file.toPath(), "puvodni".getBytes(StandardCharsets.UTF_8));
        String original = engine.computeMd5(file);
        assertEquals(DigestUtils.md5Hex("puvodni"), original);
        assertEquals(Utils.computeHash(file), original);

        Files.write(file.toPath(), "zmenene".getBytes(StandardCharsets.UTF_8));
        //v ramci jednoho behu se soubor nepocita znovu
        assertEquals(original, engine.computeMd5(new File(file.getAbsolutePath())));
        assertEquals(1, engine.getMd5ComputationCount());

        engine.clearMd5Cache();
        assertEquals(DigestUtils.md5Hex("zmenene"), engine.computeMd5(file));
        assertEquals(2, engine.getMd5ComputationCount());
    }

    @Test
    public void md5ErrorIsNotCached() throws Exception {
        Engine engine = new Engine(null);
        File missing = new File(tmp.getRoot(), "neexistuje.txt");
        for (int i = 0; i < 2; i++) {
            try {
                engine.computeMd5(missing);
                fail("chybejici soubor mel vyhodit HashComputationException");
            } catch (nkp.pspValidator.shared.engine.exceptions.HashComputationException e) {
                //text chyby je od operacniho systemu ("No such file or directory", na Windows v jazyce systemu),
                //proto se jeho zneni neporovnava
                assertTrue(e.getMessage() != null && !e.getMessage().isEmpty());
            }
        }
        assertEquals(0, engine.getMd5ComputationCount());
        Files.write(missing.toPath(), "obsah".getBytes(StandardCharsets.UTF_8));
        assertEquals(DigestUtils.md5Hex("obsah"), engine.computeMd5(missing));
    }

    @Test
    public void xsdIsCompiledOncePerFileInOneValidation() throws Exception {
        Validator validator = buildValidator(DMF_MON_132, MON_132);
        String log = run(validator, DMF_MON_132, MON_132);
        assertTrue(log, log.contains("balík je: validní"));
        int premisElements = countOccurrences(new File(MON_132, "amdsec"), "<premis:");
        assertTrue("balicek ma mit vic PREMIS elementu nez XSD souboru", premisElements > 20);
        //kazde XSD fDMF nejvyse jednou v kazde variante (bez resolveru, s resolverem importu)
        int xsdFiles = new File(FdmfTestSupport.VALIDATOR_CONFIG_DIR, "fDMF/monograph_1.3.2/xsd").listFiles((d, n) -> n.endsWith(".xsd")).length;
        int compilations = validator.getEngine().getXsdSchemaCache().getCompilationCount();
        assertTrue("kompilaci: " + compilations, compilations > 0);
        assertTrue("kompilaci: " + compilations + ", XSD: " + xsdFiles, compilations <= 2 * xsdFiles);
    }

    private static Validator buildValidator(Dmf dmf, File pspDir) throws Exception {
        ValidatorConfigurationManager configManager = FdmfTestSupport.sharedConfigManager();
        FdmfConfiguration fdmfConfig = FdmfTestSupport.sharedRegistry().getFdmfConfig(dmf);
        ExternalUtilManager utilManager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            utilManager.setUtilDisabled(util);
        }
        fdmfConfig.initBinaryFileProfiles(utilManager);
        return ValidatorFactory.buildValidator(fdmfConfig, pspDir, configManager.getDictionaryManager());
    }

    private static String run(Validator validator, Dmf dmf, File pspDir) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, "UTF-8");
        validator.run(pspDir, null, out, 1, null, null, null, null, dmf);
        return buffer.toString("UTF-8");
    }

    private static Set<String> filesFromChecksumFile(File pspDir) throws Exception {
        File[] md5Files = pspDir.listFiles((d, n) -> n.startsWith("md5_") && n.endsWith(".md5"));
        assertEquals(1, md5Files.length);
        Set<String> result = new HashSet<>();
        List<String> lines = Files.readAllLines(md5Files[0].toPath(), StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] parts = line.split("[ \\t]");
            result.add(Utils.buildAbsoluteFile(pspDir, parts[1]).getAbsolutePath());
        }
        return result;
    }

    private static Set<String> filesFromPrimaryMetsFileSec(File pspDir) throws Exception {
        File[] metsFiles = pspDir.listFiles((d, n) -> n.startsWith("mets_") && n.endsWith(".xml"));
        assertEquals(1, metsFiles.length);
        String mets = new String(Files.readAllBytes(metsFiles[0].toPath()), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<mets:FLocat[^>]*xlink:href=\"([^\"]*)\"").matcher(mets);
        Set<String> result = new HashSet<>();
        while (m.find()) {
            result.add(Utils.buildAbsoluteFile(pspDir, m.group(1)).getAbsolutePath());
        }
        return result;
    }

    private static int countOccurrences(File dir, String text) throws Exception {
        int count = 0;
        for (File file : dir.listFiles()) {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            count += content.split(java.util.regex.Pattern.quote(text), -1).length - 1;
        }
        return count;
    }
}
