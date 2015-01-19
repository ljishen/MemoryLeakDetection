package org.tree;

import org.tree.utils.Util;

import java.io.File;
import java.io.IOException;

public class EventPlayer {
    private File eventTemplate;

    public EventPlayer(File eventTemplate) {
        this.eventTemplate = eventTemplate;
    }

    public void playback(int loop) throws IOException {
        for (int i = 0; i < loop; i++) {
            Util.executeCmd(Util.MONKEY_RUNNER, Util.getResourcePath(this.getClass(), "monkey_playback.py"), eventTemplate.getPath());
        }
    }
}
