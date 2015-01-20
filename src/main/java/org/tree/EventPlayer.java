package org.tree;

import org.tree.model.ApplicationInfo;
import org.tree.utils.Util;

import java.io.File;
import java.io.IOException;

public class EventPlayer {
    private File eventTemplate;
    private ApplicationInfo appInfo;

    public EventPlayer(File eventTemplate, ApplicationInfo appInfo) {
        this.eventTemplate = eventTemplate;
        this.appInfo = appInfo;
    }

    public File playback(int loop) throws IOException, InterruptedException {
        Util.dumpheap(appInfo);

        // TODO: generate a heap dump after several plays
        for (int i = 0; i < loop; i++) {
            Util.executeCmd(Util.MONKEY_RUNNER, Util.getResourcePath(this.getClass(), "monkey_playback.py"), eventTemplate.getPath());
        }

        return Util.dumpheap(appInfo).getParentFile();
    }
}
