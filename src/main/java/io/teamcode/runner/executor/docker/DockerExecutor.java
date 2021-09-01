package io.teamcode.runner.executor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.teamcode.runner.AbortedException;
import io.teamcode.runner.BuildError;
import io.teamcode.runner.common.*;
import io.teamcode.runner.common.docker.DockerPullPolicy;
import io.teamcode.runner.common.io.ExecOutputStream;
import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.executor.AbstractExecutor;
import io.teamcode.runner.executor.ExecutorCommand;
import io.teamcode.runner.executor.ExecutorException;
import io.teamcode.runner.network.model.Artifact;
import io.teamcode.runner.network.model.JobResponse;
import io.teamcode.runner.network.model.RepositoryInfo;
import io.teamcode.runner.trace.ExecStartResultJobTraceCallback;
import io.teamcode.runner.trace.JobTrace;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>기본 구조는, 실행 전에 Container 를 생성합니다. 이 작업은 Prepare 단게에서 합니다. Container 를 생성하면서 체크아웃한 소스 코드를
 * 저장할 곳을 외부 Volume (Cache Volume) 과 연결 시키는 등 작업을 합니다.</p>
 *
 */
public class DockerExecutor extends AbstractExecutor {

    public static final String EXECUTOR_NAME = "docker+machine";

    private static final String DEFAULT_BUILDS_DIR = "/builds";

    private static final Logger logger = LoggerFactory.getLogger(DockerExecutor.class);

    private DockerClient dockerClient;

    private boolean sharedBuildsDir;

    private final Map<String, Object> options = new HashMap<>();

    //// IDs of containers that have failed in some way
    private List<String> failures = new ArrayList<>();

    private Info dockerInfo;

    private InspectContainerResponse buildContainer;

    private String hostName;

    private String hostCacheDir;

    /**
     * Docker Container 실행 시 바인드할 볼륨 목록. /home/users/data:/data 와 같은 형식의 문자열이 들어갑니다..
     *
     */
    private List<String> volumes = new ArrayList<>();

    public DockerExecutor(RunnerConfig runnerConfig, ShellScriptInfo shellScriptInfo, JobTrace jobTrace) {
        super(runnerConfig, shellScriptInfo, jobTrace);

        options.put("ShowStdout", true);
        options.put("ShowStderr", true);
        options.put("Timestamps", true);
    }

    @Override
    public void doPrepare(RunnerConfig runnerConfig) throws ExecutorException, AbortedException {
        this.prepareDocker(runnerConfig);

        buildLogger.debugln("Starting Docker command...");

        /*
        if len(s.BuildShell.DockerCommand) == 0 {
		return errors.New("Script is not compatible with Docker") }
         */

        String imageName = this.getImageName();

        //TODO not yet supported ContainerInfo predefinedContainer = this.createContainer("predefined", "", "");
        if (shellConfiguration.isPredefined()) {
            buildContainer = this.createContainer("predefined", imageName,
                    shellConfiguration.getDockerCommand().toArray(new String[shellConfiguration.getDockerCommand().size()])).exec();
        }
        else {
            buildContainer = this.createContainer("build", imageName,
                    shellConfiguration.getDockerCommand().toArray(new String[shellConfiguration.getDockerCommand().size()])).exec();
        }
    }

    private void prepareDocker(final RunnerConfig runnerConfig) throws ExecutorException {
        try {
            logger.debug("do preparing...");
            this.prepareBuildsDir(runnerConfig);
            this.prepareOptions();
            String imageName = getImageName();
            buildLogger.println(String.format("Using Docker executor with image %s ...", imageName));

            this.connectDocker();
            this.createDependencies();
        } catch (Throwable t) {
            throw new ExecutorException("Docker 를 준비 중에 문제가 발생했습니다.", t);
        }
    }

    @Override
    public void doRun(ExecutorCommand executorCommand) throws ExecutorException, AbortedException {
        //svn 을 모든 이미지에 설치를 해야지만 DockerExecutor 에서 해당 빌드를 사용할 수 있으므로 비효율적이라 이렇게 직접 실행하는 구조로 변경.
        //나중에는 구성할 필요가 있겠음.
        //TODO 체크아웃이 너무 오래 걸리는 작업인 경우 취소를 처리하는데 시간이 조금 걸리지 않을까?
        if (executorCommand.getBuildStage() == BuildStage.GET_SOURCES) {
            try {
                String s = executeGetSource(executorCommand.getScript());
                //buildLogger.println(String.format("execute get source: %s", s));//TODO error message

            } catch (IOException e) {
                logger.error("빌드를 위해 소스 코드를 동기화하던 중 오류가 발생햇습니다.", e);
                buildLogger.println(e.getMessage());//TODO 오류로 기록
                throw new ExecutorException(e.getMessage());
            } catch (AbortedException e) {
                throw e;

            } catch (Throwable t) {
                logger.error("빌드를 위해 소스 코드를 동기화하던 중 오류가 발생햇습니다.", t);
                buildLogger.println(t.getMessage());//TODO 오류로 기록
                throw new ExecutorException(t.getMessage());
            }
        }
        else if (executorCommand.getBuildStage() == BuildStage.UPLOAD_ARTIFACTS) {
            /*
            Uploading artifacts...
sd.pdf: found 1 matching files
Uploading artifacts to coordinator... ok            id=25889650 responseStatus=201 Created token=tFa69qse
             */

            logger.info("Artifact 를 확인 후 저장합니다...");
            JobResponse jobResponse = shellScriptInfo.getJobResponse();

            if (!jobResponse.hasArtifacts()) {
                logger.info("Artifact 설정이 없어 Artifact 저장 단계를 건너뜁니다.");
            }
            else {
                //buildLogger 를 지우면 안 됩니다. 여기가 있어야 CI 서버 상의 취소 상황을 알 수 있습니다. TODO 매번 이렇게알려 줘야 하나?
                //buildLogger.info("Uploading artifacts...");
                buildLogger.info("\n빌드 결과물을 저장합니다.");
                try {
                    Artifact artifact = jobResponse.getArtifact();
                    logger.debug("저장할 Artifact 가 총 '{}' 개 있습니다. 압축을 시작합니다...", artifact.getPaths().size());

                    File artifactDir = ArtifactDirectoryHelper.createArtifactDirectory(runnerConfig.getArtifactsDir(), jobResponse, jobResponse.getId());

                    File artifactsArchiveFile = new File(artifactDir, String.format("%s.zip", artifact.getName()));
                    File buildDir = new File(shellScriptInfo.getBuild().getBuildDir());
                    logger.debug("현재 빌드 디렉터리는 '{}' 입니다.", buildDir.getAbsolutePath());

                    List<String> paths = ArtifactArchiver.paths(buildDir, artifact);
                    ArtifactArchiver.zip(buildDir, artifactsArchiveFile, paths);

                    logger.info("총 '{}' 개의 Artifact 저장을 완료했습니다. 파일 이름: {}", artifact.getPaths().size(), artifactsArchiveFile.getAbsolutePath());
                    buildLogger.println("Uploading artifacts to server... ok\n");
                    //Uploading artifacts to coordinator... ok            id=24078683 responseStatus=201 Created token=GGoPHtY-
                } catch (IOException e) {
                    buildLogger.error(e.getMessage()); //TODO 여기서 전체 메시지를 출력하고 다음 아래 Exception 이 출력되는 순서가 맞을까?
                    throw new ExecutorException(String.format("빌드 스테이지 '%s' 실행 중 오류가 발생했습니다.", executorCommand.getBuildStage()), e);
                }
            }
        }
        else if (executorCommand.getBuildStage() == BuildStage.DOWNLOAD_ARTIFACTS) {
            logger.debug("이전 Job 들에서 Artifacts 를 가져옵니다...");
            try {
                JobResponse jobResponse = shellScriptInfo.getJobResponse();
                List<Dependency> dependencies = jobResponse.getDependencies();
                if (!dependencies.isEmpty()) {
                    logger.debug("이전 Job 들에서 가져올 Artifacts 가 '{}' 개 있습니다. 현재 빌드 디렉터리에 복사합니다...", dependencies.size());
                    File buildDir = new File(shellScriptInfo.getBuild().getBuildDir());

                    for (Dependency dependency : dependencies) {
                        File artifactDir = ArtifactDirectoryHelper.createArtifactDirectory(runnerConfig.getArtifactsDir(), jobResponse, dependency.getId());
                        File artifactsArchiveFile = new File(artifactDir, dependency.getArtifactsFile().getFileName());

                        logger.debug("Artifact '{}' 을 '{}' 에 압축해제합니다...", artifactsArchiveFile.getAbsolutePath(), buildDir.getAbsolutePath());

                        if (artifactsArchiveFile.exists()) {
                            //압축을 풀어 빌드 디렉터리에 넣습니다.
                            ArtifactArchiver.unzip(buildDir, artifactsArchiveFile);
                            logger.debug("Job #{} ({}) 의 Artifacts 를 가져왔습니다.", dependency.getId(), dependency.getName());
                        }
                        else {
                            //TODO Artifact 파일이 없다면 오류를 던져야 하지 않겠나?
                            logger.warn("Artifacts 파일 '{}' 가 없어서 현재 빌드 디렉터리로 가져올 수 없습니다.", artifactsArchiveFile.getName());
                        }
                    }
                }
                else {
                    logger.debug("이전 Job 들에서 가져올 Artifacts 가 없습니다.");
                }
            } catch (IOException e) {
                throw new ExecutorException(String.format("빌드 스테이지 '%s' 실행 중 오류가 발생했습니다.", executorCommand.getBuildStage()), e);
            }
        }
        else {
            this.prepareOptions();

            String imageName = getImageName();

            //buildLogger.trace("Executing on", buildContainer.name(), "the", executorCommand.getScript());//TODO
            logger.debug("Executing on {} the '{}'", buildContainer.getId(), executorCommand.getScript());
            try {
                this.watchContainer(buildContainer.getId(), executorCommand.getScript());
            } catch (DockerException | ExecutionException | InterruptedException e) {
                logger.error("빌드 스테이지 '{}' 실행 중 오류가 발생했습니다.", executorCommand.getBuildStage(), e);
                //this.jobTrace.fail();
                throw new ExecutorException(String.format("빌드 스테이지 '%s' 실행 중 오류가 발생했습니다.", executorCommand.getBuildStage()), e);
            }
        }
    }

    @Override
    protected void doCleanup() {
        if (this.failures.size() > 0) {
            for (String containerId: failures) {
                try {
                    this.removeContainer(containerId);
                } catch (Throwable t) {
                    logger.warn("Removing unstable container ({}) failed.", containerId);
                }
            }

        }

        if (this.buildContainer != null) {
            try {
                this.removeContainer(buildContainer.getId());
            } catch (Throwable t) {
                logger.warn("Removing build container ({}) failed.", buildContainer.getId());
            }
        }

        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                //TODO ignore? e.printStackTrace();
            }
        }
    }

    @Override
    protected void doFinish() {
        //do nothing...
    }

    private void createDependencies() {
        this.bindDevices();

        buildLogger.debugln("Creating build volume...");
        this.createBuildVolume();

        logger.debug("Creating services...");
        createServices();

        logger.debug("Creating user-defined volumes...");
        createUserVolumes();


    }

    //TODO 디바이스를 연결해서 사용할 수 있도록 처리. 현재 버전에서는 디바이스 사용을 지원하지 않는다.
    private void bindDevices() {
        /*
        func (s *executor) bindDevices() (err error) {
	for _, deviceString := range s.Config.Docker.Devices {
		device, err := s.parseDeviceString(deviceString)
		if err != nil {
			err = fmt.Errorf("Failed to parse device string %q: %s", deviceString, err)
			return err
		}

		s.devices = append(s.devices, device)
	}
	return nil
}
         */
    }

    private void createBuildVolume() {
        // Cache Git sources:
        // take path of the projects directory,
        // because we use `rm -rf` which could remove the mounted volume
        //parentDir := path.Dir(s.Build.FullProjectDir())

        addVolume(shellScriptInfo.getBuild().getBuildDir());
    }

    private void addVolume(final String volume) {
        String[] volumeArray = volume.split(":");
        // /home/user/data:/data 와 같은 형식일 때 호스트 볼륨과 매핑한다는 의미
        if (volumeArray.length == 2) {
            addHostVolume(volumeArray[0], volumeArray[1]);
        }
        else if (volumeArray.length == 1) {
            //addCacheVolume(volumeArray[0]);
        }
    }

    private final void addHostVolume(String hostPath, String containerPath) {
        //containerPath = s.getAbsoluteContainerPath(containerPath)
        buildLogger.debugln("Using host-based", hostPath, "for", containerPath, "...");
        this.volumes.add(String.format("%s:%s", hostPath, containerPath));

    }

    /**
     * TODO 아직 Cache 가 어떤 역할을 하는지 잘 모르겠다. 여러 컨테이너가 동일한 Checkout 소스가 있다면 계속 받지 않으려고 하는거 같은데 동시에 여러 Job 이 돌아가게 되면 Checkout 시에도 문제가 되고
     * 빌드 시에도 파일 간에 충돌이 있을 수 있다. 일단 그냥 매번 돌리는 것으로 하자.
     *
     * GitLab 의 executor_docker.go 파일에서는 복잡한 구조를 띄고 있습니다. 만약 Cache Directory 가 설정되어 있지 않다면 Cache Volume Container 를
     * 생성하고 이것을 사용하는 것으로 자동 설정됩니다. 우리는 현재 버전에서 설정으로 처리하도록 구성합니다.
     *
     * @param containerPath
     */
    private final void addCacheVolume(final String containerPath) {

        /*
        // disable cache for automatic container cache, but leave it for host volumes (they are shared on purpose)
	if s.Config.Docker.DisableCache {
		s.Debugln("Container cache for", containerPath, " is disabled.")
		return nil
	}
         */
        String hash = DigestUtils.md5Hex(containerPath);
        String hostPath = String.format("%s/%s/%s", shellScriptInfo.getDockerConfig().getCacheDir(), shellScriptInfo.getBuild().getProjectUniqueName(), hash);
        buildLogger.debugln("Using path", hostPath, "as cache for", containerPath, "...");
        this.volumes.add(String.format("%s:%s", hostPath, containerPath));
        //s.binds = append(s.binds, fmt.Sprintf("%v:%v", filepath.ToSlash(hostPath), containerPath))

        this.hostCacheDir = hostPath;
    }

    private void createServices() {

    }

    /**
     * Runner 설정에 있는 Volume 정보를 가지고 구성합니다.
     *
     */
    private void createUserVolumes() {
        for (String volume: this.runnerConfig.getDocker().getVolumes())
            this.addVolume(volume);
    }

    /**
     * 빌드 디렉터리를 준비합니다. Docker 내부에 빌드 디렉터리를 구성하므로 경로는 뭐 그렇습니다.
     *
     * @param runnerConfig
     */
    private void prepareBuildsDir(RunnerConfig runnerConfig) {

        //TODO Build 클래스에서 하는 작업을 여기서 해야 하지 않나?
    }

    //TODO 마운트 여부 체크. 그냥 공유하고 있는 것인지를 알아봅니다. 빌드가 끝나면 로그를 다 보냈을 것이고,
    //Artifact 도 다 올려 두었을터이니 굳이 공유할 필요가 있을까요?
    private boolean isHostMountedVolume(final String dir, List<String> volumes) {
        boolean isParentOf = true;

        String[] hostVolume;
        for(String volume: volumes) {
            hostVolume = volume.split(":");
            if (hostVolume.length < 2)
                continue;


        }

        return false;

        /*
        func (s *executor) isHostMountedVolume(dir string, volumes ...string) bool {
	isParentOf := func(parent string, dir string) bool {
		for dir != "/" && dir != "." {
			if dir == parent {
				return true
			}
			dir = path.Dir(dir)
		}
		return false
	}

	for _, volume := range volumes {
		hostVolume := strings.Split(volume, ":")
		if len(hostVolume) < 2 {
			continue
		}

		if isParentOf(path.Clean(hostVolume[1]), path.Clean(dir)) {
			return true
		}
	}
	return false
}
         */
    }

    //TODO 이미지 이름 설정하기
    //TODO 서비스 할당하기
    private void prepareOptions() {
        this.options.put("image", shellScriptInfo.getJobResponse().getImage());

        /*
        s.options = dockerOptions{}
	s.options.Image = s.Build.Image.Name
	for _, service := range s.Build.Services {
		serviceName := service.Name
		if serviceName == "" {
			continue
		}

		s.options.Services = append(s.options.Services, serviceName)
	}
         */
    }

    //TODO allowed images
    //TODo default image
    private String getImageName() {
        if (options.containsKey("image")) {
            String imageName = (String)options.get("image");
            if (StringUtils.hasText(imageName)) {
                return imageName;
            }
        }

        return "";
        /*
        func (s *executor) getImageName() (string, error) {
	if s.options.Image != "" {
		image := s.Build.GetAllVariables().ExpandValue(s.options.Image)
		err := s.verifyAllowedImage(s.options.Image, "images", s.Config.Docker.AllowedImages, []string{s.Config.Docker.Image})
		if err != nil {
			return "", err
		}
		return image, nil
	}

	if s.Config.Docker.Image == "" {
		return "", errors.New("No Docker image specified to run the build in")
	}

	return s.Config.Docker.Image, nil
}
         */
    }

    /**
     * Docker Engine 에 접속합니다. 접속 후 정보도 세팅합니다.
     *
     * @throws DockerException
     * @throws InterruptedException
     */
    private void connectDocker() throws DockerException, InterruptedException {
        //TODO default to custom?
        DefaultDockerClientConfig config
                = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();

        this.dockerClient = DockerClientBuilder.getInstance(config).build();
        this.dockerInfo = this.dockerClient.infoCmd().exec();
    }

    private InspectContainerCmd createContainer(final String containerType, String imageName, String[] commands) throws ExecutorException, AbortedException {
        //Fetch Image
        CreateContainerResponse containerCreation = null;
        try {
            InspectImageResponse imageInfo = getDockerImage(imageName);
            buildLogger.println(String.format("Using docker image %s ID=%s for %s container...", imageName, imageInfo.getId(), containerType));

            this.hostName = shellScriptInfo.getDockerConfig().getHostname();
            if (!StringUtils.hasText(hostName)) {
                this.hostName = this.shellScriptInfo.getBuild().getProjectUniqueName();
            }

            String containerName = String.format("%s-%s", this.shellScriptInfo.getBuild().getProjectUniqueName(), containerType);

            // this will fail potentially some builds if there's name collision
            try {
                logger.debug("Removing previous created container '{}' ...", containerName);
                this.removeContainer(containerName);
                logger.debug("Previous created container '{}' was removed.", containerName);
            } catch (Throwable t) {
                //logger.warn("");
                //실패해도 무시합니다. 없는 것을 삭제할 수도 있습니다.
            }

            buildLogger.trace("Creating container", containerName, "...");

            HostConfig hostConfig = new HostConfig();
            Bind[] binds = new Bind[this.volumes.size()];
            for (int i = 0; i < this.volumes.size(); i++) {
                binds[i] = Bind.parse(this.volumes.get(i));
            }


            hostConfig.withCpusetCpus(shellScriptInfo.getDockerConfig().getCpusetCpus())
                    .withPrivileged(shellScriptInfo.getDockerConfig().getPrivileged())
                    .withVolumesFrom(new VolumesFrom[]{new VolumesFrom("tc-home")})
                    .withBinds(new Binds(binds))
                    .withLogConfig(new LogConfig(LogConfig.LoggingType.JSON_FILE));



            CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostName(hostName)
                    .withCmd(commands)
                    .withLabels(getLabels(containerType))
                    .withTty(false)
                    .withUser("teamcode")
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withStdinOpen(true)
                    .withStdInOnce(true)
                    .withHostConfig(hostConfig).exec();


            return dockerClient.inspectContainerCmd(createContainerResponse.getId());
        } catch (DockerException | InterruptedException e) {
            if (containerCreation != null) {
                this.failures.add(containerCreation.getId());
            }

            throw new ExecutorException(e);
        }
    }

    private InspectImageResponse getDockerImage(final String imageName) throws DockerException, InterruptedException {
        DockerPullPolicy pullPolicy = getShellScriptInfo().getDockerConfig().getPullPolicy();

        InspectImageResponse inspectImageResponse = dockerClient.inspectImageCmd(imageName).exec();

        // If never is specified then we return what inspect did return
        if (pullPolicy == DockerPullPolicy.NEVER) {
            return inspectImageResponse;
        }
        else {
            // Don't pull image that is passed by ID
            if (inspectImageResponse.getId().equals(imageName)) {
                return inspectImageResponse;
            }

            //s.Println("Using locally found image version due to if-not-present pull policy")
            if (pullPolicy == DockerPullPolicy.IF_NOT_PRESENT) {

            }
            else {
                pullDockerImage("");
            }
        }


        throw new DockerException(String.format("Docker 이미지 '%s' 정보를 찾을 수 없습니다.", imageName), HttpResponseStatus.NOT_FOUND.code());//TODO
    }

    /*func (s *executor) getDockerImage(imageName string) (*types.ImageInspect, error) {
        pullPolicy, err := s.Config.Docker.PullPolicy.Get()
        if err != nil {
            return nil, err
        }

        authConfig := s.getAuthConfig(imageName)

        s.Debugln("Looking for image", imageName, "...")
        image, _, err := s.client.ImageInspectWithRaw(context.TODO(), imageName)

        // If never is specified then we return what inspect did return
        if pullPolicy == common.PullPolicyNever {
            return &image, err
        }

        if err == nil {
            // Don't pull image that is passed by ID
            if image.ID == imageName {
                return &image, nil
            }

            // If not-present is specified
            if pullPolicy == common.PullPolicyIfNotPresent {
                s.Println("Using locally found image version due to if-not-present pull policy")
                return &image, err
            }
        }

        newImage, err := s.pullDockerImage(imageName, authConfig)
        if err != nil {
            return nil, err
        }
        return newImage, nil
    }*/


    //TODO 현재 버전에서는 새로 Pull 하지 않고 항상 있는 것을 사용한다.
    private void pullDockerImage(final String imageName) {
        return;
    }

    /*func (s *executor) pullDockerImage(imageName string, ac *types.AuthConfig) (*types.ImageInspect, error) {
        s.Println("Pulling docker image", imageName, "...")

        ref := imageName
        // Add :latest to limit the download results
        if !strings.ContainsAny(ref, ":@") {
            ref += ":latest"
        }

        options := types.ImagePullOptions{}
        if ac != nil {
            options.RegistryAuth, _ = docker_helpers.EncodeAuthConfig(ac)
        }

        if err := s.client.ImagePullBlocking(context.TODO(), ref, options); err != nil {
            if strings.Contains(err.Error(), "not found") {
                return nil, &common.BuildError{Inner: err}
            }
            return nil, err
        }

        image, _, err := s.client.ImageInspectWithRaw(context.TODO(), imageName)
        return &image, err
    }*/

    private void removeContainer(String containerId) throws DockerException, InterruptedException {
        //TODO s.disconnectNetwork(id)

        logger.debug("Removing container {} ...", containerId);
        dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
        buildLogger.trace("Removed container", containerId);

    }

    private Map<String, String> getLabels(String containerType, String... otherLabels) {
        Map<String, String> labels = new HashMap<>();
        labels.put(String.format("%s.job.id", RunnerConstants.DOCKER_LABEL_PREFIX), "s.build.id");//TODO
        labels.put(String.format("%s.project.id", RunnerConstants.DOCKER_LABEL_PREFIX),
                this.getShellScriptInfo().getJobResponse().getJobInfo().getProjectId().toString());

        return labels;
    }

    /*func (s *executor) getLabels(containerType string, otherLabels ...string) map[string]string {
        labels[dockerLabelPrefix+".job.id"] = strconv.Itoa(s.Build.ID)
        //labels[dockerLabelPrefix+".job.sha"] = s.Build.GitInfo.Sha
        //labels[dockerLabelPrefix+".job.before_sha"] = s.Build.GitInfo.BeforeSha
        //labels[dockerLabelPrefix+".job.ref"] = s.Build.GitInfo.Ref
        labels[dockerLabelPrefix+".project.id"] = strconv.Itoa(s.Build.JobInfo.ProjectID)
        labels[dockerLabelPrefix+".runner.id"] = s.Build.Runner.ShortDescription()
        labels[dockerLabelPrefix+".runner.local_id"] = strconv.Itoa(s.Build.RunnerID)
        labels[dockerLabelPrefix+".type"] = containerType
        for _, label := range otherLabels {
            keyValue := strings.SplitN(label, "=", 2)
            if len(keyValue) == 2 {
                labels[dockerLabelPrefix+"."+keyValue[0]] = keyValue[1]
            }
        }
    }*/

    private InspectImageCmd getPrebuiltImage() {
        String architecture = this.getArchitecture();
        /*TODO if architecture == "" {
            return nil, errors.New("unsupported docker architecture")
        }*/

        //imageName := prebuiltImageName + ":" + architecture + "-" + common.REVISION
        return null;
    }

    private String getArchitecture() {
        String architecture = dockerInfo.getArchitecture();

        switch(architecture) {
            case "armv6l":
            case "armv7l":
            case "aarch64":
                architecture = "arm";
                break;
            case "amd64":
                architecture = "x86_64";
                break;
        }

        if (StringUtils.hasText(architecture)) {
            return architecture;
        }
        else {
            //TODO system arch?
        }

        /*
        switch runtime.GOARCH {
	case "amd64":
		return "x86_64"
	default:
		return runtime.GOARCH
	}
         */

        return "";
    }

    //FIXME GitLab 의  executor_docker.go 에서는 attach 를 먼저 하고 start 를 하는데 무슨 의미인지 모르겠다.
    //TODO Attach 를 한다는 의미는 한번 만들어 두고 그것을 실행한다는 의미인가?
    private void watchContainer(final String containerId, final String scripts) throws DockerException, ExecutionException, InterruptedException {
        //logger.debug("Attaching to container '{}' ...", containerId);
        try {
            ExecutorService executor = Executors.newFixedThreadPool(3);

            logger.debug("Starting container '{}' ...", containerId);
            dockerClient.startContainerCmd(containerId).exec();

            //logger.debug("Waiting for attach to finish '{}' ...", containerId);

            // Exec command inside running container with attached STDOUT and STDERR

            // c 옵션은 뒤의 문자열을 명령어로 인식하고 실행해라는 의미입니다.
            final String[] command = {"bash", "-c", scripts};

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withCmd(command)
                    .withUser("teamcode")
                    .exec();

            //Detach 를 설정하면 LogStream 에서 값을 가져올 수 없다 (LogStream 을 Detach 한다는 의미이다).
            Callable<Boolean> execStartTask = () -> {
                //buildLogger.info(String.format("$ %s", scripts));
                dockerClient.execStartCmd(execCreateCmdResponse.getId())
                        .withDetach(false)
                        .withTty(true)
                        .exec(new ExecStartResultJobTraceCallback(jobTrace)).awaitCompletion();// No blocking... .awaitCompletion();

                return true;
            };
            Future<Boolean> execStartFuture = executor.submit(execStartTask);


            Callable<Boolean> waitForContainerTask = () -> {
                try {
                    waitForContainer(execStartFuture, containerId, execCreateCmdResponse.getId());
                    return Boolean.TRUE;
                } catch (BuildError e) {
                    logger.debug("컨테이너 내에서 빌드하던 중 오류가 발생했습니다. 원인: {}", e.getMessage());
                    return Boolean.FALSE;
                } catch (Throwable t) {
                    logger.debug("컨테이너 종료를 기다리던 중 오류가 발생했습니다. 원인: {}", t.getMessage());
                    //t.printStackTrace();
                    //TODO logging?
                    return Boolean.FALSE;
                }
            };
            Future<Boolean> waitFuture = executor.submit(waitForContainerTask);//타임아웃을 주지 않습니다. 끝날 때까지 끝난게 아니기 때문입니다.

            if (waitFuture.get().booleanValue()) {
                buildLogger.debugln("Container", containerId, "finished with normally", "");
            }
            else {
                buildLogger.debugln("Container", containerId, "finished with error", "");
                //this.jobTrace.fail();
                //TODO custom exception
                throw new DockerException("빌드 시 오류가 발생했습니다.", HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            }
        } finally {

        }

    }

    private void waitForContainer(final Future<Boolean> execStartFuture, final String containerId, final String execId) throws DockerException, ExecutionException, InterruptedException {
        logger.debug("Waiting for container '{}' ...", containerId);
        int retries = 0;

        while(true) {
            try {
                InspectExecResponse inspectExecResponse = dockerClient.inspectExecCmd(execId).exec();

                retries = 0;

                logger.debug("Container exec is running? {}, exit code? {}", inspectExecResponse.isRunning(), inspectExecResponse.getExitCode());

                //서버 측에서 취소했을 때 jobTrace.isFinished() 는 true 가 됨.
                if (!execStartFuture.isDone()) {
                    TimeUnit.SECONDS.sleep(2);//TODO 적절한 초를 계산해 보자.
                    //무조건 로그 메시지는 기다려야 합니다. 로그 메시지를 받는 중에도 Container 는 종료될 수 있습니다.
                    //TODO 만약 Abort 가 발생하면 종료될 것이므로 이렇게 해도 됩니다?
                    continue;
                }

                if (!jobTrace.isFinished() && inspectExecResponse.isRunning()) {
                    TimeUnit.SECONDS.sleep(2);//TODO 적절한 초를 계산해 보자.
                    continue;
                }

                if (inspectExecResponse.getExitCode() != null && inspectExecResponse.getExitCode() != 0) {//TODO null 의 의미? 성공?
                    logger.debug("container error occurred. exit code: {}", inspectExecResponse.getExitCode());
                    //TODO timeout to const
                    stopContainer(containerId);
                    throw new BuildError(String.format("exit code %s", inspectExecResponse.getExitCode()));
                } else {
                    stopContainer(containerId);

                    return;
                }
            } catch(BuildError e) {
                throw e;
            } catch (Throwable t) {
                logger.warn("Docker Container 실행 요청 후 응답을 기다리던 중 오류가 발생했습니다.", t);
                //if (retries > 3) {
                    stopContainer(containerId);
                    throw new ExecutionException(t);
                //}

                //retries++;
                //TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    private final String executeGetSource(final String script) throws IOException, InterruptedException, AbortedException {
        RepositoryInfo repositoryInfo = shellScriptInfo.getJobResponse().getRepositoryInfo();
        buildLogger.info("저장소를 체크아웃합니다..."); //en; Checking out repository...

        CommandLine cmdLine = CommandLine.parse("svn")
                //.addArgument("co")
                .addArgument("log")
                .addArgument(String.format("%s/@%s", repositoryInfo.getUrl(), repositoryInfo.getRevision()))
                .addArgument("--incremental")
                .addArgument("-l")
                .addArgument("1");

        String logMessage = executeCommand(cmdLine);
        buildLogger.println(logMessage);

        //checkout 에서 export 로 변경했습니다. 현재 아키텍처 상으로는 매번 Job 에서 Checkout 을 하므로 굳이 .svn 파일까지 끌고 올 필요가 없습니다.

        //FIXME 이미 받은 것은 다시 받을 필요가 없지 않나? force 옵션을 사용하는 것도 방법임
        File buildDirObject = new File(shellScriptInfo.getBuild().getBuildDir());
        if (buildDirObject.exists()) {
            FileUtils.deleteDirectory(buildDirObject);
            logger.debug("이전에 디렉터리가 이미 있어 삭제했습니다.");
        }

        cmdLine = CommandLine.parse("svn")
                //.addArgument("co")
                .addArgument("export")
                .addArgument(String.format("%s/@%s", repositoryInfo.getUrl(), repositoryInfo.getRevision()))
                //.addArgument(runnerConfig.getPipelinesWorkDir().getAbsolutePath())
                .addArgument(shellScriptInfo.getBuild().getBuildDir());
        //.addArgument(hostCacheDir);
        //return executeCommand(cmdLine);

        return executeCommand(cmdLine);
    }

    private static final String executeCommand(CommandLine commandLine) throws IOException, InterruptedException {
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();


        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(1);
        ExecOutputStream result = new ExecOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(result));

        //TODO 나중에 WatchDog
        //ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
        //0 이면 정상, 아니면 오류입니다.
        executor.execute(commandLine, resultHandler);

        resultHandler.waitFor();

        int exitCode = resultHandler.getExitValue();
        result.setExitCode(exitCode);



        logger.debug("종료 코드: {}", exitCode);
        //TODO streaming...
        StringBuilder builder = new StringBuilder();
        if (logger.isDebugEnabled()) {
            for (String line: result.getLines()) {
                builder.append(line);
                builder.append("\n");
                //logger.debug("-----> {}", line); //FIXME 전체를 Export 하므로 메시지가 매우 길게 나옵니다.
            }
        }

        if (exitCode != 0) {
            throw new IOException(builder.toString());
        }


        return builder.toString();
    }

    private final void stopContainer(final String containerId) throws DockerException, InterruptedException {
        logger.debug("컨테이너 {} 를 종료합니다...", containerId);
        dockerClient.stopContainerCmd(containerId).withTimeout(10).exec();
    }

}
