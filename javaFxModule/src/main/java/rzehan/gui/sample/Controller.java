package rzehan.gui.sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import rzehan.shared.Os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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


    private final Os os;

    public Controller() {
        os = Os.detectOs();
        LOGGER.info("os: " + os.toString());
    }

    public void initialize() {
        osLabel.setText(os.toReadableString());
    }


    private CmlCommandResult executeCliCommand(String command) throws IOException, InterruptedException {
        //https://gist.github.com/Lammerink/3926636
        Process pr = Runtime.getRuntime().exec(command);


        //read standard error stream
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
        StringBuilder stderrBuilder = new StringBuilder();
        if (stderrReader != null) {
            String line;
            while ((line = stderrReader.readLine()) != null) {
                stderrBuilder.append(line).append('\n');
            }
            stderrReader.close();
        }

        //read standard output stream
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        StringBuilder stdoutBuilder = new StringBuilder();
        if (stdoutReader != null) {
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                stdoutBuilder.append(line).append('\n');
            }
            stdoutReader.close();
        }

        int exitValue = pr.waitFor();
        return new CmlCommandResult(exitValue, stdoutBuilder.toString(), stderrBuilder.toString());
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
            CmlCommandResult output = executeCliCommand("jpylyzer --version");
            output.print();
            //jediny radek STDERR obsahuje verzi typu "1.17.0"
            detectJpylyzerVersionLabel.setText(output.getStderr());
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            detectJpylyzerVersionLabel.setText("not found");
        } catch (InterruptedException e) {
            detectJpylyzerVersionLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }

    public void runJpylyzer(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runJpylyzerLabel.setText("running jplyzer ...");
        try {
            CmlCommandResult output = executeCliCommand("jpylyzer jp2In " + imageFile);
            output.print();
            //v STDERR nesmyslna hlaska "jp2In does not exist"
            String outStr = output.getStdout();
            outStr = outStr.replace("\n", "");
            int length = Math.min(outStr.length(), MAX_OUTPUT_LENGTH);
            runJpylyzerLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            runJpylyzerLabel.setText("not found");
        } catch (InterruptedException e) {
            runJpylyzerLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }

    public void detectJhoveVersion(ActionEvent actionEvent) {
        detectJhoveVersionLabel.setText("checking jhove ...");
        try {
            CmlCommandResult output = executeCliCommand("jhove --version");
            output.print();
            //prvni radek STDOUT obsahuje text typu "Jhove (Rel. 1.6, 2011-01-04)"
            String version = output.getStdout();
            String firstLine = version.split("\n")[0];
            detectJhoveVersionLabel.setText(firstLine);
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            detectJhoveVersionLabel.setText("not found");
        } catch (InterruptedException e) {
            detectJhoveVersionLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }

    public void runJhove(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runJhoveLabel.setText("running jhove ...");
        try {
            CmlCommandResult output = executeCliCommand("jhove -h XML -m jpeg2000-hul -k " + imageFile);
            output.print();
            String outStr = output.getStdout();
            outStr = outStr.replace("\n", "");
            int length = Math.min(outStr.length(), 150);
            runJhoveLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            runJhoveLabel.setText("not found");
        } catch (InterruptedException e) {
            runJhoveLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }


    public void detectImageMagickVersion(ActionEvent actionEvent) {
        detectImageMagickVersionLabel.setText("checking magick ...");
        try {
            CmlCommandResult output = executeCliCommand("convert -version");
            output.print();
            //prvni radek STDOUT obsahuje text typu " ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org"
            String version = output.getStdout();
            String firstLine = version.split("\n")[0];
            detectImageMagickVersionLabel.setText(firstLine);
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            detectImageMagickVersionLabel.setText("not found");
        } catch (InterruptedException e) {
            detectImageMagickVersionLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }

    public void installImageMagick(ActionEvent actionEvent) {
        try {
            //String outStr = executeCliCommand("sudo aptitude search imageMagick");
            //String outStr = executeCliCommand("totem");
            //String outStr = executeCliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/pomodoro/Pomodoro");


            //URL url = getClass().getResource("res/bin");
            //LOGGER.info(url.getFile());

            //String outStr = executeCliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/pomodoro/Pomodoro");
            //String outStr = executeCliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/imagemagick.deb");
            String dir = System.getProperty("user.dir");
            //String file = dir + File.separator + "res" + File.separator + "bin"
            String file = dir + "\\res\\bin\\ImageMagick-7.0.2-4-Q16-x64-dll.exe";

            //String outStr = executeCliCommand("/home/martin/IdeaProjects/NkpValidator/res/bin/fuckyou.sh");
            CmlCommandResult output = executeCliCommand(file);
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
            e.printStackTrace();
        } catch (InterruptedException e) {
            //detectImageMagickVersionLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }

    public void runImageMagick(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runImageMagickLabel.setText("running imageMagick ...");
        try {
            CmlCommandResult output = executeCliCommand("identify -verbose " + imageFile);
            output.print();
            String outStr = output.getStdout();
            // TODO: 17.8.16 Sehnat priklady s chybama
            System.out.println(outStr);
            outStr = outStr.replace("\n", "");
            int length = Math.min(outStr.length(), 150);
            runImageMagickLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            runImageMagickLabel.setText("not found");
        } catch (InterruptedException e) {
            runImageMagickLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }


    public void detectKakaduVersion(ActionEvent actionEvent) {
        detectKakaduVersionLabel.setText("checking kakadu ...");
        try {
            //TODO: zjistovani verze poradne
            CmlCommandResult output = executeCliCommand("kdu_expand");
            output.print();
            /*String version = output.getStdout();
            detectKakaduVersionLabel.setText(version);*/
            detectKakaduVersionLabel.setText("Kakadu available, cannot determine version");
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            detectKakaduVersionLabel.setText("not available");
        } catch (InterruptedException e) {
            detectKakaduVersionLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }

    public void runKakadu(ActionEvent actionEvent) {
        String imageFile = MC_FILE;
        runKakaduLabel.setText("running kakadu ...");
        try {
            // TODO: 18.8.16 kdu_expand nebyva na PATH
            CmlCommandResult output = executeCliCommand("kdu_expand -i " + imageFile);
            output.print();
            String outStr = output.getStdout().replace("\n", "");
            // TODO: 17.8.16 Sehnat priklady s chybama
            System.out.println(outStr);
            int length = Math.min(outStr.length(), 150);
            runKakaduLabel.setText(outStr.substring(0, length) + " ...");
        } catch (IOException e) {
            //program probably does not exist
            e.printStackTrace();
            runKakaduLabel.setText("not found");
        } catch (InterruptedException e) {
            runKakaduLabel.setText("process interrupted");
            e.printStackTrace();
        }
    }
}
