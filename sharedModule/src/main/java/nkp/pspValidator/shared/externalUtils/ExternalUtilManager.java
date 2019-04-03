package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.externalUtils.UtilHandler.CommandData;
import nkp.pspValidator.shared.externalUtils.UtilHandler.Parser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Martin Řehánek on 14.11.16.
 */
public class ExternalUtilManager {

    private final Map<ExternalUtil, UtilHandler> utilVersionDetectionHandlers;
    private final Map<ExternalUtilExecution, UtilHandler> utilExecutionHandlers;
    private final Map<ExternalUtil, Boolean> utilAvaliable = new HashMap<>();


    public ExternalUtilManager(Map<ExternalUtil, UtilHandler> utilVersionDetectionHandlers, Map<ExternalUtilExecution, UtilHandler> utilExecutionHandlers) {
        this.utilVersionDetectionHandlers = utilVersionDetectionHandlers;
        this.utilExecutionHandlers = utilExecutionHandlers;
        for (ExternalUtil util : ExternalUtil.values()) {
            utilAvaliable.put(util, false);
        }
    }

    public boolean isVersionDetectionDefined(ExternalUtil type) {
        return utilVersionDetectionHandlers.containsKey(type);
    }

    public void setUtilAvailable(ExternalUtil util, boolean available) {
        utilAvaliable.put(util, available);
    }

    public boolean isUtilAvailable(ExternalUtil util) {
        return utilAvaliable.get(util);
    }

    public boolean isUtilExecutionDefined(ExternalUtilExecution execution) {
        return utilExecutionHandlers.containsKey(execution);
    }

    public void setPaths(Map<ExternalUtil, File> paths) {
        for (ExternalUtil util : paths.keySet()) {
            File path = paths.get(util);
            setPath(util, path);
        }
    }

    public void setPath(ExternalUtil util, File path) {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(util);
        if (versionDetection != null) {
            versionDetection.getCommandData().setPath(path);
        }
        for (ExternalUtilExecution execution : utilExecutionHandlers.keySet()) {
            if (execution.getUtil() == util) {
                utilExecutionHandlers.get(execution).getCommandData().setPath(path);
            }
        }
    }

    public String runUtilVersionDetection(ExternalUtil util) throws CliCommand.CliCommandException {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(util);
        String[] command = buildCommand(versionDetection.getCommandData(), null);
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
                throw new IllegalStateException("unexpected stream " + stream);
        }
        if (rawOutput != null && !rawOutput.isEmpty()) {
            String parsed = parseData(rawOutput, parser);
            if (parsed == null || parsed.isEmpty()) {
                System.out.println("'" + rawOutput + "'");

                throw new CliCommand.CliCommandException("verze nenalezena ve výstupu: " + rawOutput);
            } else {
                return parsed.trim();
            }
        } else {
            throw new CliCommand.CliCommandException("prázdný výstup");
        }
    }

    public String runUtilExecution(ExternalUtilExecution utilExecution, File targetFile) throws CliCommand.CliCommandException {
        UtilHandler utilHandler = utilExecutionHandlers.get(utilExecution);
        String[] command = buildCommand(utilHandler.getCommandData(), targetFile);
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
                throw new IllegalStateException("unexpected stream " + stream);
        }
        if (rawOutput != null) {
            String parsed = parseData(rawOutput, utilHandler.getParser());
            return parsed == null || parsed.isEmpty() ? "" : parsed.trim();
        } else {
            throw new CliCommand.CliCommandException("chybí výstup");
        }
    }

    private String[] buildCommand(CommandData commandData, File targetFile) {
        String utilPath = commandData.getPath() == null ? "" : commandData.getPath().getAbsolutePath();
        if (!utilPath.isEmpty() && !utilPath.endsWith(File.separator)) {
            utilPath = utilPath + File.separatorChar;
        }

        String[] result = commandData.getRawCommand().split("\\s");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].replace("${UTIL_PATH}", utilPath);
            if (targetFile != null) {
                result[i] = result[i].replace("${TARGET_FILE}", targetFile.getAbsolutePath());
            }
        }
        return result;
    }

    private String parseData(String rawOutput, Parser parser) {
        if (parser.getRegexp() == null) {
            //System.err.println("regexp is null");
            return rawOutput;
        } else {
            //System.out.println("raw output: " + rawOutput);
            Matcher m = Pattern.compile(parser.getRegexp()).matcher(rawOutput);
            if (m.find()) {
                //first appearance
                String result = m.group(0);
                //System.err.println("match: "  + result);
                return result;
            } else {
                //System.err.println("no match");
                return null;
            }
        }
    }


}
