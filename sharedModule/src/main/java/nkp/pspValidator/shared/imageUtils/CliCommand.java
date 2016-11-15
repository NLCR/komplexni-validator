package nkp.pspValidator.shared.imageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Martin Řehánek on 29.9.16.
 */
public class CliCommand {

    private final String command;

    public CliCommand(String command) {
        this.command = command;
    }

    public Result execute() throws IOException, InterruptedException {
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
        return new Result(exitValue, stdoutBuilder.toString(), stderrBuilder.toString());
    }

    public static class Result {
        private final int exitValue;
        private final String stdout;
        private final String stderr;

        public Result(int exitValue, String stdout, String stderr) {
            this.exitValue = exitValue;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitValue() {
            return exitValue;
        }

        public void print() {
            System.out.println("EXIT VALUE: " + exitValue + (exitValue == 0 ? " (ok)" : " (error)"));
            System.err.println("SERR: " + stderr);
            System.out.println("SOUT: " + stdout);
        }
    }


}
