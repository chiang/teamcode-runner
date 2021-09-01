package io.teamcode.runner.common.io;


import org.apache.commons.exec.LogOutputStream;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by chiang on 2017. 8. 13..
 */
public class ExecOutputStream extends LogOutputStream {

    private final List<String> lines = new LinkedList<>();

    private int exitCode;

    @Override
    protected void processLine(String line, int level) {
        lines.add(line);
    }

    public List<String> getLines() {
        return lines;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}
