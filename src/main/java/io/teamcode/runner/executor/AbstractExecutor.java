package io.teamcode.runner.executor;

import io.teamcode.runner.AbortedException;
import io.teamcode.runner.BuildLogger;
import io.teamcode.runner.common.BuildStage;
import io.teamcode.runner.common.ExecutorStage;
import io.teamcode.runner.common.RunnerConstants;
import io.teamcode.runner.common.ShellScriptInfo;
import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.core.ApplicationContextProvider;
import io.teamcode.runner.network.model.RepositoryInfo;
import io.teamcode.runner.shell.Shell;
import io.teamcode.runner.shell.ShellConfiguration;
import io.teamcode.runner.shell.ShellProvider;
import io.teamcode.runner.trace.JobTrace;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by chiang on 2017. 4. 27..
 */
public abstract class AbstractExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(Executor.class);

    protected RunnerConfig runnerConfig;

    protected BuildLogger buildLogger;

    protected ExecutorStage currentStage;

    protected ShellScriptInfo shellScriptInfo;

    protected ShellConfiguration shellConfiguration;

    protected Shell shell;

    protected JobTrace jobTrace;

    public AbstractExecutor(RunnerConfig runnerConfig, ShellScriptInfo shellScriptInfo, JobTrace jobTrace) {
        this.runnerConfig = runnerConfig;
        this.shellScriptInfo = shellScriptInfo;
        this.jobTrace = jobTrace;
        this.shell = ApplicationContextProvider.getBean("shellProvider", ShellProvider.class).getShell(shellScriptInfo.getShell());
    }


    protected abstract void doPrepare(RunnerConfig runnerConfig) throws ExecutorException, AbortedException;

    protected abstract void doCleanup();

    protected abstract void doRun(ExecutorCommand executorCommand) throws ExecutorException, AbortedException;

    protected abstract void doFinish();

    @Override
    public Shell getShell() {
        return this.shell;
    }

    @Override
    public ShellScriptInfo getShellScriptInfo() {
        return this.shellScriptInfo;
    }

    @Override
    public void prepare(RunnerConfig runnerConfig) throws ExecutorException, AbortedException {
        buildLogger = BuildLogger.newBuildLogger(jobTrace);
        this.startBuild();
        this.updateShell();
        this.generateShellConfiguration();

        doPrepare(runnerConfig);
    }

    @Override
    public void run(ExecutorCommand executorCommand) throws ExecutorException, AbortedException {
        doRun(executorCommand);
    }

    @Override
    public void cleanup() {
        doCleanup();

        setCurrentStage(ExecutorStage.CLEANUP);
    }

    @Override
    public void finish() {
        doFinish();

        logger.info("Finished docker-machine build: ");
        //e.log().Infoln("Finished docker-machine build:", err)
    }

    protected void setCurrentStage(ExecutorStage executorStage) {
        this.currentStage = executorStage;
    }

    private void generateShellConfiguration() {
        this.shellConfiguration = shell.getShellConfiguration(shellScriptInfo);
    }

    //TODO
    private void startBuild() {

    }

    //TODO
    private void updateShell() {

    }


}
