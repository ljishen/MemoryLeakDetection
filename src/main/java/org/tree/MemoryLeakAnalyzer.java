package org.tree;

import org.tree.model.ApplicationInfo;
import org.tree.utils.Util;

import java.io.File;
import java.io.IOException;

public class MemoryLeakAnalyzer {
    public static void main(String[] args) {
        try {
            ApplicationInfo appInfo = Util.getForegroundAppInfo();

            EventRecorder recorder = new EventRecorder();
            recorder.record();

            // TODO: Show a file chooser to get eventTemplate file

            final File eventTemplate = new File("C:/Users/Libram/Desktop/events_" + appInfo.getPid());
            EventPlayer player = new EventPlayer(eventTemplate, appInfo);
            File heapdump = player.playback(20);

            new HeapdumpAnalyzer(appInfo, heapdump.getParentFile()).analyze();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}