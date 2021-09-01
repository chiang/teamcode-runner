package io.teamcode.common;

import io.teamcode.runner.common.ArtifactArchiver;
import io.teamcode.runner.network.model.Artifact;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chiang on 2017. 7. 16..
 */
public class ArtifactArchiverTest {

    @Test
    public void paths() {
        File parentDir = new File(".");
        Artifact artifact = new Artifact();

        List<String> artifactPaths = new ArrayList<>();
        artifactPaths.add("src");
        artifactPaths.add("build.gradle");

        artifact.setPaths(artifactPaths);

        List<String> paths = ArtifactArchiver.paths(parentDir, artifact);
        for (String path: paths) {
            System.out.println("path: " + path);
        }
    }

    @Test
    public void zip() throws IOException {
        File parentDir = new File("/Users/chiang/projects/teamcode-runner");
        File zipFile = new File(parentDir, "build/artifact-test.zip");

        Artifact artifact = new Artifact();

        List<String> artifactPaths = new ArrayList<>();
        artifactPaths.add("src");
        artifactPaths.add("build.gradle");

        artifact.setPaths(artifactPaths);

        List<String> paths = ArtifactArchiver.paths(parentDir, artifact);

        ArtifactArchiver.zip(parentDir, zipFile, paths);
        Assert.assertTrue(zipFile.exists());

        zipFile.delete();
    }
}
