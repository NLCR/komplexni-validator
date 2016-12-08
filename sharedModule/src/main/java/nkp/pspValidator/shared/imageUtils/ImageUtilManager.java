package nkp.pspValidator.shared.imageUtils;

import nkp.pspValidator.shared.imageUtils.UtilHandler.Command;
import nkp.pspValidator.shared.imageUtils.UtilHandler.Parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by martin on 14.11.16.
 */
public class ImageUtilManager {

    private final Map<ImageUtil, UtilHandler> utilVersionDetectionHandlers;
    private final Map<ImageUtil, UtilHandler> utilExecutionHandlers;
    private final Map<ImageUtil, Boolean> utilAvaliable = new HashMap<>();


    public ImageUtilManager(Map<ImageUtil, UtilHandler> utilVersionDetectionHandlers, Map<ImageUtil, UtilHandler> utilExecutionHandlers) {
        this.utilVersionDetectionHandlers = utilVersionDetectionHandlers;
        this.utilExecutionHandlers = utilExecutionHandlers;
        for (ImageUtil util : ImageUtil.values()) {
            utilAvaliable.put(util, false);
        }
    }

    public boolean isVersionDetectionDefined(ImageUtil type) {
        return utilVersionDetectionHandlers.containsKey(type);
    }

    public boolean isUtilExecutionDefined(ImageUtil type) {
        return utilVersionDetectionHandlers.containsKey(type);
    }

    public void setPaths(Map<ImageUtil, File> paths) {
        for (ImageUtil util : paths.keySet()) {
            File path = paths.get(util);
            setPath(util, path);
        }
    }

    public void setPath(ImageUtil util, File path) {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(util);
        if (versionDetection != null) {
            versionDetection.getCommand().setPath(path);
        }
        UtilHandler UtilHandler = utilExecutionHandlers.get(util);
        if (UtilHandler != null) {
            UtilHandler.getCommand().setPath(path);
        }
    }


    public void setUtilAvailable(ImageUtil util, boolean available) {
        utilAvaliable.put(util, available);
    }

    public boolean isUtilAvailable(ImageUtil util) {
        return utilAvaliable.get(util);
    }

    public String runUtilVersionDetection(ImageUtil type) throws IOException, InterruptedException {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(type);
        String command = buildCommand(versionDetection.getCommand());
        CliCommand.Result result = new CliCommand(command).execute();
        String rawOutput = null;
        Parser parser = versionDetection.getParser();
        Stream stream = parser.getStream();
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
        if (rawOutput != null) {
            String parsed = parseData(rawOutput, parser);
            return parsed == null || parsed.isEmpty() ? null : parsed.trim();
        } else {
            return null;
        }
    }

    private String buildCommand(Command command) {
        String path = command.getPath() == null ? "" : command.getPath().getAbsolutePath();
        String result = command.getRawCommand();
        if (!path.isEmpty()) {
            path = path + File.separatorChar;
        }
        result = result.replace("${PATH}", path);
        return result;
    }

    private String buildCommand(Command command, File imageFile) {
        String path = command.getPath() == null ? "" : command.getPath().getAbsolutePath();
        String result = command.getRawCommand();
        if (!path.isEmpty()) {
            path = path + File.separatorChar;
        }
        result = result.replace("${PATH}", path);
        result = result.replace("${IMAGE_FILE}", imageFile.getAbsolutePath());
        return result;
    }

    private String parseData(String rawOutput, Parser parser) {
        if (parser.getRegexp() == null) {
            //System.err.println("regexp is null");
            return rawOutput;
        } else {
            Matcher m = Pattern.compile(parser.getRegexp()).matcher(rawOutput);
            if (m.find()) {
                //first appearance
                String result = m.group(0);
                //System.err.println("match: "  + result);
                return result;
            } else {
                //System.err.println("no match");
                // TODO: 29.9.16 not found
                return null;
            }
        }
    }

    public String runUtilExecution(ImageUtil utilType, File imageFile) throws IOException, InterruptedException {
        UtilHandler utilHandler = utilExecutionHandlers.get(utilType);
        //TODO: poresit, NPE pro getCommand, kdyz neni definovano
        String command = buildCommand(utilHandler.getCommand(), imageFile);
        CliCommand.Result result = new CliCommand(command).execute();
        String rawOutput = null;
        Stream stream = utilHandler.getParser().getStream();
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
        if (rawOutput != null) {
            String parsed = parseData(rawOutput, utilHandler.getParser());
            return parsed == null || parsed.isEmpty() ? null : parsed.trim();
        } else {
            return null;
        }
    }
}
