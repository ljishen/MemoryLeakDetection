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
        int innerLoop = loop;
        if (innerLoop % 2 != 0) {
            innerLoop++;
        }

        System.out.println("Playback For " + innerLoop + " Times");

        for (int i = 0; i < innerLoop; i++) {
            if (i % 2 == 0) {
                Util.dumpheap(appInfo);
            }
            System.out.println("Playback [" + i + "]");
            Util.executeCmd(Util.MONKEY_RUNNER, Util.getResourcePath(this.getClass(), "monkey_playback.py"), eventTemplate.getPath());
        }

        return Util.dumpheap(appInfo);
    }

    // XXX: Only for test
    public static void main(String[] args) {
        try {
            EventPlayer player = new EventPlayer(
                    new File("C:/Users/Libram/Desktop/ifeng.mr"),
                    Util.getForegroundAppInfo());
            player.playback(20);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
