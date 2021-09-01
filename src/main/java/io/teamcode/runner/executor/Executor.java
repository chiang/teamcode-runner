package io.teamcode.runner.executor;

import io.teamcode.runner.AbortedException;
import io.teamcode.runner.common.ShellScriptInfo;
import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.shell.Shell;

/**
 * Created by chiang on 2017. 4. 27..
 */
public interface Executor {

    ShellScriptInfo getShellScriptInfo();

    Shell getShell();

    void prepare(RunnerConfig runnerConfig) throws ExecutorException, AbortedException;

    void run(ExecutorCommand executorCommand) throws ExecutorException, AbortedException;

    void cleanup();

    void finish();

    /*func (e *machineExecutor) Finish(err error) {
        if e.executor != nil {
            e.executor.Finish(err)
        }
        e.log().Infoln("Finished docker-machine build:", err)
    }*/


}
