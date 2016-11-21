package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.ValidatorFactory;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ImageCopy;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;


/**
 * Created by martin on 21.11.16.
 */
public class VfCheckImageFilesValidByExternalUtilTest {

    //TODO: presunout
    private static final File EXAMPLES_ROOT_DIR = new File("src/test/resources/jpeg2000");
    private static final File FDMD_ROOT = new File("src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2");
    private static final File IMAGE_UTILS_CONFIG = new File("src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
    private static File KAKADU_PATH = new File("/home/martin/zakazky/NKP-PSP_validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226");

    private static Engine engine;
    private static ImageUtilManager imageUtilManager;
    private static List<File> FILES_OK_MC;
    private static List<File> FILES_OK_UC;
    private static List<File> FILES_PROFILE_MISMATCH_MC;
    private static List<File> FILES_PROFILE_MISMATCH_UC;
    private static List<File> FILES_ERROR_DETECTION;
    private static List<File> FILES_INCORRECT;
    private static List<File> FILES_INVALID;


    @BeforeClass
    public static void setup() throws ValidatorConfigurationException {
        System.err.println(new File("."));
        engine = initEngine();
        FILES_OK_UC = toFilesInDir("ok_uc");
        FILES_OK_MC = toFilesInDir("ok_mc");
        FILES_PROFILE_MISMATCH_UC = toFilesInDir("odlis_od_prof_uc");
        FILES_PROFILE_MISMATCH_MC = toFilesInDir("odlis_od_prof_mc");
        FILES_ERROR_DETECTION = toFilesInDir("detekce_poskozeni");
        //TODO: co je na nich spatne?
        FILES_INCORRECT = toFilesInDir("uplne_spatne");
        FILES_INVALID = toFilesInDir("nevalidni");
    }

    private static List<File> toFilesInDir(String dirName) {
        File dir = new File(EXAMPLES_ROOT_DIR, dirName);
        File[] array = dir.listFiles();
        return Arrays.asList(array);
    }

    private static Engine initEngine() throws ValidatorConfigurationException {
        Platform platform = Platform.detectOs();
        imageUtilManager = new ImageUtilManagerFactory(IMAGE_UTILS_CONFIG).buildImageUtilManager(platform.getOperatingSystem());
        imageUtilManager.setPath(ImageUtil.KAKADU, KAKADU_PATH);
        detectImageTools(imageUtilManager);
        Validator validator = ValidatorFactory.buildValidator(FDMD_ROOT, new File("/tmp"), imageUtilManager);
        return validator.getEngine();
    }


    private static void detectImageTools(ImageUtilManager imageUtilManager) {
        for (ImageUtil util : ImageUtil.values()) {
            System.out.print(String.format("Kontroluji dostupnost nástroje %s: ", util.getUserFriendlyName()));
            if (!imageUtilManager.isVersionDetectionDefined(util)) {
                System.out.println("není definován způsob detekce verze");
            } else if (!imageUtilManager.isUtilExecutionDefined(util)) {
                System.out.println("není definován způsob spuštění");
            } else {
                try {
                    String version = imageUtilManager.runUtilVersionDetection(util);
                    imageUtilManager.setUtilAvailable(util, true);
                    System.out.println("nalezen, verze: " + version);
                } catch (IOException e) {
                    //System.out.println("I/O chyba: " + e.getMessage());
                    System.out.println("nenalezen");
                } catch (InterruptedException e) {
                    System.out.println("detekce přerušena: " + e.getMessage());
                }
            }
        }
    }

    private ValidationFunction buildValidationFunction(ImageUtil util, ImageCopy copy, List<File> files) {
        return new VfCheckImageFilesValidByExternalUtil(engine)
                .withValueParam("files", ValueType.FILE_LIST, new ValueEvaluation(files))
                .withValueParam("level", ValueType.LEVEL, new ValueEvaluation(Level.WARNING))
                .withValueParam("copy", ValueType.IMAGE_COPY, new ValueEvaluation(copy))
                .withValueParam("util", ValueType.IMAGE_UTIL, new ValueEvaluation(util));
    }

    @Test
    public void nothing() {
        //tmp method, because of IDE imports handling
        assertTrue(true);
        assertFalse(false);
        assertNull(null);
        assertNotNull(new Object());
        assertEquals(null, null);
    }

    @Test
    public void kakaduOkMc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.KAKADU, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void kakaduOkUc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.KAKADU, ImageCopy.MASTER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void imageMagickOkMc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void imageMagickOkUc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.IMAGE_MAGICK, ImageCopy.USER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }


    @Test
    public void jpylyzerOkMc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JPYLYZER, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerOkUc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JPYLYZER, ImageCopy.USER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jhoveOkMc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JHOVE, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jhoveOkUc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JHOVE, ImageCopy.USER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationError problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jhoveProfileMismatchMc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JHOVE, ImageCopy.MASTER, FILES_PROFILE_MISMATCH_MC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jhoveProfileMismatchUc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JHOVE, ImageCopy.USER, FILES_PROFILE_MISMATCH_UC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerProfileMismatchMc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JPYLYZER, ImageCopy.MASTER, FILES_PROFILE_MISMATCH_MC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerProfileMismatchUc() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JPYLYZER, ImageCopy.USER, FILES_PROFILE_MISMATCH_UC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void kakaduErrorDetection() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.KAKADU, ImageCopy.MASTER, FILES_ERROR_DETECTION);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void imageMagickErrorDetection() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_ERROR_DETECTION);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void kakaduIncorrect() {
        //TODO: co je na nich spatne? timhle projdou
        /*if (imageUtilManager.isUtilAvailable(ImageUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.KAKADU, ImageCopy.MASTER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }*/
    }

    @Test
    public void imageMagickIncorrect() {
        //TODO: co je na nich spatne? timhle projdou
        /*if (imageUtilManager.isUtilAvailable(ImageUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }*/
    }

    @Test
    public void jpylyzerIncorrect() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JPYLYZER, ImageCopy.MASTER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jhoveIncorrect() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JHOVE, ImageCopy.USER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void kakaduInvalid() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.KAKADU, ImageCopy.MASTER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void imageMagickInvalid() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerInvalid() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JPYLYZER, ImageCopy.MASTER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jhoveInvalid() {
        if (imageUtilManager.isUtilAvailable(ImageUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ImageUtil.JHOVE, ImageCopy.USER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }


}
