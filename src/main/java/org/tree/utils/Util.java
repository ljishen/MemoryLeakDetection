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

public class Util {
    public static final String MONKEY_RUNNER = "monkeyrunner.bat";
    public static final String ADB = "adb";
    public static final String SHELL = "shell";
    public static final String CONVERTED_HEAP_FILE_PREFIX = "converted-";

    private static final String HEAP_FILE_SUFFIX = ".hprof";

    public static CmdExecuteResultHandler executeCmdAsync(ExecuteWatchdog watchdog,
                                       String executable, String... arguments)
            throws IOException {
        CommandLine cmdLine = createCommandLine(executable, arguments);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Executor executor = createExecutor(out, err, watchdog);

        CmdExecuteResultHandler resultHandler = new CmdExecuteResultHandler(out, err);

        // TODO: use log4j instead, debug mode
        System.err.println("Execute: " + cmdLine);

        try {
            executor.execute(cmdLine, resultHandler);
        } catch (ExecuteException e) {
            // TODO: use log4j instead
            System.err.println(err.toString(Charsets.UTF_8.name()));

            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);

            throw e;
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
        System.err.println("Execute: " + cmdLine);

        try {
            int exitValue = executor.execute(cmdLine);

            String output = out.toString(Charsets.UTF_8.name());

            if (output.contains("Exception:") || StringUtils.containsIgnoreCase(output, "error:")) {
                throw new ExecuteException(output, exitValue);
            }
            return output;
        } catch (ExecuteException e) {
            // TODO: use log4j instead
            System.err.println(err.toString(Charsets.UTF_8.name()));

            throw e;
        } finally {
            out.close();
            err.close();
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

    public static File dumpheap(ApplicationInfo appInfo) throws IOException {
        String localHeapFilename = localHeapFilename(appInfo.pid());
        final String localHeapFileFolder = "/mnt/sdcard/Download/";
        String localHeapFilePath = localHeapFileFolder + localHeapFilename + HEAP_FILE_SUFFIX;

        boolean isEmulator = executeCmd(ADB, "devices").contains("emulator");
        if (isEmulator) {
            System.out.println("Emulator detected");
            executeCmd(ADB, SHELL, "rm", localHeapFileFolder + "*");
        } else {
            executeCmd(ADB, SHELL, "rm", localHeapFilePath);
        }

        // TODO: not working before Android 3.0?
        executeCmd(ADB, SHELL, "am", "dumpheap", appInfo.pid(), localHeapFilePath);

        // Wait until heap file created
        long lastFileSize;
        long fileSize = 0;
        final int checkIntervalInSeconds = 5;
        do {
            System.out.println("Check heap dump after " + checkIntervalInSeconds + " seconds...");
            try {
                Thread.sleep(checkIntervalInSeconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    public static String localHeapFilename(String pid) {
        return "memory_leak_analyzer_" + pid;
    }

    private static long fileSize(String filePath) throws IOException {
        String output = executeCmd(ADB, SHELL, "ls", "-s", filePath);

        String comp = StringUtils.substringBefore(output, " ");
        if (!StringUtils.isNumeric(comp)) {
            throw new IOException(output);
        }
        return Long.valueOf(StringUtils.substringBefore(output, " "));
    }
}
