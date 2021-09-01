package io.teamcode.runner;

import io.teamcode.runner.common.UpdateState;
import io.teamcode.runner.common.docker.DockerPullPolicy;
import io.teamcode.runner.config.DockerConfig;
import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.network.NetworkClient;
import io.teamcode.runner.network.model.*;
import io.teamcode.runner.trace.JobTrace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Build 동작 테스트. 먼저 파일 시스템 기반의 Subversion Repository 를 준비해야 합니다.
 *
 * <ul>
 *     <li>docker create -v /Users/chiang/my-temp/teamcode-home/data/ci:/var/opt/teamcode/data/ci --name tccidata busybox /bin/true</li>
 * </ul>
 *
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
//@RunWith(MockitoJUnitRunner.class)
public class BuildTest {

    @MockBean
    RunnerConfig runnerConfig;

    @MockBean
    JobTrace jobTrace;

    @Mock
    RunnerScheduler runnerScheduler;

    @MockBean
    NetworkClient networkClient;

    @Before
    public void init() {
        ReflectionTestUtils.setField(runnerConfig,"serverPort", 9999);
        //ReflectionTestUtils.setField(runnerConfig,"buildsPipelinesDir", "aafsa");
    }

    @Test
    public void executeBuild() {
        //Mockito.doThrow(new RuntimeException()).when(jobTrace.inc)
        Mockito.when(networkClient.patchTrace(null, null)).thenReturn(UpdateState.UPDATE_SUCCEEDED);

        String pipelinesWorkDirPath = "/Users/chiang/my-temp/teamcode-home/data/ci/pipelines";
        String artifactsDirPath = "/Users/chiang/my-temp/teamcode-home/data/ci/artifacts";

        Mockito.when(runnerConfig.getPipelinesWorkDir()).thenReturn(new File(pipelinesWorkDirPath));
        Mockito.when(runnerConfig.getArtifactsDir()).thenReturn(new File(artifactsDirPath));

        DockerConfig dockerConfig = Mockito.mock(DockerConfig.class);
        Mockito.when(dockerConfig.getCacheDir()).thenReturn("");
        Mockito.when(dockerConfig.getPullPolicy()).thenReturn(DockerPullPolicy.NEVER);
        Mockito.when(runnerConfig.getDocker()).thenReturn(dockerConfig);


        //Mockito.doThrow(new RuntimeException()).when(runnerScheduler).init();

        Build build = new Build(runnerConfig, buildJobResponse());
        build.run();
    }

    private JobResponse buildJobResponse() {
        JobResponse jobResponse = new JobResponse();
        jobResponse.setId(new Long(3));
        jobResponse.setPipelineId(new Long(10));
        jobResponse.setImage("teamcode/default-image:0.1");

        //파이프라인 설정 파일에서 설정한 Global Variables
        List<Variable> variables = new ArrayList<>();
        variables.add(Variable.builder().name("DB_USER").value("tiger").build());
        jobResponse.setVariables(variables);

        RepositoryInfo repositoryInfo = new RepositoryInfo();
        repositoryInfo.setUrl("file:///Users/chiang/my-temp/teamcode-home/data/repositories/wings/trunk");
        repositoryInfo.setRevision("HEAD");
        jobResponse.setRepositoryInfo(repositoryInfo);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setProjectId(new Long(2));
        jobResponse.setJobInfo(jobInfo);

        List<Step> steps = new ArrayList<>();
        //steps.add(Step.builder().name("script").scripts(Arrays.asList("./mvnw package")).timeout(100).build());
        steps.add(Step.builder().name("script")
                .scripts(Arrays.asList("echo $DB_USER", "echo $CI_SERVER", "ls -al", "./gradlew package")).timeout(100).build());

        Artifact artifact = new Artifact();
        artifact.setName("build-test-artifact");
        artifact.setPaths(Arrays.asList("src/main/resources/logback.xml"));

        jobResponse.setArtifact(artifact);

        jobResponse.setSteps(steps);

        return jobResponse;
    }
}
