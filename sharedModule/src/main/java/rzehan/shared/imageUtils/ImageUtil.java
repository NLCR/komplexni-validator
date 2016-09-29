package rzehan.shared.imageUtils;

import rzehan.shared.OperatingSystem;
import rzehan.shared.Platform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Martin Řehánek on 29.9.16.
 */
public class ImageUtil {

    public static class Execution {
        private final String path;
        private final String command;

        public Execution(String path, String command) {
            this.path = path;
            this.command = command;
        }

        public String getPath() {
            return path;
        }

        public String getCommand() {
            return command;
        }
    }

    public static class Parsing {
        private final Stream stream;
        private final String regexp;

        public Parsing(Stream stream, String regexp) {
            this.stream = stream;
            this.regexp = regexp;
        }

        public Stream getStream() {
            return stream;
        }

        public String getRegexp() {
            return regexp;
        }
    }

    public static class VersionDetection {
        // TODO: 29.9.16 replace with OperatingSystem object
        private final OperatingSystem operatingSystem;
        private final Execution execution;
        private final Parsing parsing;

        public VersionDetection(OperatingSystem operatingSystem, Execution execution, Parsing parsing) {
            this.operatingSystem = operatingSystem;
            this.execution = execution;
            this.parsing = parsing;
        }

        public OperatingSystem getOperatingSystem() {
            return operatingSystem;
        }

        public Execution getExecution() {
            return execution;
        }

        public Parsing getParsing() {
            return parsing;
        }
    }

    public static class Run {
        private final OperatingSystem operatingSystem;
        private final Execution execution;
        private final Parsing parsing;

        public Run(OperatingSystem operatingSystem, Execution execution, Parsing parsing) {
            this.operatingSystem = operatingSystem;
            this.execution = execution;
            this.parsing = parsing;
        }

        public OperatingSystem getOperatingSystem() {
            return operatingSystem;
        }

        public Execution getExecution() {
            return execution;
        }

        public Parsing getParsing() {
            return parsing;
        }
    }

    private final String name;
    private final Map<OperatingSystem, VersionDetection> versionDetectionByPlatform = new HashMap<>();
    private final Map<OperatingSystem, Run> runByPlatform = new HashMap<>();

    public ImageUtil(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<OperatingSystem, VersionDetection> getVersionDetectionByPlatform() {
        return versionDetectionByPlatform;
    }

    public Map<OperatingSystem, Run> getRunByPlatform() {
        return runByPlatform;
    }

    public String runVersionDetection(Platform platform) throws IOException, InterruptedException {
        VersionDetection versionDetection = versionDetectionByPlatform.get(platform.getOperatingSystem());
        String command = constructCommand(versionDetection.getExecution());
        CliCommand.Result result = new CliCommand(command).execute();
        String rawOutput = null;
        Stream stream = versionDetection.getParsing().getStream();
        switch (stream) {
            case STDERR:
                rawOutput = result.getStderr();
                break;
            case STDOUT:
                rawOutput = result.getStdout();
                break;
            default:
                throw new IOException(String.format("empty response from '%s' (%s)", command, stream));
        }
        String parsed = parseData(rawOutput, versionDetection.getParsing());
        return parsed == null || parsed.isEmpty() ? rawOutput.trim() : parsed.trim();
    }

    private String constructCommand(Execution execution) {
        String path = execution.getPath();
        return path != null ?
                path + File.separator + execution.getCommand() :
                execution.getCommand();
    }


    private String constructCommand(Execution execution, String imageFile) {
        String path = execution.getPath();
        String command = path != null ? path + File.separator + execution.getCommand() :
                execution.getCommand();
        command = command.replace("${IMAGE_FILE}", imageFile);
        System.out.println(command);
        return command;
    }

    private String parseData(String rawOutput, Parsing parsing) {
        if (parsing.getRegexp() == null) {
            return rawOutput;
        } else {
            Matcher m = Pattern.compile(parsing.getRegexp()).matcher(rawOutput);
            if (m.find()) {
                //first appearance
                return m.group(0);
            } else {
                // TODO: 29.9.16 not found
                return null;
            }
        }
    }


    public String runUtil(Platform platform, String imageFile) throws IOException, InterruptedException {
        Run run = runByPlatform.get(platform.getOperatingSystem());
        String command = constructCommand(run.getExecution(), imageFile);
        CliCommand.Result result = new CliCommand(command).execute();
        String rawOutput = null;
        Stream stream = run.getParsing().getStream();
        switch (stream) {
            case STDERR:
                rawOutput = result.getStderr();
                break;
            case STDOUT:
                rawOutput = result.getStdout();
                break;
            default:
                throw new IOException(String.format("empty response from '%s' (%s)", command, stream));
        }
        String parsed = parseData(rawOutput, run.getParsing());
        return parsed == null || parsed.isEmpty() ? rawOutput.trim() : parsed.trim();
    }


}
