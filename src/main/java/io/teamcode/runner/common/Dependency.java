package io.teamcode.runner.common;

import io.teamcode.runner.network.model.DependencyArtifactsFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 빌드 시 필요로 하는 것들?
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {

    private long id;

    private String token;

    private String name;

    private DependencyArtifactsFile artifactsFile;

    /*
    ID            int                     `json:"id"`
	Token         string                  `json:"token"`
	Name          string                  `json:"name"`
	ArtifactsFile DependencyArtifactsFile `json:"artifacts_file"`
     */
}
