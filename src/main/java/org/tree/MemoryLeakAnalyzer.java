package org.tree;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.tree.model.ApplicationInfo;
import org.tree.utils.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MemoryLeakAnalyzer {
    public static void main(String[] args) {
        // TODO: Provide selection for target device

        ApplicationInfo appInfo = Util.getForegroundAppInfo();
        if (appInfo == null && !appInfo.isValid()) {
            System.err.println("Fail to get application info");
            return;
        }

        EventRecorder recorder = new EventRecorder();

        try {
            recorder.record();

            // TODO: Show a file chooser to get eventTemplate file

            final File eventTemplate = new File("c:/Users/Libram/Desktop/events_" + appInfo.getPid());
            EventPlayer player = new EventPlayer(eventTemplate, appInfo);
            File heapdumpFolder = player.playback(20);

            List<File> files = new ArrayList<File>(
                    FileUtils.listFiles(
                            heapdumpFolder,
                            new PrefixFileFilter(Util.convertedHeapFilePrefix(appInfo.getPid())),
                            null));

            new HeapdumpAnalyzer(appInfo.getPackageName(), files).analyze();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}