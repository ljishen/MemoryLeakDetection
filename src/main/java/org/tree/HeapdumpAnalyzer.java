package org.tree;

import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.tree.model.ApplicationInfo;
import org.tree.utils.CmdExecuteResultHandler;
import org.tree.utils.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeapdumpAnalyzer {
    private static final Pattern CLASS_INSTANCE_COUNT = Pattern.compile("(\\d+).+class ([\\.\\w\\d\\$\\[\\]]+)");
    private static final int MINIMUM_COMPARISONS = 4;

    private ApplicationInfo appInfo;
    private List<File> hprofs;

    public HeapdumpAnalyzer(ApplicationInfo appInfo, File hprofFolder) {
        this.appInfo = appInfo;

        hprofs = new ArrayList<File>(
                FileUtils.listFiles(
                        hprofFolder,
                        new PrefixFileFilter(Util.convertedHeapFilePrefix(appInfo.getPid())),
                        null));

        if (hprofs.size() < MINIMUM_COMPARISONS) {
            throw new IllegalArgumentException("Require at lease " + MINIMUM_COMPARISONS + " HPROF files to analyze!");
        }

        // newer file first
        Collections.sort(hprofs, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if ((o1).lastModified() > (o2).lastModified()) {
                    return -1;
                } else if ((o1).lastModified() < (o2).lastModified()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        System.out.println("Found " + hprofs.size() + " HPROF files");
    }

    public void analyze() throws IOException, InterruptedException {
        Map<String, List<Integer>> classInstanceCounts = collectClassInstanceCounts();

        System.out.println("\n========= Leak Suspect Classes (Insufficient\tClass\tInstance_Counts\tIncrease_Ratio\tMin_Ratio) =========");

        for (Map.Entry<String, List<Integer>> e : classInstanceCounts.entrySet()) {
            List<Integer> counts = e.getValue();
            if (counts.size() < MINIMUM_COMPARISONS) {
//                System.out.println("T\t" + e.getKey() + "\t" + counts + "\t[]\t[]");
                continue;
            }

            List<Float> increaseRatio = new ArrayList<Float>(counts.size() - 1);

            float minRatio = Integer.MAX_VALUE;
            int minRatioIndex = Integer.MAX_VALUE;
            for (int i = 1;  i < counts.size(); i++) {
                float delta;
                int current = counts.get(i);
                delta = current == 0 ? 0 : (1.0f * counts.get(i - 1) / current);
                increaseRatio.add(delta);

                if (delta < minRatio) {
                    minRatio = delta;
                    minRatioIndex = increaseRatio.size() - 1;
                }
            }

            if (minRatio < Math.pow(10, -5)) {
                increaseRatio.remove(minRatioIndex);
            }

            if (Collections.min(increaseRatio) > 1) {
                System.out.println("F\t" + e.getKey() + "\t" + counts + "\t" + increaseRatio + "\t" + minRatio);
            } else {
//                System.out.println("G\t" + e.getKey() + "\t" + counts + "\t" + increaseRatio + "\t" + minRatio);
            }
        }
    }

    private Map<String, List<Integer>> collectClassInstanceCounts() throws InterruptedException, IOException {
        Map<String, List<Integer>> classInstanceCounts = new HashMap<String, List<Integer>>();

        HttpURLConnection connection = null;
        InputStream is = null;

        String result;
        ExecuteWatchdog watchdog = null;
        CmdExecuteResultHandler executeResultHandler = null;

        final int checkIntervalInSeconds = 3;
        for (int i = 0; i < hprofs.size(); i++) {
            try {
                watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
                executeResultHandler = Util.executeCmdAsync(watchdog, "jhat", hprofs.get(i).getPath());
                System.out.println("Parsing HPROF [" + i + "] By jhat...");

                String tmpOutput;
                do {
                    System.out.println("Check jhat parse status after " + checkIntervalInSeconds + " seconds...");
                    Thread.sleep(TimeUnit.SECONDS.toMillis(checkIntervalInSeconds));

                    if (executeResultHandler.hasResult()) {
                        throw new IOException(executeResultHandler.getErrorOutput(), executeResultHandler.getException());
                    }

                    tmpOutput = executeResultHandler.getStandardOutput();
                } while (!tmpOutput.contains("Server is ready."));

                connection = (HttpURLConnection) new URL("http://127.0.0.1:7000/showInstanceCounts/").openConnection();

                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);
                connection.setRequestMethod("GET");

                is = connection.getInputStream();

                System.out.println("Downloading Instance Counts for All Classes...");
                result = IOUtils.toString(is, Charsets.UTF_8);
            } catch (IOException e) {
                if (executeResultHandler != null) {
                    System.err.println(executeResultHandler.getStandardOutput());
                }
                throw e;
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.close(connection);
                watchdog.destroyProcess();
                watchdog = null;
                executeResultHandler = null;
            }

            Matcher m = CLASS_INSTANCE_COUNT.matcher(result);
            if (m.find()) {
                do {
                    String className = m.group(2);
                    if (!className.startsWith(appInfo.getPackageName())) {
                        continue;
                    }

                    List<Integer> counts;
                    if (!classInstanceCounts.containsKey(className)) {
                        counts = new ArrayList<Integer>();
                        classInstanceCounts.put(className, counts);
                    } else {
                        counts = classInstanceCounts.get(className);
                    }
                    counts.add(Integer.valueOf(m.group(1)));
                } while (m.find());
            } else {
                throw new IOException("Invalid Clsass Instances Result from " + hprofs.get(i));
            }
        }
        return classInstanceCounts;
    }

    // XXX: Only for test
    public static void main(String[] args) {
        try {
            new HeapdumpAnalyzer(Util.getForegroundAppInfo(), new File("C:\\Users\\Libram\\Desktop\\com.androidcentral.app")).analyze();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
