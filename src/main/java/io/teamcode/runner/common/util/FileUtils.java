package io.teamcode.runner.common.util;

import java.io.File;

/**
 * Created by chiang on 2017. 7. 16..
 */
public abstract class FileUtils {

    public static final String getRelativePath(final File baseDir, final String fileName) {
        int index = fileName.indexOf(baseDir.getAbsolutePath());
        if (index != -1)
            return fileName.substring(baseDir.getAbsolutePath().length(), fileName.length());

        return fileName;
    }

}
