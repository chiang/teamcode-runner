package io.teamcode.runner.network.model;

import io.teamcode.runner.common.Dependency;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Runner 가 Job 을 Request 해서 받아온 응답 값을 여기에 할당합니다. 이 값은 Job 을 처리하는 내내 사용합니다.
 *
 */
@Data
public class JobResponse {

    /**
     * TeamCode CI 서버 상에서 Job 의 아이디.
     *
     */
    private Long id;

    private Long pipelineId;

    /**
     * Job 정보를 TeamCode CI 와 통신할 때 사용하는 Token. 이 값은 Runner 가 TeamCode CI 에 요청할 때 해당 요청이 유효한지를 검사할 때
     * 사용합니다.
     *
     */
    private String token;

    private boolean allowGitFetch;

    private RepositoryInfo repositoryInfo;

    private JobInfo jobInfo;

    private String image;

    private List<Step> steps;

    private List<Variable> variables;

    private Artifact artifact;

    private List<Dependency> dependencies;

    //GitInfo       GitInfo       `json:"git_info"`
    //RunnerInfo    RunnerInfo    `json:"runner_info"`
    //Services      Services      `json:"services"`
    //Cache         Caches        `json:"cache"`
    //Credentials   []Credentials `json:"credentials"`

    public boolean hasArtifacts() {

        return this.artifact != null && this.artifact.getPaths().size() > 0;
    }

    public List<Variable> getVariables() {
        if (this.variables == null)
            return Collections.emptyList();

        return this.variables;
    }

    public List<Dependency> getDependencies() {
        if (this.dependencies == null)
            return Collections.emptyList();

        return this.dependencies;
    }
}
