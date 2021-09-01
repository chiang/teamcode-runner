package io.teamcode.runner.network;

import io.teamcode.runner.RunnerException;
import io.teamcode.runner.common.JobState;
import io.teamcode.runner.common.UpdateState;
import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.config.RunnerCredentials;
import io.teamcode.runner.network.model.*;
import io.teamcode.runner.trace.JobTrace;
import io.teamcode.runner.trace.TracePatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by chiang on 2017. 4. 27..
 */
@Component
public class NetworkClient implements Network {

    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);

    private static final int CLIENT_ERROR = -100;

    @Autowired
    RunnerConfig runnerConfig;

    @Autowired
    RestTemplate restTemplate;


    @Override
    public void registerRunner() {
        RegisterRunnerRequest registerRunnerRequest = new RegisterRunnerRequest();
        registerRunnerRequest.setToken(runnerConfig.getToken());
        registerRunnerRequest.setInfo(getRunnerVersion());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Accept-Charset", "UTF-8");
        HttpEntity<RegisterRunnerRequest> request = new HttpEntity<>(registerRunnerRequest, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(runnerConfig.getRunnerCredentials().getUrl(), request, Void.class);
            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new RunnerException("Runner 를 서버에 등록하지 못했습니다.");//TODO 상세한 메시지를 주고 받아서 알려줘야 한다.
            }

            logger.info("Runner 정보를 서버에 등록, 업데이트했습니다.");
        } catch(HttpClientErrorException e) {
            throw new RunnerException("Runner 를 서버에 등록하지 못했습니다.", e);
        }
    }

    @Override
    public JobResponseEntity requestJob() {
        logger.trace("Sending a requests for jobs...");
        JobRequest jobRequest = JobRequest.builder()
                .info(getRunnerVersion())
                .token(runnerConfig.getToken())
                .lastUpdate(getLastUpdate(runnerConfig.getRunnerCredentials()))
                .build();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Accept-Charset", "UTF-8");
        HttpEntity<JobRequest> request = new HttpEntity<>(jobRequest, headers);

        String uri = String.format("%s/request", runnerConfig.getRunnerCredentials().getUrl());
        logger.trace("request jobs uri: {}", uri);

        try {
            ResponseEntity<JobResponse> response = restTemplate.postForEntity(uri, request, JobResponse.class);
            if (logger.isTraceEnabled()) {
                logger.trace("response status: {}", response.getStatusCode().getReasonPhrase());
                logger.trace("response entity: {}", response.getBody());
            }

            switch(response.getStatusCode()) {
                case CREATED:
                    logger.info("Checking for jobs... received. job: #{} ({})", response.getBody().getId(), response.getBody().getJobInfo().getName());
                    logger.trace("Job token: {}", response.getBody().getToken());
                    if (logger.isTraceEnabled()) {
                        JobResponse jobResponse = response.getBody();
                        logger.trace("Job variables: {}", jobResponse.getVariables());
                    }
                    return JobResponseEntity.builder().jobResponse(response.getBody()).success(true).build();

                case NO_CONTENT:
                    logger.trace("Checking for jobs... nothing");
                    return JobResponseEntity.builder().jobResponse(null).success(true).build();

                default:
                    logger.error("Checking for jobs... failed. Caused by: {}, {}", response.getStatusCode(), response.getStatusCode().getReasonPhrase());
                    //config.Log().WithField("status", statusText).Warningln("Checking for jobs...", "failed")
                    //n.checkGitLabVersionCompatibility(config.RunnerCredentials)

                    return JobResponseEntity.builder().jobResponse(null).success(true).build();
            }
        } catch (final HttpClientErrorException e) {
            switch(e.getStatusCode()) {
                case NOT_FOUND:
                    //Job 을 요청했는데 처리할 Job 이 하나도 없는 것이니까 그냥 무시.
                    logger.trace("Checking for jobs... job not found");
                    return JobResponseEntity.builder().jobResponse(null).success(false).build();

                case FORBIDDEN:
                    logger.error("Checking for jobs... forbidden");
                    return JobResponseEntity.builder().jobResponse(null).success(false).build();

                default:
                    //TODO 아래 라인으로 올 일이 있나?
                    if (e.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR) {
                        logger.error("Checking for jobs... error");
                        //config.Log().WithField("status", statusText).Errorln("Checking for jobs...", "error")
                        return JobResponseEntity.builder().jobResponse(null).success(false).build();
                    }
                    else {
                        logger.error("Checking for jobs... failed. Caused by: {}, {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase());
                        //config.Log().WithField("status", statusText).Warningln("Checking for jobs...", "failed")
                        //n.checkGitLabVersionCompatibility(config.RunnerCredentials)

                        return JobResponseEntity.builder().jobResponse(null).success(true).build();
                    }
            }
        }

    }

    public JobTrace processJob() {
        //trace := newJobTrace(n, config, jobCredentials)
        //trace.start()
        //return trace

        //JobTrace jobTrace = new JobTrace();
        //jobTrace.setTraceUpdateInterval(RunnerConstants.UPDATE_INTERVAL);
        //jobTrace.start();

        //return jobTrace;
        return null;
    }

    /**
     * 현재 Job 의 상태 값을 TeamCode 서버 측으로 업데이트합니다.
     *
     * @param jobCredentials
     * @param jobState
     * @param trace
     * @return
     */
    @Override
    public UpdateState updateJob(JobCredentials jobCredentials, JobState jobState, String trace) {
        UpdateJobRequest updateJobRequest = UpdateJobRequest.builder()
                .info(getRunnerVersion())
                .token(jobCredentials.getToken())
                .state(jobState)
                .trace(trace)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Accept-Charset", "UTF-8");
        HttpEntity<UpdateJobRequest> request = new HttpEntity<>(updateJobRequest, headers);

        try {
            logger.debug("Updating to job with state: {}, token: {}", jobState, updateJobRequest.getToken());
            ResponseEntity<JobResponse> response
                    = restTemplate.exchange(String.format("%s/jobs/%s",
                    runnerConfig.getRunnerCredentials().getUrl(), jobCredentials.getId()),
                    HttpMethod.PUT,
                    request,
                    JobResponse.class
            );

            if (logger.isTraceEnabled()) {
                logger.trace("response status: {}", response.getStatusCode().getReasonPhrase());
            }

            switch(response.getStatusCode()) {
                case OK:
                    logger.debug("Submitting job to coordinator... {}", "ok");
                    return UpdateState.UPDATE_SUCCEEDED;

                default:
                    logger.warn("Submitting job to coordinator... status code: {}, failed: {}",
                            response.getStatusCode(),
                            response.getStatusCode().getReasonPhrase());

                    return UpdateState.UPDATE_FAILED;
            }
        } catch (final HttpClientErrorException e) {
            switch(e.getStatusCode()) {
                case NOT_FOUND:
                    logger.warn("Submitting job to coordinator... {}", "isAborted");
                    return UpdateState.UPDATE_ABORT;

                case FORBIDDEN:
                    logger.error("Submitting job to coordinator... forbidden: {}", e.getStatusCode().getReasonPhrase());
                    return UpdateState.UPDATE_ABORT;

            /*case clientError:
                log.WithField("status", statusText).Errorln("Submitting job to coordinator...", "error")
                return common.UpdateAbort*/

                default:
                    logger.warn("Submitting job to coordinator... failed: {}", e.getStatusCode().getReasonPhrase());
                    return UpdateState.UPDATE_FAILED;
            }
        }
    }

    public UpdateState patchTrace(JobCredentials jobCredentials, TracePatch tracePatch) {

        String contentRange = String.format("%s-%s", tracePatch.getOffset(), tracePatch.getLimit());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN));
        headers.setAcceptCharset(Arrays.asList(Charset.forName("UTF-8")));
        headers.add("Content-Range", contentRange);
        headers.add("JOB-TOKEN", jobCredentials.getToken());
        logger.debug("<Job #{}> content range: {}", jobCredentials.getId(), contentRange);

        //gitlab <- patch method
        try {
            HttpEntity<String> request = new HttpEntity<>(new String(tracePatch.get(), "UTF-8"), headers);

            ResponseEntity<String> response
                    = restTemplate.exchange(String.format("%s/jobs/%s/trace",
                    runnerConfig.getRunnerCredentials().getUrl(), jobCredentials.getId()),
                    HttpMethod.PATCH,
                    request,
                    String.class
            );

            HttpHeaders responseHeaders = response.getHeaders();
            TracePatchResponse tracePatchResponse
                    = TracePatchResponse
                    .builder()
                    .remoteState(JobState.valueOf(responseHeaders.getFirst("Job-Status")))
                    .build();

            tracePatchResponse.setResponseEntity(response);

            if (tracePatchResponse.isAborted()) {
                logger.warn("Appending trace to coordinator isAborted");
                return UpdateState.UPDATE_ABORT;
            }
            else {
                switch(response.getStatusCode()) {
                    case ACCEPTED:
                        logger.trace("Appending trace to coordinator... ok");
                        return UpdateState.UPDATE_SUCCEEDED;

                    case NOT_FOUND:
                        logger.warn("Appending trace to coordinator... not-found");
                        return UpdateState.UPDATE_NOT_FOUND;

                    case REQUESTED_RANGE_NOT_SATISFIABLE:
                        logger.warn("Appending trace to coordinator... range mismatch");
                        //TODO 응답 값의 Offset 을 JobTrace 에 설정해야 한다...?
                        //tracePatch.SetNewOffset(tracePatchResponse.NewOffset())
                        return UpdateState.UPDATE_RANGE_MISMATCH;

                    /*case response.StatusCode == clientError:
                        log.Errorln("Appending trace to coordinator...", "error")
                        return common.UpdateAbort*/

                    default:
                        logger.warn("Appending trace to coordinator... failed");
                        return UpdateState.UPDATE_FAILED;
                }
            }
        } catch(Throwable t) {
            logger.error("Appending trace to coordinator... error: {}", t);
            return UpdateState.UPDATE_FAILED;
        }
    }

    private VersionInfo getRunnerVersion() {
        VersionInfo versionInfo = VersionInfo.builder()
                .name("runner")
                .version("1.0.0")
                .revision("1.0.1")
                .platform("afbc")
                .architecture("abc")
                .executor("").build();



        /*if executor := common.GetExecutor(config.Executor); executor != nil {
            executor.GetFeatures(&info.Features)
        }

        if shell := common.GetShell(config.Shell); shell != nil {
            shell.GetFeatures(&info.Features)
        }*/

        return versionInfo;
    }

    private String getLastUpdate(final RunnerCredentials runnerCredentials) {
        TeamCodeClient client = getClient();

        //TODO
        //return client.getLastUpdate();
        return "";

        /*
        cli, err := n.getClient(credentials)
        if err != nil {
            return ""
        }
        return cli.getLastUpdate()
         */
    }

    private TeamCodeClient getClient() {

        return null;
    }

    /*func (n *GitLabClient) getLastUpdate(credentials requestCredentials) (lu string) {
        cli, err := n.getClient(credentials)
        if err != nil {
            return ""
        }
        return cli.getLastUpdate()
    }*/
}
