package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.loadProfile;
import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.output;
import static org.junit.Assert.*;

/**
 * Profily imageMagick.xml nad skutecnymi vystupy ImageMagicku 6.9 a 7.1 (nalez N1 z overeni 23. 11. 2026).
 * <p>
 * ImageMagick 6.9 z Debianu a Ubuntu spusteny z PATH (prikaz "identify") pise hlasky s nazvem programu
 * "identify-im6.q16:", spusteny absolutni cestou (/usr/bin/identify, validator se zadanou cestou) "identify:".
 * ImageMagick 7.1 pise "identify:" vzdy. Profil musi u poskozeneho JP2 najit vsech 6 hlasek ve vsech tvarech
 * a kazdou hlasku vratit zvlast. Vystupy viz src/test/resources/externalUtils/README.md.
 */
public class ImageMagickProfileTest {

    private static final String FDMF = "fund_unit_0.2";
    private static final File DUMMY_FILE = new File("src/test/resources/jpeg2000/detekce_poskozeni/w0352gUC.jp2");
    private static final String PREFIX = "no_errors: zakázaná hodnota nalezena: ";

    private static List<Validation.Problem> problems(String fdmf, String copy, String outputName) throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.IMAGE_MAGICK, output("imageMagick", outputName));
        return loadProfile(manager, fdmf, copy, ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE);
    }

    private static List<String> messages(List<Validation.Problem> problems) {
        List<String> result = new ArrayList<>();
        for (Validation.Problem problem : problems) {
            // uroven hlasky no_errors neni v profilu urcena, bere se z pravidla (rules.xml)
            assertNull(problem.toString(), problem.getLevel());
            result.add(problem.getMessage());
        }
        return result;
    }

    @Test
    public void imageMagick69FromPathFindsEveryMessage() throws Exception {
        List<String> expected = Arrays.asList(
                PREFIX + "identify-im6.q16: Expected SOP marker\n `OpenJP2' @ warning/jp2.c/JP2WarningHandler/240.",
                PREFIX + "identify-im6.q16: Expected EPH marker\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify-im6.q16: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify-im6.q16: Failed to decode tile 1/8\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify-im6.q16: Failed to decode the codestream in the JP2 file\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify-im6.q16: unable to decode image file `src/test/resources/jpeg2000/detekce_poskozeni/w0352gUC.jp2' @ error/jp2.c/ReadJP2Image/403.");
        for (String copy : Arrays.asList("mc", "uc")) {
            assertEquals(copy, expected, messages(problems(FDMF, copy, "im69_path_detekce_poskozeni_w0352gUC.txt")));
        }
    }

    @Test
    public void imageMagick69WithAbsolutePathFindsEveryMessage() throws Exception {
        assertEquals(Arrays.asList(
                PREFIX + "identify: Expected SOP marker\n `OpenJP2' @ warning/jp2.c/JP2WarningHandler/240.",
                PREFIX + "identify: Expected EPH marker\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify: Failed to decode tile 1/8\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify: Failed to decode the codestream in the JP2 file\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify: unable to decode image file `src/test/resources/jpeg2000/detekce_poskozeni/w0352gUC.jp2' @ error/jp2.c/ReadJP2Image/403."
        ), messages(problems(FDMF, "uc", "im69_absolutni_cesta_detekce_poskozeni_w0352gUC.txt")));
    }

    @Test
    public void imageMagick71FindsEveryMessage() throws Exception {
        assertEquals(Arrays.asList(
                PREFIX + "identify: Expected SOP marker\n `OpenJP2' @ warning/jp2.c/JP2WarningHandler/256.",
                PREFIX + "identify: Expected EPH marker\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/213.",
                PREFIX + "identify: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/213.",
                PREFIX + "identify: Failed to decode tile 1/8\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/213.",
                PREFIX + "identify: Failed to decode the codestream in the JP2 file\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/213.",
                PREFIX + "identify: unable to decode image file `src/test/resources/jpeg2000/detekce_poskozeni/w0352gUC.jp2' @ error/jp2.c/ReadJP2Image/446."
        ), messages(problems(FDMF, "uc", "im71_detekce_poskozeni_w0352gUC.txt")));
    }

    @Test
    public void imageMagick69GoodFileHasNoProblem() throws Exception {
        for (String copy : Arrays.asList("mc", "uc")) {
            assertEquals(copy, Collections.emptyList(), problems(FDMF, copy, "im69_path_tools_mc.txt"));
        }
    }

    @Test
    public void programNameWithOtherSuffix() throws Exception {
        // jina sestaveni ImageMagicku 6 (napr. s HDRI) maji jinou priponu; synteticke hlasky odvozene ze skutecnych (6.9.12-98)
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.IMAGE_MAGICK,
                "identify-im6.q16hdri: Expected EPH marker\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.\n"
                        + "identify-im6.q16hdri: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.\n");
        assertEquals(Arrays.asList(
                PREFIX + "identify-im6.q16hdri: Expected EPH marker\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.",
                PREFIX + "identify-im6.q16hdri: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197."
        ), messages(loadProfile(manager, FDMF, "uc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE)));
    }

    /**
     * Slovo „identify“ uvnitř hlášky (adresář nebo soubor toho jména v cestě) hlášku neukončuje: hranicí další hlášky
     * je název programu s dvojtečkou na začátku řádku. Dřív se hláška usekla před cestou a název souboru se ztratil.
     */
    @Test
    public void identifyInsidePathDoesNotCutTheMessage() throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.IMAGE_MAGICK,
                "identify-im6.q16: unable to decode image file `/data/identify/bad.jp2' @ error/jp2.c/ReadJP2Image/401.\n"
                        + "identify-im6.q16: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197.\n");
        assertEquals(Arrays.asList(
                PREFIX + "identify-im6.q16: unable to decode image file `/data/identify/bad.jp2' @ error/jp2.c/ReadJP2Image/401.",
                PREFIX + "identify-im6.q16: Failed to decode.\n `OpenJP2' @ error/jp2.c/JP2ErrorHandler/197."
        ), messages(loadProfile(manager, FDMF, "uc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE)));
        //totez s tvarem ImageMagicku 7 a souborem pojmenovanym identify
        manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.IMAGE_MAGICK,
                "identify: unable to open image 'C:\\skeny\\identify: test\\identify.jp2': No such file or directory @ error/blob.c/OpenBlob/3596.\n");
        assertEquals(Collections.singletonList(
                PREFIX + "identify: unable to open image 'C:\\skeny\\identify: test\\identify.jp2': No such file or directory @ error/blob.c/OpenBlob/3596."
        ), messages(loadProfile(manager, FDMF, "uc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE)));
    }

    @Test
    public void wordStartingWithIdentifyIsNotProgramName() throws Exception {
        // pripona se oddeluje pomlckou, "identifying:" neni nazev programu
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.IMAGE_MAGICK, "identifying: nothing\n");
        assertEquals(Collections.emptyList(),
                loadProfile(manager, FDMF, "uc", ExternalUtil.IMAGE_MAGICK).validateProblems("jp2k", DUMMY_FILE));
    }

    /**
     * Vsech 78 profilu imageMagick.xml (mc a uc kazde fDMF) najde u ImageMagicku 6.9 z PATH stejnych 6 hlasek.
     */
    @Test
    public void everyFdmfProfileFindsImageMagick69Messages() throws Exception {
        File[] fdmfDirs = ExternalUtilsTestSupport.FDMF_DIR.listFiles(File::isDirectory);
        assertNotNull(fdmfDirs);
        Arrays.sort(fdmfDirs);
        int profiles = 0;
        for (File fdmfDir : fdmfDirs) {
            for (String copy : Arrays.asList("mc", "uc")) {
                if (!ExternalUtilsTestSupport.profileFile(fdmfDir.getName(), copy, ExternalUtil.IMAGE_MAGICK).isFile()) {
                    continue;
                }
                profiles++;
                List<String> found = messages(problems(fdmfDir.getName(), copy, "im69_path_detekce_poskozeni_w0352gUC.txt"));
                assertEquals(fdmfDir.getName() + "/" + copy + ": " + found, 6, found.size());
                for (String message : found) {
                    assertTrue(fdmfDir.getName() + "/" + copy + ": " + message, message.startsWith(PREFIX + "identify-im6.q16: "));
                }
            }
        }
        assertEquals(78, profiles);
    }
}
