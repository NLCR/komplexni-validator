package rzehan.gui.sample;

/**
 * Created by Martin Řehánek on 17.8.16.
 */
public class CmlCommandResult {
    private final int exitValue;
    private final String stdout;
    private final String stderr;

    public CmlCommandResult(int exitValue, String stdout, String stderr) {
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
