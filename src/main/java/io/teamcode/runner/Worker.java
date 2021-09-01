package io.teamcode.runner;

import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.network.NetworkClient;
import io.teamcode.runner.network.model.*;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chiang on 2017. 5. 5..
 */
public class Worker implements Callable<String> {

    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    private RunnerConfig runnerConfig;

    private NetworkClient networkClient;

    public Worker(RunnerConfig runnerConfig, NetworkClient networkClient) {
        this.runnerConfig = runnerConfig;
        this.networkClient = networkClient;
    }

    @Override
    public String call() throws Exception {
        logger.trace("데이터를 수집합니다...");

        requestJobs();

        return "finished....";
    }

    private void requestJobs() {
        try {
            JobResponseEntity jobResponseEntity = networkClient.requestJob();
            if (jobResponseEntity.isSuccess() && jobResponseEntity.getJobResponse() != null) {
                JobResponse jobResponse = jobResponseEntity.getJobResponse();
                if (!StringUtils.hasText(jobResponse.getImage())) {
                    jobResponse.setImage(runnerConfig.getDocker().getImage());
                }

            /*JobResponse jobResponse = new JobResponse();
            jobResponse.setImage("openjdk:8");
            jobResponse.setJobInfo(JobInfo.builder().projectId(new Long(3)).build());
            jobResponse.setSteps(Arrays.asList(Step.builder().name("script").scripts(Arrays.asList("ls")).build()));

            RepositoryInfo repositoryInfo = RepositoryInfo.builder().url("file:///Users/chiang/my-temp/teamcode-home/data/repositories/example").revision("HEAD").build();
            jobResponse.setRepositoryInfo(repositoryInfo);*/

            Thread.currentThread().setName(String.format("thread-job-%s-%s", jobResponse.getId(), jobResponse.getJobInfo().getName()));


                Build build = new Build(runnerConfig, jobResponse);
                build.run();
            }
        } catch (Throwable t) {
            if (t instanceof ResourceAccessException) {
                logger.error("팀코드 서버에 접속할 수 없습니다. 팀코드 서버 주소 및 포트를 확인해 보세요. 문제가 없다면 방화벽도 확인해 보시기 바랍니니다. 상세 메시지: {}", t.getMessage());
            }
            else {
                t.printStackTrace();
            }
        }
    }
}
