package rzehan.gui.sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import rzehan.shared.Platform;
import rzehan.shared.imageUtils.CliCommand;
import rzehan.shared.imageUtils.ImageUtil;
import rzehan.shared.imageUtils.ImageUtilRegistry;

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

    public Controller() {
        platform = Platform.detectOs();
        LOGGER.info("platform: " + platform.toString());
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

    public void detectUtilVersion(String utilName, Label label) {
        label.setText(String.format("checking %s ...", utilName));
        try {
            ImageUtil imageUtil = ImageUtilRegistry.getImageUtilByName().get(utilName);
            if (imageUtil == null) {
                label.setText(String.format("util '%s' not defined", utilName));
            } else {
                String version = imageUtil.runVersionDetection(platform);
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

    public void runUtil(String utilName, Label label, String imageFile) {
        label.setText(String.format("running %s ...", utilName));
        try {
            ImageUtil imageUtil = ImageUtilRegistry.getImageUtilByName().get(utilName);
            if (imageUtil == null) {
                label.setText(String.format("util '%s' not defined", utilName));
            } else {
                String output = imageUtil.runUtil(platform, imageFile);
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
        detectUtilVersion("jpylyzer", detectJpylyzerVersionLabel);
    }

    public void detectJhoveVersion(ActionEvent actionEvent) {
        detectUtilVersion("jhove", detectJhoveVersionLabel);
    }

    public void detectImageMagickVersion(ActionEvent actionEvent) {
        detectUtilVersion("imageMagick", detectImageMagickVersionLabel);
    }

    public void detectKakaduVersion(ActionEvent actionEvent) {
        detectUtilVersion("kakadu", detectKakaduVersionLabel);
    }


    public void runJpylyzer(ActionEvent actionEvent) {
        runUtil("jpylyzer", runJpylyzerLabel, getMcFile());
    }


    public void runJhove(ActionEvent actionEvent) {
        runUtil("jhove", runJhoveLabel, getMcFile());
    }


    public void runImageMagick(ActionEvent actionEvent) {
        runUtil("imageMagick", runImageMagickLabel, getMcFile());
    }

    public void runKakadu(ActionEvent actionEvent) {
        runUtil("kakadu", runKakaduLabel, getMcFile());
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
