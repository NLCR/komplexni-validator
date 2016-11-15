package rzehan.gui.sample;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.imageUtils.CliCommand;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class Controller {

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getSimpleName());

    private static final int MAX_OUTPUT_LENGTH = 100;
    private static final String MC_FILE_LINUX = "/home/martin/zakazky/NKP-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/mastercopy/mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2";
    private static final String MC_FILE_WINDOWS = "C:\\Users\\Martin\\Documents\\PspValidator\\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2";


    @FXML
    Label osLabel;
    @FXML
    Label logLabel;

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
    //private final ImageUtilRegistry utilRegistry = ImageUtilRegistryImpl.instanceOf();
    private final ImageUtilManager utilManager;


    public Controller() {
        platform = Platform.detectOs();
        LOGGER.info("platform: " + platform.toString());
        utilManager = ImageUtilManagerFactory.instanceOf().buildImageUtilManager(platform.getOperatingSystem());
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


    public void detectUtilVersion(ImageUtil util, Label label) {
        //detectUtilVersionOnFxThread(util,label);
        detectUtilVersionOnWorkerThread(util, label);
    }

    public void detectUtilVersionOnFxThread(ImageUtil util, Label label) {
        label.setText(String.format("checking %s ...", util));
        try {
            if (!utilManager.isVersionDetectionDefined(util)) {
                label.setText(String.format("version detection not defined for %s", util));
            } else {
                String version = utilManager.runUtilVersionDetection(util);
                System.out.println(version);
                label.setText(version);
            }
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace() here throws IOEXception on Windows
            //e.printStackTrace();
            label.setText("not found");
        } catch (InterruptedException e) {
            //e.printStackTrace() here throws IOEXception on Windows
            //e.printStackTrace();
            label.setText("process interrupted");
        }
    }


    public void detectUtilVersionOnWorkerThread(ImageUtil util, Label label) {
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
                } catch (IOException e) {
                    //program probably does not exist
                    //e.printStackTrace() here throws IOEXception on Windows
                    //e.printStackTrace();
                    updateMessage("not found");
                } catch (InterruptedException e) {
                    //e.printStackTrace() here throws IOEXception on Windows
                    //e.printStackTrace();
                    updateMessage("process interrupted");
                }
                return null;
            }
        };

        new Thread(task).start();
        task.messageProperty().addListener((obs, oldMessage, newMessage) -> label.setText(newMessage));
    }


    public void runUtil(ImageUtil type, Label label, String imageFile) {
        runUtilOnWorkerThread(type, label, imageFile);
        //runUtilOnFxThread(type, label, imageFile);
    }


    public void runUtilOnFxThread(ImageUtil util, Label label, String imageFile) {
        label.setText(String.format("running %s ...", util));
        try {
            if (!utilManager.isUtilExecutionDefined(util)) {
                label.setText(String.format("util execution not defined for %s", util));
            } else {
                String output = utilManager.runUtilExecution(util, imageFile);
                System.out.println(output);
                String partial = output.replace("\n", " ").substring(0, Math.min(output.length(), MAX_OUTPUT_LENGTH)) + " ...";
                label.setText(partial);
            }
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace() here throws IOEXception on Windows
            //e.printStackTrace();
            label.setText("not found");
        } catch (InterruptedException e) {
            //e.printStackTrace() here throws IOEXception on Windows
            //e.printStackTrace();
            label.setText("process interrupted");
        }
    }

    public void runUtilOnWorkerThread(ImageUtil util, Label label, String imageFile) {
        label.setText(String.format("running %s ...", util));
        Task task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                if (isCancelled()) {
                    updateMessage("canceled");
                    return null;
                }
                try {
                    if (!utilManager.isUtilExecutionDefined(util)) {
                        updateMessage(String.format("util execution not defined for %s", util));
                    } else {
                        String output = utilManager.runUtilExecution(util, imageFile);
                        System.out.println(output);
                        String partial = output.replace("\n", " ").substring(0, Math.min(output.length(), MAX_OUTPUT_LENGTH)) + " ...";
                        updateMessage(partial);
                    }
                } catch (IOException e) {
                    //program probably does not exist
                    //e.printStackTrace() here throws IOEXception on Windows
                    //e.printStackTrace();
                    updateMessage("not found");
                } catch (InterruptedException e) {
                    //e.printStackTrace() here throws IOEXception on Windows
                    //e.printStackTrace();
                    updateMessage("process interrupted");
                }
                return null;
            }
        };
        new Thread(task).start();
        task.messageProperty().addListener((obs, oldMessage, newMessage) -> label.setText(newMessage));
    }

    private String getMcFile() {
        switch (platform.getOperatingSystem()) {
            case LINUX:
                return MC_FILE_LINUX;
            case WINDOWS:
                return MC_FILE_WINDOWS;
            default:
                throw new IllegalStateException("Unsupported platform");
        }
    }

    public void detectJpylyzerVersion(ActionEvent actionEvent) {
        detectUtilVersion(ImageUtil.JPYLYZER, detectJpylyzerVersionLabel);
    }

    public void detectJhoveVersion(ActionEvent actionEvent) {
        detectUtilVersion(ImageUtil.JHOVE, detectJhoveVersionLabel);
    }

    public void detectImageMagickVersion(ActionEvent actionEvent) {
        detectUtilVersion(ImageUtil.IMAGE_MAGICK, detectImageMagickVersionLabel);
    }

    public void detectKakaduVersion(ActionEvent actionEvent) {
        detectUtilVersion(ImageUtil.KAKADU, detectKakaduVersionLabel);
    }


    public void runJpylyzer(ActionEvent actionEvent) {
        runUtil(ImageUtil.JPYLYZER, runJpylyzerLabel, getMcFile());
    }


    public void runJhove(ActionEvent actionEvent) {
        runUtil(ImageUtil.JHOVE, runJhoveLabel, getMcFile());
    }


    public void runImageMagick(ActionEvent actionEvent) {
        runUtil(ImageUtil.IMAGE_MAGICK, runImageMagickLabel, getMcFile());
    }

    public void runKakadu(ActionEvent actionEvent) {
        runUtil(ImageUtil.KAKADU, runKakaduLabel, getMcFile());
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
                    file = dir + "\\resources\\bin\\ImageMagick-7.0.2-4-Q16-x64-dll.exe";
                    break;
                case LINUX:
                    file = dir + "/resources/bin/ImageMagick.deb";
                    break;
            }


            //String outStr = new CliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/fuckyou.sh");
            CliCommand.Result output = new CliCommand(file).execute();
            output.print();
            String outStr = output.getStdout();

            int length = Math.min(outStr.length(), MAX_OUTPUT_LENGTH);
            installImageMagickLabel.setText(outStr.substring(0, length) + " ...");

            //LOGGER.info(outStr);

            //LOGGER.info("current dir: " + Runtime.getRuntime().);


            //installImageMagickLabel.setText(outStr);
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            String output = e.getMessage();
            int length = Math.min(output.length(), MAX_OUTPUT_LENGTH);
            installImageMagickLabel.setText(output.substring(0, length) + " ...");
            //e.printStackTrace();
        } catch (InterruptedException e) {
            installImageMagickLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }

}
