package io.teamcode.runner.common;

import io.teamcode.runner.Build;
import io.teamcode.runner.config.DockerConfig;
import io.teamcode.runner.network.model.JobResponse;
import lombok.Data;

/**
 * Created by chiang on 2017. 4. 27..
 */
@Data
public class ShellScriptInfo {

    private String shell;

    private Build build;

    /**
     * Shell이 사용하는 사용자 계정. ShellExecutor 는 로컬에서 실행이 되는데 이 경우 어떤 사용자로 실행할 것인지를 설정합니다. 기본은
     * <code>teamcode</code> 입니다.
     *
     */
    private String user;

    private String runnercommand;

    private String preCloneScript;

    private String preBuildScript;

    private String postBuildScript;

    private JobResponse jobResponse;

    private DockerConfig dockerConfig;


    //Type            ShellType


}
