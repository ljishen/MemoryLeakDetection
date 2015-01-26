package org.tree.utils;

import org.apache.commons.exec.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.tree.model.ApplicationInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static final String MONKEY_RUNNER = "monkeyrunner.bat";
    public static final String ADB = "adb";
    public static final String SHELL = "shell";

    private static final String CONVERTED_HEAP_FILE_PREFIX = "converted-";
    private static final String LOCAL_HEAP_FILE_PREFIX = "memory_leak_analyzer_";
    private static final String HEAP_FILE_SUFFIX = ".hprof";

    private static final String PACKAGE_NAME = "([\\.\\w\\d]+)";
    private static final Pattern PACKAGE_NAME_P1 = Pattern.compile("Recent #0.+?=" + PACKAGE_NAME);
    private static final Pattern PACKAGE_NAME_P2 = Pattern.compile("Recent #0.+?\\{.+? .+? .+? " + PACKAGE_NAME);

    private static final Pattern PID = Pattern.compile("\\n.+?\\s(\\d+)");
    private static final Pattern DEVICE_INFO =
            Pattern.compile("List of devices attached[\\s]+([\\-\\d\\w+]+)\\s+device\\s?([^\\r\\n]*)");


    public static CmdExecuteResultHandler executeCmdAsync(ExecuteWatchdog watchdog,
                                       String executable, String... arguments)
            throws IOException {
        CommandLine cmdLine = createCommandLine(executable, arguments);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Executor executor = createExecutor(out, err, watchdog);

        CmdExecuteResultHandler resultHandler = new CmdExecuteResultHandler(out, err);

        // TODO: use log4j instead, debug mode
        System.out.println("Execute: " + cmdLine);

        try {
            executor.execute(cmdLine, resultHandler);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);
        }
        return resultHandler;
    }

    private static Executor createExecutor(ByteArrayOutputStream out,
                                           ByteArrayOutputStream err,
                                           ExecuteWatchdog watchdog) {
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(out, err));
        executor.setWatchdog(watchdog);
        return executor;
    }

    private static CommandLine createCommandLine(String executable, String... arguments) {
        CommandLine cmdLine = new CommandLine(executable);
        if (arguments != null) {
            for (String arg : arguments) {
                cmdLine.addArgument(arg);
            }
        }
        return cmdLine;
    }

    public static String executeCmd(String executable, String... arguments) throws IOException {
        CommandLine cmdLine = createCommandLine(executable, arguments);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Executor executor = createExecutor(out, err, null);

        // TODO: use log4j instead, debug mode
        System.out.println("Execute: " + cmdLine);

        try {
            int exitValue = executor.execute(cmdLine);

            String output = out.toString(Charsets.UTF_8.name());
            if (output.contains("Exception:") || StringUtils.containsIgnoreCase(output, "error:")) {
                throw new ExecuteException("Standard Output Contains Error", exitValue);
            }
            return output;
        } catch (IOException e) {
            // TODO: use log4j instead
            System.err.println(out.toString(Charsets.UTF_8.name()));
            System.err.println(err.toString(Charsets.UTF_8.name()));

            throw e;
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);
        }
    }

    public static String getResourcePath(Class<?> clazz, String name) throws IllegalArgumentException {
        URL url = clazz.getClassLoader().getResource(name);
        if (url != null) {
            return url.getPath();
        } else {
            throw new IllegalArgumentException("Resource " + name + " does not exist!");
        }
    }

    public static File dumpheap(ApplicationInfo appInfo) throws IOException, InterruptedException {
        String localHeapFilename = localHeapFilename(appInfo.getPid());
        final String localHeapFileFolder = "/mnt/sdcard/Download/";
        String localHeapFilePath = localHeapFileFolder + localHeapFilename + HEAP_FILE_SUFFIX;

        executeCmd(ADB,
                SHELL,
                "rm",
                localHeapFileFolder +
                        (appInfo.deviceSerialNumber().startsWith("emulator") ? "" : LOCAL_HEAP_FILE_PREFIX) +
                        "*");

        // TODO: not working before Android 3.0?
        executeCmd(ADB, SHELL, "am", "dumpheap", appInfo.getPid(), localHeapFilePath);

        // Wait until heap file created
        long lastFileSize;
        long fileSize = 0;
        final int checkIntervalInSeconds = 6;
        do {
            System.out.println("Check heap dump after " + checkIntervalInSeconds + " seconds...");
            Thread.sleep(TimeUnit.SECONDS.toMillis(checkIntervalInSeconds));
            lastFileSize = fileSize;
            fileSize = fileSize(localHeapFilePath);
        } while (lastFileSize != fileSize);

        System.out.println("Heap dump size: " + fileSize + " KB");

        if (fileSize == 0) {
            throw new IOException("Fail to dump heap for app: " + appInfo);
        }

        File rawHeapFile = File.createTempFile(localHeapFilename, HEAP_FILE_SUFFIX);
        executeCmd(ADB, "pull", localHeapFilePath, rawHeapFile.getPath());

        // Convert the .hprof file from the Dalvik format to the J2SE HPROF
        // (Eclipse Memory Analyzer (MAT) compatible)format
        File convertedHeapFile = new File(rawHeapFile.getParent(), CONVERTED_HEAP_FILE_PREFIX + rawHeapFile.getName());
        executeCmd("hprof-conv", rawHeapFile.getPath(), convertedHeapFile.getPath());

        System.out.println("HPROF file created at: " + convertedHeapFile);
        return convertedHeapFile;
    }

    private static String localHeapFilename(String pid) {
        return LOCAL_HEAP_FILE_PREFIX + pid + "_";
    }

    public static String convertedHeapFilePrefix(String pid) {
        return CONVERTED_HEAP_FILE_PREFIX + localHeapFilename(pid);
    }

    private static long fileSize(String filePath) throws IOException {
        String output = executeCmd(ADB, SHELL, "ls", "-s", filePath);

        String comp = StringUtils.substringBefore(output, " ");
        if (!StringUtils.isNumeric(comp)) {
            throw new IOException(output);
        }
        return Long.valueOf(StringUtils.substringBefore(output, " "));
    }

    public static ApplicationInfo getForegroundAppInfo() throws IOException {
        ApplicationInfo appInfo = null;
        String devicesInfo = executeCmd(ADB, "devices", "-l");
        Matcher m = DEVICE_INFO.matcher(devicesInfo);
        if (m.find()) {
            appInfo = new ApplicationInfo(m.group(1), m.group(2));
            System.out.println("Select device " + appInfo.deviceSerialNumber());
        } else {
            throw new IOException("No device online!");
        }

        String activitiesInfo = Util.executeCmd(
                Util.ADB,
                "-s",
                appInfo.deviceSerialNumber(),
                Util.SHELL,
                "dumpsys",
                "activity",
                "activities");

        m = PACKAGE_NAME_P1.matcher(activitiesInfo);
        if (!m.find()) {
            m = PACKAGE_NAME_P2.matcher(activitiesInfo);
            if (!m.find()) {
                throw new IOException("Cannot Found Foreground Package Name!");
            }
        }

        appInfo.setPackageName(m.group(1));
        int length = appInfo.getPackageName().length();

        // In command "adb shell ps [package]", package name can only be matched with its last 15 characters.
        String packageAbbreviation = length > 15 ?
                appInfo.getPackageName().substring(length - 15, length) : appInfo.getPackageName();

        String pidInfo = Util.executeCmd(
                Util.ADB,
                "-s",
                appInfo.deviceSerialNumber(),
                Util.SHELL,
                "ps",
                packageAbbreviation);
        m = PID.matcher(pidInfo);

        if (!m.find()) {
            throw new IOException("Cannot Found Foreground App PID!");
        }
        appInfo.setPid(m.group(1));

        // TODO: use log4j inform mode instead
        System.out.println(appInfo);

        return appInfo;
    }

    // XXX: Only for test
    public static void main(String[] args) {
        try {
            dumpheap(getForegroundAppInfo());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
