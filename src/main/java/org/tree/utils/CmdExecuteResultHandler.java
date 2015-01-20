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

    private volatile String finalOutput;

    public CmdExecuteResultHandler(
            ByteArrayOutputStream out, ByteArrayOutputStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        super.onProcessComplete(exitValue);
        obtainStandardOutputAndCleanUp();
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        super.onProcessFailed(e);
        obtainStandardOutputAndCleanUp();
    }

    private void obtainStandardOutputAndCleanUp() {
        try {
            finalOutput = getStandardOutput();
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);
        }
    }

    public String getErrorOutput() {
        try {
            return err.toString(Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getStandardOutput() {
        if (finalOutput == null) {
            try {
                return out.toString(Charsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return finalOutput;
        }
    }
}
