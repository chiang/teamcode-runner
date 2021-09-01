package io.teamcode.runner;

import io.teamcode.runner.common.AnsiColors;
import io.teamcode.runner.common.BuildLoggerType;
import io.teamcode.runner.executor.docker.DockerExecutor;
import io.teamcode.runner.trace.JobTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Created by chiang on 2017. 5. 8..
 */
public class BuildLogger {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    private JobTrace jobTrace;

    public static final BuildLogger newBuildLogger(final JobTrace jobTrace) {

        return new BuildLogger(jobTrace);
    }

    private BuildLogger(final JobTrace jobTrace) {
        this.jobTrace = jobTrace;
    }

    public void debugln(String... arguments) {
        logger.debug(String.join(" ", arguments));
    }

    public void trace(String message, String... arguments) {
        logger.trace(message, arguments);
    }

    public void sendCancel() {
        try {
            this.sendLog(BuildLoggerType.ERROR, "Job Failed: canceled");
            //ERROR: Job failed: canceled
        } catch (AbortedException e) {
            //무시합니다. 그냥 무조건...
        }
    }

    public void println(String message) throws AbortedException {
        try {
            this.sendLog(null, AnsiColors.ANSI_CLEAR, message);
        } catch (AbortedException e) {
            logger.warn("Job 이 취소되었습니다.");
            this.sendCancel();
        }
    }

    public final void info(final String message, final String... arguments) throws AbortedException {
        sendLog(BuildLoggerType.INFO, message, arguments);
    }

    public final void error(final String message, final String... arguments) throws AbortedException {
        sendLog(BuildLoggerType.ERROR, message, arguments);
    }

    private final void sendLog(BuildLoggerType buildLoggerType, String message, String... arguments) throws AbortedException {
        StringBuilder messageBuilder = new StringBuilder();

        if (buildLoggerType != null) {
            switch (buildLoggerType) {
                case INFO:
                    messageBuilder.append(AnsiColors.ANSI_BOLD_GREEN);
                    break;

                case ERROR:
                    messageBuilder.append(AnsiColors.ANSI_BOLD_RED);
                    break;

                case WARN:
                    messageBuilder.append(AnsiColors.ANSI_BOLD_YELLOW);
                    break;
            }
        }
        messageBuilder.append(message);

        messageBuilder.append(String.join(" ", arguments));
        if (buildLoggerType != null)
            messageBuilder.append(AnsiColors.ANSI_RESET);
        messageBuilder.append("\r\n");
        //String message = String.format("%s%s%s", prefix, , AnsiColors.ANSI_RESET);

        try {
            jobTrace.process(ByteBuffer.wrap(messageBuilder.toString().getBytes("UTF-8")));//TODO 그냥 String 을 전달, 처리하는 것을 만들 필요기 있겠다.
        } catch (UnsupportedEncodingException e) {
            //TODO 이런 일은 없다! e.printStackTrace();
        }

        if (buildLoggerType == BuildLoggerType.DEBUG) {
            logger.debug(messageBuilder.toString());
        }
    }

}
