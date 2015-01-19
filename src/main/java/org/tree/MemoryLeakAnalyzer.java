package org.tree;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.tree.model.ApplicationInfo;
import org.tree.utils.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryLeakAnalyzer {
    private static final Pattern PACKAGE_NAME = Pattern.compile("realActivity=([\\.\\w]+)");
    private static final Pattern PID = Pattern.compile("\\n.+?\\s(\\d+?)\\s");

    public static final int PLAY_BACK_LOOPS = 20;

    public static void main(String[] args) {
        MemoryLeakAnalyzer mla = new MemoryLeakAnalyzer();

        // TODO: Provide selection for target device

        ApplicationInfo appInfo = mla.getForegroundAppInfo();
        if (appInfo == null) {
            System.err.println("Fail to get application info");
            return;
        }

        EventRecorder recorder = new EventRecorder();

        try {
            recorder.record();

            // TODO: Show a file chooser to get eventTemplate file

            Util.dumpheap(appInfo);

            final File eventTemplate = new File("c:/Users/Libram/Desktop/events_" + appInfo.pid());
            EventPlayer player = new EventPlayer(eventTemplate);

            player.playback(PLAY_BACK_LOOPS);

            File heapdump = Util.dumpheap(appInfo);

            List<File> files = new ArrayList<File>(
                    FileUtils.listFiles(
                            heapdump.getParentFile(),
                            new PrefixFileFilter(Util.CONVERTED_HEAP_FILE_PREFIX + Util.localHeapFilename(appInfo.pid())),
                            null));
            if (files.size() < 2) {
                System.err.println("Require at lease two HPROF files to analyze");
                return;
            }

            // newer file first
            Collections.sort(files, new Comparator<File>() {
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

            new HeapdumpAnalyzer(
                    appInfo.packageName(),
                    files.get(0).getPath(),
                    files.get(files.size() - 1).getPath()).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ApplicationInfo getForegroundAppInfo() {
        try {
            String activitiesInfo = Util.executeCmd(Util.ADB, Util.SHELL, "dumpsys", "activity", "activities");

            Matcher m = PACKAGE_NAME.matcher(activitiesInfo);
            if (m.find()) {
                String packageName = m.group(1);
                int length = packageName.length();

                // In command "adb shell ps [package]", package name can only be matched with its last 15 characters.
                String packageAbbreviation = length > 15 ?
                        packageName.substring(length - 15, length) : packageName;

                String pidInfo = Util.executeCmd(Util.ADB, Util.SHELL, "ps", packageAbbreviation);
                m = PID.matcher(pidInfo);
                if (m.find()) {
                    String pid = m.group(1);
                    ApplicationInfo appInfo = new ApplicationInfo(pid, packageName);
                    System.out.println(appInfo);
                    return appInfo;
                }
            }
        } catch (IOException e) {
            // TODO: use log4j instead
            e.printStackTrace();
        }
        return null;
    }
}