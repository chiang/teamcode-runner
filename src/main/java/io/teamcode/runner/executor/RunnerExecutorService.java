package io.teamcode.runner.executor;

import io.teamcode.runner.RunnerScheduler;
import io.teamcode.runner.executor.docker.DockerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chiang on 2017. 5. 5..
 */
public class RunnerExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(RunnerExecutorService.class);

    private Map<String, ExecutorProvider> executorProviders = new HashMap<>();

    public void registerExecutors() {
        /*executorProviders.put("docker", new DefaultExecutorProvider(){

            @Override
            public Executor getExecutor() {
                return new DockerExecutor();
            }
        });*/
    }

    public void registerExecutor(String name, ExecutorProvider executorProvider) {
        logger.debug("Registering {} executor...", name);

        if (executorProviders.containsKey(name)) {
            logger.warn("Executor already exist: {}", name);
        }
        else {
            executorProviders.put(name, executorProvider);
        }
    }

    /*
    func RegisterExecutor(executor string, provider ExecutorProvider) {
	log.Debugln("Registering", executor, "executor...")

	if executors == nil {
		executors = make(map[string]ExecutorProvider)
	}
	if _, ok := executors[executor]; ok {
		panic("Executor already exist: " + executor)
	}
	executors[executor] = provider
}
     */


    public ExecutorProvider getExecutorProvider(String providerName) {

        return this.executorProviders.get(providerName);
    }

    /*
    func GetExecutor(executor string) ExecutorProvider {
	if executors == nil {
		return nil
	}

	provider, _ := executors[executor]
	return provider
}
     */
}
