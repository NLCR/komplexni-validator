package nkp.pspValidator.shared.externalUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Created by Martin Řehánek on 29.9.16.
 */
public class CliCommand {

    private final String[] command;

    public CliCommand(String[] command) {
        this.command = command;
    }

    public Result execute() throws CliCommandException {
        try {
            //System.out.println("command: " + this);
            Process process = Runtime.getRuntime().exec(command);
            //TODO: perhaps thread pooling here

            //read standard error stream in separate thread
            StringBuilder stderrBuilder = new StringBuilder();
            Thread errThread = new Thread(() -> {
                try {
                    BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        stderrBuilder.append(line);
                        stderrBuilder.append('\n');
                    }
                    stderrReader.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                    //nothing, only main thread running program itself shoud propagate exception
                }
            });

            //read standard output stream in separate thread
            StringBuilder stdoutBuilder = new StringBuilder();
            Thread outThread = new Thread(() -> {
                try {
                    BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        stdoutBuilder.append(line);
                        stdoutBuilder.append('\n');
                        //System.err.println(line);
                    }
                    stdoutReader.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                    //nothing, only main thread running program itself shoud propagate exception
                }
            });
            //start threads to read output streams
            outThread.start();
            errThread.start();

            //wait for all 3 threads to finish
            int exitValue = process.waitFor();
            outThread.join();
            errThread.join();
            String stdOut = stdoutBuilder.toString();
            String stdErr = stderrBuilder.toString();
            /*System.err.println("SOUT: " + stdOut);
            System.err.println("SERR: " + stdErr);*/
            Result result = new Result(exitValue, stdOut, stdErr);
            //result.print();
            return result;
        } catch (Throwable e) {
            //e.printStackTrace();
            //throw e;
            //return null;
            //return new Result(-1,null, e.getMessage());
            throw new CliCommandException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "CliCommand{" +
                "command=" + Arrays.toString(command) +
                '}';
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
            System.out.println("SERR: " + stderr);
            System.out.println("SOUT: " + stdout);
        }
    }

    public static class CliCommandException extends Exception {
        public CliCommandException(String message) {
            super(message);
        }

        public CliCommandException(String message, Throwable cause) {
            super(message, cause);
        }
    }


}
