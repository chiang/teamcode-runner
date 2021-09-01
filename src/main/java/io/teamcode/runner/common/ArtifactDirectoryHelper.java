package io.teamcode.runner.common;

import io.teamcode.runner.network.model.JobResponse;

import java.io.File;
import java.io.IOException;

/**
 * Created by chiang on 2017. 6. 28..
 */
public abstract class ArtifactDirectoryHelper {


    public static final File createArtifactDirectory(File artifactsRootDir, JobResponse jobResponse, Long jobId) throws IOException {
        String path = String.format("%s/%s/%s/%s/%s/%s",
                artifactsRootDir.getAbsolutePath(),
                jobResponse.getJobInfo().getProjectPath(),
                jobResponse.getPipelineId() % 256,
                jobResponse.getPipelineId(),
                jobId % 256,
                jobId);

        File artifactDir = new File(path);
        if (!artifactDir.exists()) {
            if (!artifactDir.mkdirs()) {
                throw new IOException(String.format("Artifact 디렉터리 '%s' 를 만들 수 없습니다.", artifactDir.getAbsolutePath()));
            }
        }

        return artifactDir;
    }

}
