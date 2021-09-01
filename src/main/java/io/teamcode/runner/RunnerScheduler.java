package io.teamcode.runner;

import io.teamcode.runner.config.RunnerConfig;
import io.teamcode.runner.network.NetworkClient;
import io.teamcode.runner.network.model.JobResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO naming
 *
 * 주기적으로 Job 을 요청해서 처리하는 ...
 *
 * Created by chiang on 2017. 4. 29..
 */
@Component
public class RunnerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RunnerScheduler.class);

    private ExecutorService executorService =
            new ThreadPoolExecutor(100, 200, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());

    private Map<Long, Build> jobs = new ConcurrentHashMap<>();

    private Lock lock = new ReentrantLock();

    @Autowired
    RunnerConfig runnerConfig;

    @Autowired
    NetworkClient networkClient;

    //TODO 만약 네트워크 오류가 발생하면 계속 시도해야 하지 않나?
    @PostConstruct
    public void init() {
        logger.debug("Initializing...");

        networkClient.registerRunner();
    }

    /**
     * 이전 작업 완료를 기다리지 않고 바로 해당 밀리세컨즈 후에 실행된다.
     */
    //@Scheduled(fixedRate = 5000)
    @Scheduled(fixedRateString = "${runners.interval}")
    public void startWorkers() {
        //TODO for mr.stopSignal == nil {

        if (runnerConfig.isWorkerEnabled()) {
            Worker worker = new Worker(runnerConfig, networkClient);
            Future<String> future = executorService.submit(worker);
        }
    }

    //Multi threaded
    private void process() {
        JobResponseEntity jobResponseEntity = networkClient.requestJob();

        //TODO NOT_FOUND 는 걸러내야 한다.
        if (!jobResponseEntity.isSuccess()) {
            logger.error("Failed to request job: runner requestConcurrency meet");
        }
        else {
            networkClient.processJob();

            try {
                lock.lock();
                Build build = null;
                if (!jobs.containsKey(jobResponseEntity.getJobResponse().getId())) {
                    //build = new Build();
                    build.setJobResponse(jobResponseEntity.getJobResponse());

                    jobs.put(build.getJobResponse().getId(), build);
                }
                else {
                    //TODO warn?
                }
                lock.unlock();

                if (build != null)
                    build.run();
            } finally {
                jobs.remove(jobResponseEntity.getJobResponse().getId());
            }
        }

    }


}
