package io.teamcode.runner.config;

import io.teamcode.runner.Runner;
import io.teamcode.runner.RunnerException;
import org.apache.catalina.session.StandardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Created by chiang on 2017. 4. 27..
 */
@Configuration
public class RunnerConfig {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    @Value("${container.port}")
    private Integer serverPort;

    @Value("${runners.user}")
    private String user;

    /**
     * TeamCode Server URL
     *
     */
    @Value("${runners.url}")
    private String url;

    @Value("${runners.token}")
    private String token;

    @Value("${runners.pipelines-work-dir}")
    private String pipelinesWorkDirPath;

    //TODO Container 내부 경로와 외부 Host 상의 경로간에 중간 경로가 하드코딩되어 있으므로 이를 관리해야 함.
    private String pipelinesWorkDirPathInContainer;

    /**
     * 이 값은 TC Server 의 설정과 동일해야 합니다. 또한 Volume 으로 연결되어 있어야 합니다.
     *
     */
    @Value("${runners.artifacts-dir}")
    private String artifactsDirPath;

    @Value("${runners.worker.enabled}")
    private boolean workerEnabled;

    @Value("${runners.trace.watch}")
    private boolean watchEnabled;

    private File artifactsDir;

    private File workingCopyDir;

    /**
     * 파이프라인 작업 디렉터리 (Root path) 입니다.
     */
    private File pipelinesWorkDir;

    private RunnerCredentials runnerCredentials;

    @Autowired
    private DockerConfig docker;

    /**
     * Define active checking interval of jobs
     * 'defines in seconds how often to check GitLab for a new builds'
     */
    private int checkInterval;

    @PostConstruct
    public void init() {
        this.runnerCredentials = new RunnerCredentials();
        this.runnerCredentials.setUrl(url);
        this.runnerCredentials.setToken("");

        initBuildsDir();
        initArtifactsDir();
    }

    /**
     * 빌드 디렉터리를 체크하고 기본 디렉터리를 생성합니다. 설정에 있는 디렉터리는 이미 존재하는 디렉터리여야 합니다.
     *
     */
    private void initBuildsDir() {
        this.pipelinesWorkDir = new File(this.pipelinesWorkDirPath);
        if (!this.pipelinesWorkDir.exists()) {
            throw new RunnerException(String.format("파이프라인 작업 디렉터리 '%s' 가 없어 Runner 를 실행할 수 없습니다.", pipelinesWorkDir));
        }

        /*TODO this.pipelinesWorkDir = new File(buildsRootDir, "pipelines");
        if (!this.pipelinesWorkDir.exists()) {
            logger.info("빌드 Working 디렉터리 내 파이프라인 디렉터리가 없어 새로 생성합니다...");
            if (!this.pipelinesWorkDir.mkdir()) {
                throw new RunnerException(String.format("'%s' 디렉터리를 생성할 수 없습니다.", this.pipelinesWorkDir.getAbsolutePath()));
            }
        }*/
    }

    /**
     * Artifact 디렉터리가 있는지 확인하고 설정합니다. 이 디렉터리를 미리 생성이 되어 있어야 합니다.
     *
     */
    private void initArtifactsDir() {
        this.artifactsDir = new File(this.artifactsDirPath);
        if (!artifactsDir.exists()) {
            throw new RunnerException(String.format("Artifact 디렉터리 '%s' 가 없어 Runner 를 실행할 수 없습니다.", this.artifactsDirPath));
        }

        logger.info("빌드 Artifact 를 저장하는 경로는 '{}' 입니다.", this.artifactsDir.getAbsolutePath());
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();

        factory.setProtocol("HTTP/1.1");
        factory.setPort(serverPort);

        //disable session persist
        factory.addContextCustomizers((TomcatContextCustomizer) context -> {
            if (context.getManager() instanceof StandardManager) {
                ((StandardManager) context.getManager()).setPathname("");
            }
        });

        //FIXME runtime error factory.setUriEncoding("UTF-8");

        return factory;
    }

    public String getToken() {
        return this.token;
    }

    public String getShortDescription() {
        //ShortenToken
        if (this.getToken().length() > 8) {

            return getToken().substring(0, 8);
        }
        else {
            return getToken();
        }
    }

    public RunnerCredentials getRunnerCredentials() {

        return this.runnerCredentials;
    }

    /**
     *
     *
     * @return
     */
    public int getCheckInterval() {
        if (this.checkInterval > 0) {
            return checkInterval * 1000;
        }
        else {
            return 5000;//TODO default
        }
    }

    public String getArtifactsDirPath() {

        return this.artifactsDirPath;
    }

    public File getArtifactsDir() {
        return this.artifactsDir;
    }

    public File getPipelinesWorkDir() {

        return this.pipelinesWorkDir;
    }

    public DockerConfig getDocker() {
        return this.docker;
    }

    public String getUser() {
        return this.user;
    }

    public boolean isWorkerEnabled() {

        return this.workerEnabled;
    }

    public boolean isWatchEnabled() {

        return this.watchEnabled;
    }
}
