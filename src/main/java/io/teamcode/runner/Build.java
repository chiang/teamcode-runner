package io.teamcode.runner;

import io.teamcode.runner.common.*;
import io.teamcode.runner.common.util.FileUtils;
import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.executor.AbstractExecutor;
import io.teamcode.runner.executor.Executor;
import io.teamcode.runner.executor.ExecutorCommand;
import io.teamcode.runner.executor.ExecutorException;
import io.teamcode.runner.executor.docker.DockerExecutor;
import io.teamcode.runner.network.JobCredentials;
import io.teamcode.runner.network.model.Artifact;
import io.teamcode.runner.network.model.JobResponse;
import io.teamcode.runner.network.model.Variable;
import io.teamcode.runner.shell.Shell;
import io.teamcode.runner.trace.JobTrace;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread 마다 생성되어야 하므로 @Component 로 만들지 않습니다.
 *
 *
 */
@Data
public class Build {

    private static final Logger logger = LoggerFactory.getLogger(Build.class);

    private RunnerConfig runnerConfig;

    private JobResponse jobResponse;

    private JobTrace jobTrace;

    private BuildRuntimeState currentState;

    private BuildStage currentStage;

    /**
     * // Unique ID for all running builds on this runner and this project
     ProjectRunnerID int `json:"project_runner_id"`
     //TODO 이것으로 여러 빌드를 제어하니까 잘 살펴보자!!!!
     */
    private static AtomicLong projectRunnerId = new AtomicLong(0);

    /**
     * Docker 컨테이너 내에서의 빌드 디렉터리 경로. 이 경로는 실제로 외부 Volume Container 와 연결된 경로가 됩니다.
     *
     */
    private String buildDirInContainer;

    private String buildDir;

    public Build(RunnerConfig runnerConfig, JobResponse jobResponse) {
        this.projectRunnerId.incrementAndGet();
        this.runnerConfig = runnerConfig;
        this.jobResponse = jobResponse;
        this.jobTrace = new JobTrace(
                JobCredentials.builder()
                        .id(jobResponse.getId())
                        .token(jobResponse.getToken())
                        .build(), runnerConfig.isWatchEnabled());
        this.jobTrace.setTraceUpdateInterval(RunnerConstants.UPDATE_INTERVAL);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(jobTrace);
    }

    //Async
    public void run() {
        this.currentState = BuildRuntimeState.PENDING;

        DockerExecutor dockerExecutor = null;
        try {
            this.setupBuildDir();

            //logger.debug("Running with {}\n  on {} ({})", AppVersion.Line(), b.Runner.Name, b.Runner.ShortDescription()));
            logger.debug("Running with ({})", runnerConfig.getShortDescription());

            ShellScriptInfo shellScriptInfo = new ShellScriptInfo();
            shellScriptInfo.setShell("bash");
            shellScriptInfo.setJobResponse(jobResponse);
            shellScriptInfo.setDockerConfig(runnerConfig.getDocker());
            shellScriptInfo.setBuild(this);

            dockerExecutor = new DockerExecutor(runnerConfig, shellScriptInfo, jobTrace);
            dockerExecutor.prepare(runnerConfig);

            this.doRun(dockerExecutor);
            dockerExecutor.finish();

            this.jobTrace.success();
        } catch (AbortedException e) {
            logger.warn("현재 Job 이 CI 서버에서 취소되었습니다. 현재 빌드를 취소하고 서버에 취소 메시지를 전달합니다...");
            this.jobTrace.cancel();

        } catch (Throwable t) {
            logger.error("Build failed", t);
            this.jobTrace.fail(t.getMessage());
        } finally {
            if (dockerExecutor != null) {
                dockerExecutor.cleanup();
            }
        }
    }

    private void setupBuildDir() throws ExecutorException {
        long pipelineId = this.jobResponse.getPipelineId().longValue();
        String[] yearMonth = DateUtils.yearMonthArray();

        // 아래는 초기 코드. 한 Stage 에서 여러 Job 이 동시에 실행될 수 있으므로 Pipeline 별로 빌드 디렉터리를 만들면 충돌됨.
        //this.buildDirInContainer
        //        = String.format("/var/opt/teamcode/data/ci/pipelines/%s/%s/%s/%s", yearMonth[0], yearMonth[1], pipelineId % 256, pipelineId);

        // 경로 '/var/opt/teamcode' 는 Volume 이 매핑된 경로입니다. 따라서 이 밑으로 필요한 경로를 매핑시키면 됩니다.
        //TODO Container 내부 경로와 외부 Host 상의 경로간에 중간 경로가 하드코딩되어 있으므로 이를 관리해야 함.

        //한 파이프라인 디렉터리에 여러 Job 을 넣을 수 있도록 합니다.
        //날짜로 구분하지 않습니다. 날짜로 구분하면 실행 중에 자정에서 넘어갈 때 문제가 생길 수 있습니다.fsetup
        this.buildDirInContainer
                = String.format("/var/opt/teamcode/data/ci/pipelines/%s/%s/%s/%s",
                jobResponse.getJobInfo().getProjectPath(),
                pipelineId % 256,
                pipelineId,
                this.jobResponse.getId());

        //FIXME Teamcode 이전 버전을 위한 방어적인 코드
        String projectPath = StringUtils.hasText(jobResponse.getJobInfo().getProjectPath()) ?
                jobResponse.getJobInfo().getProjectPath() : jobResponse.getJobInfo().getProjectId().toString();

        Path path = Paths.get(this.runnerConfig.getPipelinesWorkDir().getAbsolutePath(),
                projectPath,
                String.valueOf(pipelineId % 256),
                String.valueOf(pipelineId),
                String.valueOf(jobResponse.getId()));

        File dir = path.toFile();

        if (dir.exists()) {
            logger.debug("파이프라인 작업 디렉터리가 이미 존재합니다. 삭제 후 다시 생성합니다. 기존 디렉터리: {}", dir.exists());

            if (!FileSystemUtils.deleteRecursively(dir)) {
                logger.warn("기존에 만들어진 파이프라인 작업 디렉터리를 삭제할 수 없습니다.");

                throw new ExecutorException("기존에 만들어진 파이프라인 작업 디렉터리를 삭제할 수 없습니다.");
            }
            else {
                if (!dir.mkdirs()) {
                    throw new ExecutorException(String.format("파이프라인 작업 디렉터리 %s 를 만들 수 없습니다.", dir.getAbsolutePath()));
                }
            }
        }
        else {
            if (!dir.mkdirs()) {
                throw new ExecutorException(String.format("파이프라인 작업 디렉터리 %s 를 만들 수 없습니다.", dir.getAbsolutePath()));
            }
        }

        this.buildDir = path.toFile().getAbsolutePath();
    }

    private final void doRun(Executor executor) throws ExecutorException, AbortedException {
        this.currentState = BuildRuntimeState.RUNNING;

        executeScript(executor);
    }

    public String getBuildDir() {
        return this.buildDir;
    }

    public String getBuildDirInContainer() {

        return this.buildDirInContainer;
    }

    public String getHostname() {
        //TODO
        return "";
    }

    public boolean isDebugTraceEnabled() {
        /**
         * func (b *Build) IsDebugTraceEnabled() bool {
         trace, err := strconv.ParseBool(b.GetAllVariables().Get("CI_DEBUG_TRACE"))
         if err != nil {
         return false
         }

         return trace
         }
         */

        return false;
    }


    private void retryCreateExecutor(AbstractExecutor abstractExecutor) {

        //abstractExecutor.prepare(runnerConfig);

    }

    private void executeScript(Executor executor) throws ExecutorException, AbortedException {
        try {
            this.executeStage(BuildStage.PREPARE, executor);

            this.attemptExecuteStage(BuildStage.GET_SOURCES, executor, getGetSourcesAttempts());

            this.attemptExecuteStage(BuildStage.DOWNLOAD_ARTIFACTS, executor, getGetSourcesAttempts());

            this.attemptExecuteStage(BuildStage.RESTORE_CACHE, executor, getGetSourcesAttempts());

            // Execute user build script (before_script + script)
            this.executeStage(BuildStage.USER_SCRIPT, executor);

            this.executeStage(BuildStage.AFTER_SCRIPT, executor);

            // Execute post script (cache store, artifacts upload)
            this.executeStage(BuildStage.ARCHIVE_CACHE, executor);

            this.executeStage(BuildStage.UPLOAD_ARTIFACTS, executor);
        } catch (IOException e) {
            throw new ExecutorException("빌드 스크립트를 실행하던 중 오류가 발생했습니다.", e);
        }
    }

    private void executeStage(BuildStage buildStage, Executor executor) throws IOException, ExecutorException, AbortedException {
        if (this.jobTrace.isFinished()) {
            //Cancel 인 경우가 되겠다.
            logger.warn("Job 이 이미 종료되어 스테이지 {} 를 실행할 수 없습니다. Job 이 Cancel 되었을 수 있습니다.", buildStage);
            return;
        }


        this.currentStage = buildStage;

        Shell shell = executor.getShell();
        if (shell == null) {
            throw new IllegalArgumentException("No shell defined");//TODO custom error
        }

        try {
            String script = shell.generateShellScript(buildStage, executor.getShellScriptInfo());
            logger.debug("Executing build stage '{}'... with script: [{}]", buildStage, script);

            // Nothing to execute
            //TODO 일단 별도 스크립트가 필요 없습니다. 그냥 Artifact 를 저장하니까요...
            /*if (!StringUtils.hasText(script)) {
                logger.debug("Generated script is empty. Skipping execute stage '{}'", buildStage);
                return;
            }*/

            ExecutorCommand executorCommand
                    = ExecutorCommand.builder().buildStage(buildStage).script(script).build();

            switch (buildStage) {
                case USER_SCRIPT:
                case AFTER_SCRIPT:
                    executorCommand.setPredefined(false);//use custom build environment
                    break;

                default:// all other stages use a predefined build environment
                    executorCommand.setPredefined(true);
                    break;
            }

            executor.run(executorCommand);
        } catch (IOException e) {
            //TODO logging?
            throw e;
        }
    }

    private void attemptExecuteStage(BuildStage buildStage, Executor executor, int attempts) throws ExecutorException, AbortedException {
        if (attempts < 1 || attempts > 10) {
            //TODO custom error
            throw new IllegalArgumentException("Number of attempts out of the range [1, 10] for stage: %s".format(buildStage.toString()));
        }

        for (int i = 0; i < attempts; i++) {
            try {
                executeStage(buildStage, executor);
                break;
            } catch (IOException e) {
                throw new ExecutorException(e);//TODO add message
            }
        }
    }


    public List<JobVariable> getAllVariables() {
        List<JobVariable> jobVariables = new ArrayList<>();

        for (Variable variable: jobResponse.getVariables())
            jobVariables.add(JobVariable.builder().name(variable.getName()).value(variable.getValue()).build());

        jobVariables.addAll(getDefaultVariables());
        //TODO runner 자체에 설정한 변수도 바인딩한다.

        return jobVariables;
    }

    private List<JobVariable> getDefaultVariables() {

        return Arrays.asList(
                JobVariable.builder().name("CI_PROJECT_DIR").value("a").publik(true).internal(true).file(false).build(),
                JobVariable.builder().name("CI_SERVER").value("yes").publik(true).internal(true).file(false).build()
        );

        //{Key: "CI_PROJECT_DIR", Value: b.FullProjectDir(), Public: true, Internal: true, File: false},
        //{Key: "CI_SERVER", Value: "yes", Public: true, Internal: true, File: false},
    }

    public int getGetSourcesAttempts() {
        if (getAllVariables().stream().anyMatch(j -> j.getName().equals("GET_SOURCES_ATTEMPTS"))) {
            JobVariable jobVariable = getAllVariables().stream().filter(j -> j.getName().equals("GET_SOURCES_ATTEMPTS")).findFirst().get();
            return Integer.parseInt(jobVariable.getValue());
        }
        else {
            return RunnerConstants.DEFAULT_GET_SOURCES_ATTEMPTS;
        }
    }

    /**
     *
     func (b *Build) executeUploadArtifacts(state error, executor Executor, abort chan interface{}) (err error) {
     jobState := state

     for _, artifacts := range b.Artifacts {
     when := artifacts.When
     if state == nil {
     // Previous stages were successful
     if when == "" || when == ArtifactWhenOnSuccess || when == ArtifactWhenAlways {
     state = b.executeStage(BuildStageUploadArtifacts, executor, abort)
     }
     } else {
     // Previous stage did fail
     if when == ArtifactWhenOnFailure || when == ArtifactWhenAlways {
     err = b.executeStage(BuildStageUploadArtifacts, executor, abort)
     }
     }
     }

     // Use job's error if set
     if jobState != nil {
     err = jobState
     }
     return
     }

     * @return
     */

    public String getProjectUniqueName() {
        return String.format("runner-%s-project-%d-concurrent-%d",
                runnerConfig.getShortDescription(),
                jobResponse.getJobInfo().getProjectId(),
                this.projectRunnerId.get());
    }

    /*private String getProjectUniqueDir() {
        //dir = fmt.Sprintf("project-%d", b.JobInfo.ProjectID)

        return String.format("project-%s", jobResponse.getJobInfo().getProjectId());
    }*/

    /*func (b *Build) executeUploadArtifacts(state error, executor Executor, abort chan interface{}) (err error) {
        jobState := state

        for _, artifacts := range b.Artifact {
            when := artifacts.When
            if state == nil {
                // Previous stages were successful
                if when == "" || when == ArtifactWhenOnSuccess || when == ArtifactWhenAlways {
                    state = b.executeStage(BuildStageUploadArtifacts, executor, abort)
                }
            } else {
                // Previous stage did fail
                if when == ArtifactWhenOnFailure || when == ArtifactWhenAlways {
                    err = b.executeStage(BuildStageUploadArtifacts, executor, abort)
                }
            }
        }

        // Use job's error if set
        if jobState != nil {
            err = jobState
        }
        return
    }*/

}
