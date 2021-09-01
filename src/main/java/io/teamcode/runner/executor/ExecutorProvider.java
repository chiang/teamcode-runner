package io.teamcode.runner.executor;

/**
 * Created by chiang on 2017. 5. 5..
 */
public interface ExecutorProvider {

    boolean canCreate();

    Executor getExecutor();
}
