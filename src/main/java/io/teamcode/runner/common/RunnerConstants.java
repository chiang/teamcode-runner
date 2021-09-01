package io.teamcode.runner.common;

import java.util.concurrent.TimeUnit;

/**
 * Created by chiang on 2017. 4. 29..
 */
public abstract class RunnerConstants {

    public static final long UPDATE_INTERVAL = 3 * 1000;

    public static final long UPDATE_RETRY_INTERVAL_SECONDS = 3;

    /**
     * 한번 JobTrace 에 쓸 때 기본 제한 크기. kb 단위. 4MB
     *
     */
    public static final int DEFAULT_OUTPUT_LIMIT = 4096; // 4MB in kilobytes

    public static final int DEFAULT_GET_SOURCES_ATTEMPTS = 1;

    public static final String DOCKER_LABEL_PREFIX = "io.teamcode.runner";

}
