package io.teamcode.runner.executor;

/**
 * Created by chiang on 2017. 5. 5..
 */
public class DefaultExecutorProvider implements ExecutorProvider {
    @Override
    public boolean canCreate() {
        return false;
    }

    @Override
    public Executor getExecutor() {
        return null;
    }
}
