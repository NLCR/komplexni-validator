package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;


/**
 * Created by Martin Řehánek on 21.11.16.
 */
public class VfCheckImageFilesValidByExternalUtilTest {

    //TODO: presunout
    private static final File EXAMPLES_ROOT_DIR = new File("src/test/resources/jpeg2000");
    //private static final File FDMD_ROOT = new File("src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2");
    private static final File FDMD_ROOT = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
    private static final File IMAGE_UTILS_CONFIG = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig/externalUtils.xml");
    private static File IMAGEMAGICK_PATH_LINUX = null;
    //ImageMagick-7.0.3-7-Q16-x64-static.exe
    private static File IMAGEMAGICK_PATH_WINDOWS = new File("C:\\Program Files\\ImageMagick-7.0.3-Q16");
    private static File IMAGEMAGICK_PATH_MAC = null;

    private static File JHOVE_PATH_LINUX = null;
    private static File JHOVE_PATH_WINDOWS = new File("C:\\Users\\Lenovo\\Documents\\software\\jhove-1_11\\jhove");
    private static File JHOVE_PATH_MAC = null;

    private static File JPYLYZER_PATH_LINUX = null;
    private static File JPYLYZER_PATH_WINDOWS = new File("C:\\Users\\Lenovo\\Documents\\software\\jpylyzer_1.17.0_win64");
    private static File JPYLYZER_PATH_MAC = null;

    private static File KAKADU_PATH_LINUX = new File("/home/martin/zakazky/NKP-Komplexni_Validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226");
    private static File KAKADU_PATH_WINDOWS = new File("C:\\Program Files (x86)\\Kakadu\\");
    private static File KAKADU_PATH_MAC = null;

    private static Engine engine;
    private static ExternalUtilManager externalUtilManager;
    private static List<File> FILES_OK_MC;
    private static List<File> FILES_OK_UC;
    private static List<File> FILES_PROFILE_MISMATCH_MC;
    private static List<File> FILES_PROFILE_MISMATCH_UC;
    private static List<File> FILES_ERROR_DETECTION;
    private static List<File> FILES_INCORRECT;
    private static List<File> FILES_INVALID;


    @BeforeClass
    public static void setup() throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {
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

    private static Engine initEngine() throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {
        Platform platform = Platform.detectOs();
        //TODO: should not be initialized here
        externalUtilManager = new ExternalUtilManagerFactory(IMAGE_UTILS_CONFIG).buildExternalUtilManager(platform.getOperatingSystem());
        switch (platform.getOperatingSystem()) {
            case WINDOWS:
                externalUtilManager.setPath(ExternalUtil.IMAGE_MAGICK, IMAGEMAGICK_PATH_WINDOWS);
                externalUtilManager.setPath(ExternalUtil.JHOVE, JHOVE_PATH_WINDOWS);
                externalUtilManager.setPath(ExternalUtil.JPYLYZER, JPYLYZER_PATH_WINDOWS);
                externalUtilManager.setPath(ExternalUtil.KAKADU, KAKADU_PATH_WINDOWS);
                break;
            case LINUX:
                externalUtilManager.setPath(ExternalUtil.IMAGE_MAGICK, IMAGEMAGICK_PATH_LINUX);
                externalUtilManager.setPath(ExternalUtil.JHOVE, JHOVE_PATH_LINUX);
                externalUtilManager.setPath(ExternalUtil.JPYLYZER, JPYLYZER_PATH_LINUX);
                externalUtilManager.setPath(ExternalUtil.KAKADU, KAKADU_PATH_LINUX);
                break;
            case MAC:
                externalUtilManager.setPath(ExternalUtil.IMAGE_MAGICK, IMAGEMAGICK_PATH_MAC);
                externalUtilManager.setPath(ExternalUtil.JHOVE, JHOVE_PATH_MAC);
                externalUtilManager.setPath(ExternalUtil.JPYLYZER, JPYLYZER_PATH_MAC);
                externalUtilManager.setPath(ExternalUtil.KAKADU, KAKADU_PATH_MAC);
                break;
        }


        detectImageTools(externalUtilManager);
        ValidatorConfigurationManager validatorConfigManager = new ValidatorConfigurationManager(FDMD_ROOT);
        FdmfConfiguration fdmfConfig = new FdmfRegistry(validatorConfigManager).getFdmfConfig(new Dmf(Dmf.Type.MONOGRAPH, "1.2"));
        fdmfConfig.initJ2kProfiles(externalUtilManager);
        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, new File("/tmp"), validatorConfigManager.getDictionaryManager());
        return validator.getEngine();
    }


    private static void detectImageTools(ExternalUtilManager externalUtilManager) {
        for (ExternalUtil util : ExternalUtil.values()) {
            System.out.print(String.format("Kontroluji dostupnost nástroje %s: ", util.getUserFriendlyName()));
            if (!externalUtilManager.isVersionDetectionDefined(util)) {
                System.out.println("není definován způsob detekce verze");
            } else if (!externalUtilManager.isUtilExecutionDefined(util)) {
                System.out.println("není definován způsob spuštění");
            } else {
                try {
                    String version = externalUtilManager.runUtilVersionDetection(util);
                    externalUtilManager.setUtilAvailable(util, true);
                    System.out.println("nalezen, verze: " + version);
                } catch (CliCommand.CliCommandException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ValidationFunction buildValidationFunction(ExternalUtil util, ImageCopy copy, List<File> files) {
        return new VfCheckImageFilesValidByExternalUtil("test", engine)
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
        if (externalUtilManager.isUtilAvailable(ExternalUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.KAKADU, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void kakaduOkUc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.KAKADU, ImageCopy.MASTER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void imageMagickOkMc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void imageMagickOkUc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.IMAGE_MAGICK, ImageCopy.USER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            //verze ImageMagick-7.0.3-Q16 pro Windows najde chybu v souboru UC_cnb001652709-001_0233.jp2
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }


    @Test
    public void jpylyzerOkMc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JPYLYZER, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerOkUc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JPYLYZER, ImageCopy.USER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jhoveOkMc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JHOVE, ImageCopy.MASTER, FILES_OK_MC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jhoveOkUc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JHOVE, ImageCopy.USER, FILES_OK_UC);
            ValidationResult result = validationFunction.validate();
            for (ValidationProblem problem : result.getProblems()) {
                assertNull(problem.getMessage());
            }
            assertFalse(result.hasProblems());
        }
    }

    @Test
    public void jhoveProfileMismatchMc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JHOVE, ImageCopy.MASTER, FILES_PROFILE_MISMATCH_MC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jhoveProfileMismatchUc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JHOVE, ImageCopy.USER, FILES_PROFILE_MISMATCH_UC);
            ValidationResult result = validationFunction.validate();
            //verze jhove-1_11 pro Windows nenajde zadnou chybu
            //ani Jhove (Rel. 1.6, 2011-01-04) pro Linux
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerProfileMismatchMc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JPYLYZER, ImageCopy.MASTER, FILES_PROFILE_MISMATCH_MC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerProfileMismatchUc() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JPYLYZER, ImageCopy.USER, FILES_PROFILE_MISMATCH_UC);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void kakaduErrorDetection() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.KAKADU, ImageCopy.MASTER, FILES_ERROR_DETECTION);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void imageMagickErrorDetection() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_ERROR_DETECTION);
            ValidationResult result = validationFunction.validate();
            //FIXME: neprojde na macOS pro ImageMagick 6.9.6-6 Q16 x86_64 2016-12-07
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void kakaduIncorrect() {
        //TODO: co je na nich spatne? timhle projdou
        /*if (externalUtilManager.isUtilAvailable(ExternalUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.KAKADU, ImageCopy.MASTER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }*/
    }

    @Test
    public void imageMagickIncorrect() {
        //TODO: co je na nich spatne? timhle projdou
        /*if (externalUtilManager.isUtilAvailable(ExternalUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }*/
    }

    @Test
    public void jpylyzerIncorrect() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JPYLYZER, ImageCopy.MASTER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jhoveIncorrect() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JHOVE, ImageCopy.USER, FILES_INCORRECT);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void kakaduInvalid() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.KAKADU)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.KAKADU, ImageCopy.MASTER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void imageMagickInvalid() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.IMAGE_MAGICK)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.IMAGE_MAGICK, ImageCopy.MASTER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            //FIXME: neprojde, mozna je to v datech
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jpylyzerInvalid() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JPYLYZER)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JPYLYZER, ImageCopy.MASTER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }

    @Test
    public void jhoveInvalid() {
        if (externalUtilManager.isUtilAvailable(ExternalUtil.JHOVE)) {
            ValidationFunction validationFunction = buildValidationFunction(ExternalUtil.JHOVE, ImageCopy.USER, FILES_INVALID);
            ValidationResult result = validationFunction.validate();
            assertTrue(result.hasProblems());
        }
    }


}
