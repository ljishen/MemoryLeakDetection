package org.tree;

import org.tree.utils.Util;

import java.io.IOException;

public class EventRecorder {

    public void record() throws IOException {
        // TODO: Consider a more efficient event recorder
        Util.executeCmd(Util.MONKEY_RUNNER, Util.getResourcePath(this.getClass(), "monkey_recorder.py"));
    }
}
