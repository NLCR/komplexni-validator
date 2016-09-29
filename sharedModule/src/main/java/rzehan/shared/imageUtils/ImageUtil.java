package rzehan.shared.imageUtils;

import java.util.HashMap;
import java.util.Map;

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
        private final String platform;
        private final Execution execution;
        private final Parsing parsing;

        public VersionDetection(String platform, Execution execution, Parsing parsing) {
            this.platform = platform;
            this.execution = execution;
            this.parsing = parsing;
        }

        public String getPlatform() {
            return platform;
        }

        public Execution getExecution() {
            return execution;
        }

        public Parsing getParsing() {
            return parsing;
        }
    }

    public static class Installation {
        private final String platform;
        private final Execution execution;

        public Installation(String platform, Execution execution) {
            this.platform = platform;
            this.execution = execution;
        }

        public String getPlatform() {
            return platform;
        }

        public Execution getExecution() {
            return execution;
        }

    }

    public static class Run {
        private final String platform;
        private final Execution execution;
        private final Parsing parsing;

        public Run(String platform, Execution execution, Parsing parsing) {
            this.platform = platform;
            this.execution = execution;
            this.parsing = parsing;
        }

        public String getPlatform() {
            return platform;
        }

        public Execution getExecution() {
            return execution;
        }

        public Parsing getParsing() {
            return parsing;
        }
    }

    private final String name;
    private final Map<String, VersionDetection> versionDetectionByPlatform = new HashMap<>();
    private final Map<String, Installation> installationByPlatform = new HashMap<>();
    private final Map<String, VersionDetection> runByPlatform = new HashMap<>();

    public ImageUtil(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, VersionDetection> getVersionDetectionByPlatform() {
        return versionDetectionByPlatform;
    }

    public Map<String, Installation> getInstallationByPlatform() {
        return installationByPlatform;
    }

    public Map<String, VersionDetection> getRunByPlatform() {
        return runByPlatform;
    }
}
