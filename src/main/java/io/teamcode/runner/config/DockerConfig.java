package io.teamcode.runner.config;

import io.teamcode.runner.common.docker.DockerPullPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chiang on 2017. 5. 8..
 */
@Data
@Configuration
@ConfigurationProperties(prefix="runners.docker", ignoreUnknownFields = true)
public class DockerConfig {

    /**
     * specify custom hostname for Docker container
     */
    private String hostname;

    private String image;

    /**
     * string value containing the cgroups CpusetCpus to use
     * 컨테이너가 사용할 cpu core 갯수를 할당 할 수 있다.
     *
     * <code>--cpuset-cpus=0 : 0번째 cpu 하나를 할당한다.</code>
     * <code>-cpuset-cpus=0,1 : 0, 1번째 cpu를 할당한다.</code>
     */
    private String cpusetCpus;

    /**
     * make container run in Privileged mode (insecure)
     *
     * <code>true</code> 이면 컨테이너 안에서 호스트의 리눅스 커널 기능(Capability)을 모두 사용합니다.
     *
     */
    private Boolean privileged;

    private DockerPullPolicy pullPolicy = DockerPullPolicy.NEVER;

    /**
     * specify where Docker caches should be stored (this can be absolute or relative to current working directory)
     * 실제로는 절대 경로만 허용합니다.
     *
     * TODO cacheDir 이 있는지와 절대 경로인지를 체크
     */
    private String cacheDir;

    private List<String> volumes = new ArrayList<>();

}
