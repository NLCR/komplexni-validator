package nkp.pspValidator.gui.sample;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import nkp.pspValidator.shared.externalUtils.ExternalUtilExecution;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class SampleController {

    private static final Logger LOGGER = Logger.getLogger(SampleController.class.getSimpleName());

    private static final int MAX_OUTPUT_LENGTH = 100;
    private static final File MC_FILE_LINUX = new File("/home/martin/Dropbox/PspValidator/data/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/mastercopy/mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2");
    private static final File MC_FILE_WINDOWS = new File("C:\\Users\\Lenovo\\Dropbox\\PspValidator\\data\\monograph_1.2\\b50eb6b0-f0a4-11e3-b72e-005056827e52\\mastercopy\\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2");
    private static final File MC_FILE_MAC = new File("/Users/martinrehanek/Dropbox/PspValidator/data/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/mastercopy/mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2");

    private static final File IMAGE_PROPERTIES_LINUX = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/externalUtils.xml");
    private static final File IMAGE_PROPERTIES_WINDOWS = new File("C:\\Users\\Lenovo\\IdeaProjects\\PspValidator\\sharedModule\\src\\main\\resources\\nkp\\pspValidator\\shared\\fDMF\\externalUtils.xml");
    private static final File IMAGE_PROPERTIES_MAC = new File("/Users/martinrehanek/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/externalUtils.xml");

    @FXML
    Label logLabel;
    @FXML
    Label osLabel;

    @FXML
    Label detectJpylyzerVersionLabel;
    @FXML
    Label runJpylyzerLabel;
    @FXML
    Label detectJhoveVersionLabel;
    @FXML
    Label runJhoveLabel;

    @FXML
    Label detectImageMagickVersionLabel;
    @FXML
    Label installImageMagickLabel;
    @FXML
    Label runImageMagickLabel;
    @FXML
    Label detectKakaduVersionLabel;
    @FXML
    Label runKakaduLabel;


    private final Platform platform;
    private final ExternalUtilManager utilManager;


    public SampleController() throws ValidatorConfigurationException {
        platform = Platform.detectOs();
        LOGGER.info("platform: " + platform.toString());
        File imageProperties = getImageProperties(platform);
        utilManager = new ExternalUtilManagerFactory(imageProperties).buildExternalUtilManager(platform.getOperatingSystem());
        //paths
        switch (platform.getOperatingSystem()) {
            case LINUX: {
                utilManager.setPath(ExternalUtil.KAKADU, new File("/home/martin/zakazky/NKP-Komplexni_Validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226"));
                break;
            }
            case WINDOWS: {
                utilManager.setPath(ExternalUtil.JHOVE, new File("C:\\Users\\Lenovo\\Documents\\software\\jhove"));
                utilManager.setPath(ExternalUtil.JPYLYZER, new File("C:\\Users\\Lenovo\\Documents\\software\\jpylyzer_1.17.0_win64"));
                utilManager.setPath(ExternalUtil.IMAGE_MAGICK, new File("C:\\Program Files\\ImageMagick-7.0.3-Q16"));
                utilManager.setPath(ExternalUtil.KAKADU, new File("C:\\Program Files (x86)\\Kakadu\\"));
                break;
            }
            case MAC: {
                utilManager.setPath(ExternalUtil.JHOVE, new File("/Users/martinrehanek/Software/jhove"));
                utilManager.setPath(ExternalUtil.JPYLYZER, new File("/Users/martinrehanek/Software/jpylyzer-1.17.0/jpylyzer"));
                utilManager.setPath(ExternalUtil.IMAGE_MAGICK, new File("/opt/local/bin"));
                break;
            }
            default:
                throw new IllegalStateException("Unsupported platform: " + platform);
        }
    }

    private File getImageProperties(Platform platform) {
        switch (platform.getOperatingSystem()) {
            case LINUX:
                return IMAGE_PROPERTIES_LINUX;
            case WINDOWS:
                return IMAGE_PROPERTIES_WINDOWS;
            case MAC:
                return IMAGE_PROPERTIES_MAC;
            default:
                throw new IllegalStateException("Unsupported platform");
        }
    }

    public void initialize() {
        osLabel.setText(platform.toReadableString());
    }


    public void logProperties(ActionEvent actionEvent) {
        StringBuilder builder = new StringBuilder();
        Properties properties = System.getProperties();
        List<String> propertyNames = new ArrayList<String>();
        propertyNames.addAll(properties.stringPropertyNames());
        Collections.sort(propertyNames);

        for (String propertyName : propertyNames) {
            String line = String.format("%s: %s", propertyName, properties.getProperty(propertyName));
            builder.append(line).append("\n");
            //LOGGER.info();
        }
        logLabel.setText(builder.toString());
        installImageMagick(actionEvent);
    }


    public void detectUtilVersion(ExternalUtil util, Label label) {
        //detectUtilVersionOnFxThread(util,label);
        detectUtilVersionOnWorkerThread(util, label);
    }

    public void detectUtilVersionOnFxThread(ExternalUtil util, Label label) {
        label.setText(String.format("checking %s ...", util));
        try {
            if (!utilManager.isVersionDetectionDefined(util)) {
                label.setText(String.format("version detection not defined for %s", util));
            } else {
                String version = utilManager.runUtilVersionDetection(util);
                System.out.println(version);
                label.setText(version);
            }
        } catch (CliCommand.CliCommandException e) {
            //program probably does not exist
            //e.printStackTrace();
            label.setText("not found: " + e.getMessage());
        }
    }


    public void detectUtilVersionOnWorkerThread(ExternalUtil util, Label label) {
        label.setText(String.format("checking %s ...", util));
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    if (!utilManager.isVersionDetectionDefined(util)) {
                        updateMessage(String.format("version detection not defined for %s", util));
                    } else {
                        String version = utilManager.runUtilVersionDetection(util);
                        System.out.println(version);
                        updateMessage(version);
                    }
                } catch (CliCommand.CliCommandException e) {
                    //program probably does not exist
                    //e.printStackTrace();
                    updateMessage("not found: " + e.getMessage());
                }
                return null;
            }
        };

        new Thread(task).start();
        task.messageProperty().addListener((obs, oldMessage, newMessage) -> label.setText(newMessage));
    }


    public void runUtil(ExternalUtilExecution exec, Label label, File imageFile) {
        runUtilOnWorkerThread(exec, label, imageFile);
        //runUtilOnFxThread(exec, label, imageFile);
    }


    public void runUtilOnFxThread(ExternalUtilExecution exec, Label label, File imageFile) {
        label.setText(String.format("running %s ...", exec));
        try {
            if (!utilManager.isUtilExecutionDefined(exec)) {
                label.setText(String.format("util execution not defined for %s", exec));
            } else {
                String output = utilManager.runUtilExecution(exec, imageFile);
                System.out.println(output);
                String partial = output.replace("\n", " ").substring(0, Math.min(output.length(), MAX_OUTPUT_LENGTH)) + " ...";
                label.setText(partial);
            }
        } catch (CliCommand.CliCommandException e) {
            //program probably does not exist
            label.setText("not found: " + e.getMessage());
        }
    }

    public void runUtilOnWorkerThread(ExternalUtilExecution exec, Label label, File imageFile) {
        label.setText(String.format("running %s ...", exec));
        Task task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                if (isCancelled()) {
                    updateMessage("canceled");
                    return null;
                }
                try {
                    if (!utilManager.isUtilExecutionDefined(exec)) {
                        updateMessage(String.format("util execution not defined for %s", exec));
                    } else {
                        String output = utilManager.runUtilExecution(exec, imageFile);
                        System.out.println("output: " + output);
                        String partial = output.replace("\n", " ").substring(0, Math.min(output.length(), MAX_OUTPUT_LENGTH)) + " ...";
                        updateMessage(partial);
                    }
                } catch (CliCommand.CliCommandException e) {
                    updateMessage("not found: " + e.getMessage());
                }
                return null;
            }
        };
        new Thread(task).start();
        task.messageProperty().addListener((obs, oldMessage, newMessage) -> label.setText(newMessage));
    }

    private File getMcFile() {
        switch (platform.getOperatingSystem()) {
            case LINUX:
                return MC_FILE_LINUX;
            case WINDOWS:
                return MC_FILE_WINDOWS;
            case MAC:
                return MC_FILE_MAC;
            default:
                throw new IllegalStateException("Unsupported platform");
        }
    }

    public void detectJpylyzerVersion(ActionEvent actionEvent) {
        detectUtilVersion(ExternalUtil.JPYLYZER, detectJpylyzerVersionLabel);
    }

    public void detectJhoveVersion(ActionEvent actionEvent) {
        detectUtilVersion(ExternalUtil.JHOVE, detectJhoveVersionLabel);
    }

    public void detectImageMagickVersion(ActionEvent actionEvent) {
        detectUtilVersion(ExternalUtil.IMAGE_MAGICK, detectImageMagickVersionLabel);
    }

    public void detectKakaduVersion(ActionEvent actionEvent) {
        detectUtilVersion(ExternalUtil.KAKADU, detectKakaduVersionLabel);
    }


    public void runJpylyzer(ActionEvent actionEvent) {
        runUtil(new ExternalUtilExecution("jp2k", ExternalUtil.JPYLYZER), runJpylyzerLabel, getMcFile());
    }


    public void runJhove(ActionEvent actionEvent) {
        runUtil(new ExternalUtilExecution("jp2k", ExternalUtil.JHOVE), runJhoveLabel, getMcFile());
    }


    public void runImageMagick(ActionEvent actionEvent) {
        runUtil(new ExternalUtilExecution("jp2k", ExternalUtil.IMAGE_MAGICK), runImageMagickLabel, getMcFile());
    }

    public void runKakadu(ActionEvent actionEvent) {
        runUtil(new ExternalUtilExecution("jp2k", ExternalUtil.KAKADU), runKakaduLabel, getMcFile());
    }

    public void installImageMagick(ActionEvent actionEvent) {
        try {
            //String outStr = new CliCommand("sudo aptitude search imageMagick");
            //String outStr = new CliCommand("totem");
            //String outStr = new CliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/pomodoro/Pomodoro");


            //URL url = getClass().getResource("res/bin");
            //LOGGER.info(url.getFile());

            //String outStr = new CliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/pomodoro/Pomodoro");
            //String outStr = new CliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/imagemagick.deb");
            String dir = System.getProperty("user.dir");
            //String file = dir + File.separator + "res" + File.separator + "bin"
            String file = null;
            switch (platform.getOperatingSystem()) {
                case WINDOWS:
                    file = "\\resources\\bin\\ImageMagick-7.0.2-4-Q16-x64-dll.exe";
                    break;
                case LINUX:
                    file = "/resources/bin/ImageMagick.deb";
                    break;
            }


            //String outStr = new CliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/fuckyou.sh");
            CliCommand.Result output = new CliCommand(new String[]{file}).execute();
            System.err.println("executed");
            output.print();
            String outStr = output.getStdout();

            int length = Math.min(outStr.length(), MAX_OUTPUT_LENGTH);
            installImageMagickLabel.setText(outStr.substring(0, length) + " ...");

            //LOGGER.info(outStr);

            //LOGGER.info("current dir: " + Runtime.getRuntime().);


            //installImageMagickLabel.setText(outStr);
        } catch (CliCommand.CliCommandException e) {
            //program probably does not exist
            installImageMagickLabel.setText("not found: " + e.getMessage());
        }
    }

}
