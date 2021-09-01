package io.teamcode.runner.network;

import io.teamcode.runner.common.JobState;
import io.teamcode.runner.common.UpdateState;
import io.teamcode.runner.network.model.JobResponse;
import io.teamcode.runner.network.model.JobResponseEntity;
import io.teamcode.runner.trace.TracePatch;

/**
 * Created by chiang on 2017. 4. 27..
 */
public interface Network {

    void registerRunner();

    //UpdateJob(config RunnerConfig, jobCredentials *JobCredentials, id int, state JobState, trace *string) UpdateState

    JobResponseEntity requestJob();

    /**
     *
     * @param jobCredentials
     * @param jobState
     * @param trace <code>null</code> 일 수 있습니다. 상태만 업데이트하는 경우가 있습니다.
     * @return
     */
    UpdateState updateJob(JobCredentials jobCredentials, JobState jobState, String trace);

    UpdateState patchTrace(JobCredentials jobCredentials, TracePatch tracePatch);

}
