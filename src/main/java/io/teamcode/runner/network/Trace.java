package io.teamcode.runner.network;

/**
 * Created by chiang on 2017. 4. 27..
 */
public class Trace {

    public static Trace newJobTrace() {

        return null;
    }


    /*func newJobTrace(client common.Network, config common.RunnerConfig, jobCredentials *common.JobCredentials) *clientJobTrace {
        return &clientJobTrace{
            client:         client,
                    config:         config,
                    jobCredentials: jobCredentials,
                    id:             jobCredentials.ID,
                    abortCh:        make(chan interface{}),
        }
    }*/
}
