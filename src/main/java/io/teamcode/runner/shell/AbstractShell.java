package io.teamcode.runner.shell;

import io.teamcode.runner.Runner;
import io.teamcode.runner.common.BuildStage;
import io.teamcode.runner.common.Dependency;
import io.teamcode.runner.common.JobVariable;
import io.teamcode.runner.common.ShellScriptInfo;
import io.teamcode.runner.network.model.RepositoryInfo;
import io.teamcode.runner.network.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chiang on 2017. 5. 6..
 */
public abstract class AbstractShell implements Shell {

    public static final Logger logger = LoggerFactory.getLogger(AbstractShell.class);

    protected String name;

    private Map<String, Shell> shells = new HashMap<>();

    public AbstractShell(String name) {
        this.name = name;
    }

    @Override
    public String generateShellScript(BuildStage buildStage, ShellScriptInfo shellScriptInfo) throws IOException {
        /*Shell shell = getShell(shellScriptInfo.getShell());
        if (shell == null) {
            throw new IllegalArgumentException(String.format("shell %s not found", shellScriptInfo.getShell()));//TODO custom error
        }

        return shell.generateShellScript(buildStage, shellScriptInfo);*/
        return this.generateScript(buildStage, shellScriptInfo);
    }

    private Shell getShell(final String shellName) {
        if (this.shells.isEmpty()) {
            return null;
        }

        return shells.get(shellName);
    }

    protected void writeScript(ShellWriter shellWriter, BuildStage buildStage, ShellScriptInfo shellScriptInfo) {

        switch(buildStage) {
            case PREPARE:
                this.writePrepareScript(shellWriter, shellScriptInfo);
                break;

            case GET_SOURCES:
                this.writeGetSourcesScript(shellWriter, shellScriptInfo);
                break;

            case RESTORE_CACHE:
                this.writeRestoreCacheScript(shellWriter);
                break;

            case DOWNLOAD_ARTIFACTS:
                this.writeDownloadArtifactsScript(shellWriter, shellScriptInfo);
                break;

            case USER_SCRIPT:
                this.writeUserScript(shellWriter, shellScriptInfo);
                break;

            case AFTER_SCRIPT:
                this.writeAfterScript(shellWriter);
                break;

            case ARCHIVE_CACHE:
                this.writeArchiveCacheScript(shellWriter);
                break;

            case UPLOAD_ARTIFACTS:
                this.writeUploadArtifactsScript(shellWriter);
                break;

            default:
                throw new UnsupportedOperationException(String.format("Not supported script type: %s", buildStage));
        }


    }

    private void writePrepareScript(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        if (StringUtils.hasText(shellScriptInfo.getBuild().getHostname())) {
            shellWriter.line(String.format("echo \"Running on $(hostname) via %s...\"", shellScriptInfo.getBuild().getHostname()));
        }
        else {
            shellWriter.line("echo \"Running on $(hostname)...\"");
        }

        this.writeTurnOffGradleDaemon(shellWriter);
    }

    private void writeRestoreCacheScript(ShellWriter shellWriter) {
        return;//TODO
    }

    /**
     * Step 의 <code>script</code> 블록 (배열 형태) 정보를 가지고 Script 를 작성합니다.
     *
     * @param shellWriter
     * @param shellScriptInfo
     */
    private void writeUserScript(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        Step currentStep = null;
        for (Step step: shellScriptInfo.getJobResponse().getSteps()) {
            if (step.getName().equals("script")) {
                currentStep = step;
                break;
            }
        }

        if (currentStep == null)
            return;

        this.writeExports(shellWriter, shellScriptInfo);
        this.writeCdBuildDir(shellWriter, shellScriptInfo);

        //TODO 아직 지원하지 않음
        /*
        if info.PreBuildScript != "" {
		b.writeCommands(w, info.PreBuildScript)
	}
         */

        String commands = String.join("\n", currentStep.getScripts());
        this.writeCommands(shellWriter, commands, true);

        //TODO 아직 지원하지 않음
        /*
        if info.PostBuildScript != "" {
		b.writeCommands(w, info.PostBuildScript)
	}
         */

    }

    // Write the given string of commands using the provided ShellWriter object.
    private void writeCommands(ShellWriter shellWriter, String commands, boolean xtrace) {
        String[] commandArray = commands.trim().split("\n");
        for (String command: commandArray) {
            if (!StringUtils.hasText(command)) {
                shellWriter.notice("$ %s", command);
                shellWriter.notice(String.format("$ %s", command));
            }
            else {
                shellWriter.emptyLine();
            }

            shellWriter.line(command);
            //w.CheckForErrors()
        }
    }

    private void writeAfterScript(ShellWriter shellWriter) {

    }

    private void writeArchiveCacheScript(ShellWriter shellWriter) {
        //TODO
    }

    private void writeUploadArtifactsScript(ShellWriter shellWriter) {
        //TODO
    }

    /**
     * Checkout
     *
     * @param shellWriter
     * @param shellScriptInfo
     */
    private void writeGetSourcesScript(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        this.writeCheckoutCommand(shellWriter, shellScriptInfo, shellScriptInfo.getBuild().getBuildDir());

        //this.writeExports(shellWriter, shellScriptInfo);

        /*String fullProjectDir = shellScriptInfo.getBuild().getFullProjectDir();
        Path fullProjectDirPath = Paths.get(fullProjectDir);
        File fullProjectDirPathDir = fullProjectDirPath.toFile();
        if (fullProjectDirPathDir.exists() && (new File(fullProjectDirPathDir, ".svn")).exists()) {
            //TODO svn update
        }
        else {
            this.writeCheckoutCommand(shellWriter, shellScriptInfo, fullProjectDir);
        }*/
    }

    /*func (b *AbstractShell) writeGetSourcesScript(w ShellWriter, info common.ShellScriptInfo) (err error) {
        b.writeExports(w, info)
        b.writeTLSCAInfo(w, info.Build, "GIT_SSL_CAINFO")

        if info.PreCloneScript != "" && info.Build.GetGitStrategy() != common.GitNone {
            b.writeCommands(w, info.PreCloneScript)
        }

        if err := b.writeCloneFetchCmds(w, info); err != nil {
            return err
        }

        if err = b.writeSubmoduleUpdateCmds(w, info); err != nil {
            return err
        }

        return nil
    }*/

    //gitlab <-- writeCloneFetchCmds
    private final void writeCheckoutCommand(final ShellWriter shellWriter, ShellScriptInfo shellScriptInfo, final String projectDir) {
        RepositoryInfo repositoryInfo = shellScriptInfo.getJobResponse().getRepositoryInfo();
        //TODO get revision
        shellWriter.notice("Checking out r%s ...", "231");
        shellWriter.command("svn", "co",
                String.format("%s@%s", repositoryInfo.getUrl(), repositoryInfo.getRevision()), projectDir);
        shellWriter.cd(projectDir);

        //.Notice("Checking out %s as %s...", build.GitInfo.Sha[0:8], build.GitInfo.Ref)
        //w.Command("git", "checkout", "-f", "-q", build.GitInfo.Sha)
    }

    /**
     * 다운로드라고 하지만 실제로는 그냥 가져오는 것. 나중에는 다운로드도 있겠지요.
     *
     * @param shellWriter
     * @param shellScriptInfo
     */
    private void writeDownloadArtifactsScript(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        this.writeExports(shellWriter, shellScriptInfo);
        this.writeCdBuildDir(shellWriter, shellScriptInfo);
        //b.writeTLSCAInfo(w, info.Build, "CI_SERVER_TLS_CA_FILE")


        // Process all artifacts
        this.downloadAllArtifacts(shellWriter, shellScriptInfo);
    }

    /**
     * 현재 Job 에서 사용할 Variables 를 모두 Shell 에 Export 합니다.
     *
     * @param shellWriter
     * @param shellScriptInfo
     */
    private void writeExports(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        List<JobVariable> jobVariables = shellScriptInfo.getBuild().getAllVariables();
        logger.debug("Export 할 Variable 이 총 '{}' 개 있습니다. Variable 이름 목록: {}", jobVariables.size(), jobVariables.stream().map(j -> j.getName()).collect(Collectors.toList()));

        for (JobVariable jobVariable: jobVariables) {
            shellWriter.variable(jobVariable);
        }
    }

    /**
     *
     * Gradle Home 디렉터리로 이동합니다. 없다면 만들고 들어갑니다. 운영체제 별 Gradle Home 은 아래와 같습니다.
     *
     * <ul>
     *     <li>C:\Users\<username> (Windows Vista & 7+)</li>
     *     <li>/Users/<username> (Mac OS X)</li>
     *     <li>/home/<username> (Linux)</li>
     * </ul>
     *
     * 보통 Gradle 을 사용할 때는 Daemon 을 사용하는 것이 빠르지만 TeamCode CI 에서는 1 Job == 1 Container 구조이며 동시에 여러 번
     * Gradle 을 실행하는 경우가 별로 없습니다. 또한 아래 링크를 보면 Gradle 에서도 CI 에서는 사용하지 않는 것을 권장합니다.
     *
     * 실행 시 <code>--no-daemon</code> 를 줘서 실행할 수 있지만 이걸 매번 가이드하는 것도 적절하지 않으니 이렇게 처리합니다.
     *
     * @see https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:stopping_an_existing_daemon
     * @param shellWriter
     */
    private void writeTurnOffGradleDaemon(ShellWriter shellWriter) {
        shellWriter.cd();
        shellWriter.ifDirectory("./.gradle");
        shellWriter.cd("./.gradle");
        shellWriter.elze();
        shellWriter.mkDir("./.gradle");
        shellWriter.cd("./.gradle");
        shellWriter.endIf();

        shellWriter.ifFile("./gradle.properties");
        shellWriter.emptyLine();
        shellWriter.elze();
        shellWriter.newFile("gradle.properties", "org.gradle.daemon=false");
        shellWriter.endIf();
    }

    private void writeCdBuildDir(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        //shellWriter.cd(shellScriptInfo.getBuild().getBuildDir());
        shellWriter.cd(shellScriptInfo.getBuild().getBuildDirInContainer());
    }

    private void downloadAllArtifacts(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        List<Dependency> dependencies = jobArtifacts(shellScriptInfo);
        if (dependencies.isEmpty())
            return;

        logger.debug("총 '{}' 개의 Artifact 를 현재 Job 의 Workspace 에 복사합니다...", dependencies.size());

        this.guardRunnerCommand(shellWriter, shellScriptInfo.getRunnercommand(), "Artifact downloading", () -> {
            for(Dependency dependency: dependencies) {
                downloadArtifacts(shellWriter, dependency, shellScriptInfo);
            }
        });
    }

    /*
    func (b *AbstractShell) downloadAllArtifacts(w ShellWriter, info common.ShellScriptInfo) {
	otherJobs := b.jobArtifacts(info)
	if len(otherJobs) == 0 {
		return
	}

	b.guardRunnerCommand(w, info.RunnerCommand, "Artifacts downloading", func() {
		for _, otherJob := range otherJobs {
			b.downloadArtifacts(w, otherJob, info)
		}
	})
}
     */

    private void downloadArtifacts(ShellWriter shellWriter, Dependency dependency, ShellScriptInfo shellScriptInfo) {



        //reference abstract.go
        /*args := []string{
            "artifacts-downloader",
                    "--url",
                    info.Build.Runner.URL,
                    "--token",
                    job.Token,
                    "--id",
                    strconv.Itoa(job.ID),
        }

        w.Notice("Downloading artifacts for %s (%d)...", job.Name, job.ID)
        w.Command(info.RunnerCommand, args...)*/
    }


    private List<Dependency> jobArtifacts(ShellScriptInfo shellScriptInfo) {
        List<Dependency> dependencies = new ArrayList<>();


        return dependencies;
    }

    /*
    func (b *AbstractShell) jobArtifacts(info common.ShellScriptInfo) (otherJobs []common.Dependency) {
	for _, otherJob := range info.Build.Dependencies {
		if otherJob.ArtifactsFile.Filename == "" {
			continue
		}

		otherJobs = append(otherJobs, otherJob)
	}
	return
}
     */

    private void guardRunnerCommand(ShellWriter shellWriter, String runnerCommand, String action, RunnerFunction runnerFunction) {
        if (!StringUtils.hasLength(runnerCommand)) {
            //w.Warning("%s is not supported by this executor.", action)
            return;
        }

        shellWriter.ifCmd(runnerCommand, "--version");
        runnerFunction.invoke();
        shellWriter.elze();
        shellWriter.warning("Missing %s. %s is disabled.", runnerCommand, action);
        shellWriter.endIf();

    }

    interface RunnerFunction {

        void invoke();
    }

    /*func (b *AbstractShell) downloadAllArtifacts(w ShellWriter, info common.ShellScriptInfo) {
        otherJobs := b.jobArtifacts(info)
        if len(otherJobs) == 0 {
            return
        }

        b.guardRunnerCommand(w, info.RunnerCommand, "Artifact downloading", func() {
            for _, otherJob := range otherJobs {
                b.downloadArtifacts(w, otherJob, info)
            }
        })
    }

    func (b *AbstractShell) downloadArtifacts(w ShellWriter, job common.Dependency, info common.ShellScriptInfo) {
        args := []string{
            "artifacts-downloader",
                    "--url",
                    info.Build.Runner.URL,
                    "--token",
                    job.Token,
                    "--id",
                    strconv.Itoa(job.ID),
        }

        w.Notice("Downloading artifacts for %s (%d)...", job.Name, job.ID)
        w.Command(info.RunnerCommand, args...)
    }*/

    /*func (b *AbstractShell) jobArtifacts(info common.ShellScriptInfo) (otherJobs []common.Dependency) {
        for _, otherJob := range info.Build.Dependencies {
            if otherJob.ArtifactsFile.Filename == "" {
                continue
            }

            otherJobs = append(otherJobs, otherJob)
        }
        return
    }*/

    /*func (b *AbstractShell) guardRunnerCommand(w ShellWriter, runnerCommand string, action string, f func()) {

        w.IfCmd(runnerCommand, "--version")
        f()
        w.Else()
        w.Warning("Missing %s. %s is disabled.", runnerCommand, action)
        w.EndIf()
    }*/

    private void uploadArtifacts(ShellWriter shellWriter, ShellScriptInfo shellScriptInfo) {
        /*if info.Build.Runner.URL == "" {
            return
        }*/


    }

}
