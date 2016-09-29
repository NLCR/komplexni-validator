package rzehan.gui.sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import rzehan.shared.Platform;
import rzehan.shared.imageUtils.CliCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class Controller {

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getSimpleName());

    private static final int MAX_OUTPUT_LENGTH = 100;
    private static final String MC_FILE = "/home/martin/zakazky/NKP-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/mastercopy/mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2";


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


    public void detectJpylyzerVersion(ActionEvent actionEvent) {
        detectJpylyzerVersionLabel.setText("checking jplyzer ...");
        try {
            CliCommand.Result output = new CliCommand("jpylyzer --version").execute();
            output.print();
            //jediny radek STDERR obsahuje verzi typu "1.17.0"
            detectJpylyzerVersionLabel.setText(output.getStderr());
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace() throws IOEXception on Windows
            // e.printStackTrace();
            detectJpylyzerVersionLabel.setText("not found");
        } catch (InterruptedException e) {
            detectJpylyzerVersionLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }

    public void runJpylyzer(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runJpylyzerLabel.setText("running jplyzer ...");
        try {
            CliCommand.Result output = new CliCommand("jpylyzer jp2In " + imageFile).execute();
            output.print();
            //v STDERR nesmyslna hlaska "jp2In does not exist"
            String outStr = output.getStdout();
            outStr = outStr.replace("\n", "");
            int length = Math.min(outStr.length(), MAX_OUTPUT_LENGTH);
            runJpylyzerLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace() throws IOEXception on Windows
            //e.printStackTrace();
            runJpylyzerLabel.setText("not found");
        } catch (InterruptedException e) {
            runJpylyzerLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }

    public void detectJhoveVersion(ActionEvent actionEvent) {
        detectJhoveVersionLabel.setText("checking jhove ...");
        try {

            CliCommand.Result output = new CliCommand("jhove --version").execute();
            output.print();
            //prvni radek STDOUT obsahuje text typu "Jhove (Rel. 1.6, 2011-01-04)"
            String version = output.getStdout();
            String firstLine = version.split("\n")[0];
            detectJhoveVersionLabel.setText(firstLine);
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            detectJhoveVersionLabel.setText("not found");
        } catch (InterruptedException e) {
            detectJhoveVersionLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }

    public void runJhove(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runJhoveLabel.setText("running jhove ...");
        try {
            CliCommand.Result output = new CliCommand("jhove -h XML -m jpeg2000-hul -k " + imageFile).execute();
            output.print();
            String outStr = output.getStdout();
            outStr = outStr.replace("\n", "");
            int length = Math.min(outStr.length(), 150);
            runJhoveLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            runJhoveLabel.setText("not found");
        } catch (InterruptedException e) {
            runJhoveLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }


    public void detectImageMagickVersion(ActionEvent actionEvent) {
        detectImageMagickVersionLabel.setText("checking imageMagick ...");
        try {
            CliCommand.Result output = new CliCommand("convert -version").execute();
            output.print();
            //prvni radek STDOUT obsahuje text typu " ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org"
            String version = output.getStdout();
            String firstLine = version.split("\n")[0];
            System.out.println(firstLine);
            detectImageMagickVersionLabel.setText(firstLine);
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            detectImageMagickVersionLabel.setText("not found");
        } catch (InterruptedException e) {
            detectImageMagickVersionLabel.setText("process interrupted");
            //e.printStackTrace();
        }
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

    public void runImageMagick(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runImageMagickLabel.setText("running imageMagick ...");
        try {
            CliCommand.Result output = new CliCommand("identify -verbose " + imageFile).execute();
            output.print();
            String outStr = output.getStdout();
            // TODO: 17.8.16 Sehnat priklady s chybama
            System.out.println(outStr);
            outStr = outStr.replace("\n", "");
            int length = Math.min(outStr.length(), 150);
            runImageMagickLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            runImageMagickLabel.setText("not found");
        } catch (InterruptedException e) {
            runImageMagickLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }


    public void detectKakaduVersion(ActionEvent actionEvent) {
        detectKakaduVersionLabel.setText("checking kakadu ...");
        try {
            //TODO: zjistovani verze poradne
            CliCommand.Result output = new CliCommand("kdu_expand").execute();
            output.print();
            /*String version = output.getStdout();
            detectKakaduVersionLabel.setText(version);*/
            detectKakaduVersionLabel.setText("Kakadu available, cannot determine version");
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            detectKakaduVersionLabel.setText("not available");
        } catch (InterruptedException e) {
            detectKakaduVersionLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }

    public void runKakadu(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runKakaduLabel.setText("running kakadu ...");
        try {
            // TODO: 18.8.16 kdu_expand nebyva na PATH
            CliCommand.Result output = new CliCommand("kdu_expand -i " + imageFile).execute();
            output.print();
            String outStr = output.getStdout().replace("\n", "");
            // TODO: 17.8.16 Sehnat priklady s chybama
            System.out.println(outStr);
            int length = Math.min(outStr.length(), 150);
            runKakaduLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            //e.printStackTrace();
            runKakaduLabel.setText("not found");
        } catch (InterruptedException e) {
            runKakaduLabel.setText("process interrupted");
            //e.printStackTrace();
        }
    }
}
