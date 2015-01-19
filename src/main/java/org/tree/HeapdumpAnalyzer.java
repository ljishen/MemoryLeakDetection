package org.tree;

import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.tree.model.ClassInstanceCount;
import org.tree.utils.CmdExecuteResultHandler;
import org.tree.utils.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeapdumpAnalyzer {
    private static final Pattern CLASS_INSTANCE_COUNT = Pattern.compile("^(\\d+).+>(\\d+).+class ([\\.\\w\\d\\$]+)", Pattern.MULTILINE);

    private String packageName;
    private String hprof;
    private String baselineHprof;

    public HeapdumpAnalyzer(String packageName, String hprof, String baselineHprof) {
        this.packageName = packageName;
        this.hprof = hprof;
        this.baselineHprof = baselineHprof;
    }

    public void show() throws IOException {
        ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        HttpURLConnection connection = null;
        InputStream is = null;

        String result;
        CmdExecuteResultHandler executeResultHandler = null;

        try {
            executeResultHandler = Util.executeCmdAsync(watchdog, "jhat", "-baseline", baselineHprof, hprof);
            System.out.println("Parsing HPROF Result By jhat...");

            connection = (HttpURLConnection) new URL("http://127.0.0.1:7000/showInstanceCounts/").openConnection();

            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("GET");

            is = connection.getInputStream();

            System.out.println("Downloading Instance Counts for All Classes...");
            result = IOUtils.toString(is, Charsets.UTF_8);
        } catch(IOException e) {
            if (executeResultHandler != null && executeResultHandler.hasResult()) {
                System.err.println(executeResultHandler.getOutput());
            }
            throw e;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.close(connection);
            watchdog.destroyProcess();
        }

        List<ClassInstanceCount> cics = new ArrayList<ClassInstanceCount>();
        Matcher m = CLASS_INSTANCE_COUNT.matcher(result);

        if (m.find()) {
            final int leakThreshold = MemoryLeakAnalyzer.PLAY_BACK_LOOPS / 3;
            System.out.println("leakThreshold " + leakThreshold);

            System.out.println("========= Leak Suspect Classes =========");
            do {
                int newCount = Integer.valueOf(m.group(2));
                String className = m.group(3);

                // Only filter new created instances > increaseThreshold AND className start with package name.
                if (newCount > leakThreshold && className.startsWith(packageName)) {
                    ClassInstanceCount cic = new ClassInstanceCount(Integer.valueOf(m.group(1)), newCount, className);
                    cics.add(cic);
                    System.out.println(cic);
                }
            } while (m.find());
        } else {
            System.err.println("Invalid Clsass Instances Result");
        }
    }

    // XXX: Only for test
    public static void main(String[] args) {
        try {
            new HeapdumpAnalyzer("",
                    "C:\\Users\\Libram\\AppData\\Local\\Temp\\converted-memory_leak_analyzer_1466366299153852855787.hprof",
                    "C:\\Users\\Libram\\AppData\\Local\\Temp\\converted-memory_leak_analyzer_14668789654037508480417.hprof").show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
