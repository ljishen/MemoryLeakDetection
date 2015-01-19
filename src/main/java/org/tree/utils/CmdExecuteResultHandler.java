package org.tree.utils;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class CmdExecuteResultHandler extends DefaultExecuteResultHandler {
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    private String output;

    public CmdExecuteResultHandler(
            ByteArrayOutputStream out, ByteArrayOutputStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        super.onProcessComplete(exitValue);
        obtainOutputAndCleanUp();
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        super.onProcessFailed(e);
        obtainOutputAndCleanUp();
    }

    private void obtainOutputAndCleanUp() {
        try {
            output = out.toString(Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);
        }
    }

    public String getOutput() {
        if (!hasResult()) {
            throw new IllegalStateException("The process has not exited yet therefore no result is available ...");
        }

        return output;
    }
}
