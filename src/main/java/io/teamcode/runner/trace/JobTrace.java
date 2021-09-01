package io.teamcode.runner.trace;

import io.teamcode.runner.AbortedException;
import io.teamcode.runner.BuildLogger;
import io.teamcode.runner.common.*;
import io.teamcode.runner.core.ApplicationContextProvider;
import io.teamcode.runner.network.JobCredentials;
import io.teamcode.runner.network.NetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <code>JobTrace</code> 는 <code>Build</code> 에서 Job 처리 내역을 추적 (로그 등) 하는데 사용합니다. 이 JobTrace 는 Build Instance
 * Lifecycle 내에서만 존재합니다.
 *
 * Created by chiang on 2017. 4. 27..
 */
public class JobTrace implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JobTrace.class);

    private NetworkClient networkClient;

    private JobCredentials jobCredentials;

    private Lock lock = new ReentrantLock();

    private boolean finished;

    private long traceUpdateInterval = 1000 * 2;

    private ByteBuffer byteBuffer;

    private int limit;

    private String exceedMessaged;

    /**
     * 마지막으로 전송한 ByteBuffer Position. 이 값을 기준으로 데이터를 읽어서 전송합니다.
     */
    private int sentTrace;

    /**
     * 마지막으로 쓴 데이터 offset. 이 값을 기준으로 버퍼에 쓸 때 사용합니다.
     */
    private int writeOffset;

    /**
     * 마지막으로 서버 측에 전송한 JobState
     */
    private JobState sentJobState;

    private JobState currentJobState;

    private Date sentTime;

    private boolean watchEnabled;

    public JobTrace(JobCredentials jobCredentials, boolean watchEnabled) {
        //logger.debug("Job trace initializing...");
        this.networkClient = ApplicationContextProvider.getBean("networkClient", NetworkClient.class);
        this.jobCredentials = jobCredentials;

        this.limit = RunnerConstants.DEFAULT_OUTPUT_LIMIT * 1024;
        this.exceedMessaged = String.format("\n%sJob's log exceeded limit of %s bytes.%s\n", AnsiColors.ANSI_BOLD_RED, limit, AnsiColors.ANSI_RESET);
        this.byteBuffer = ByteBuffer.allocateDirect(limit + exceedMessaged.length());
        this.sentTrace = byteBuffer.position();
        this.writeOffset = 0;
        this.byteBuffer.flip();
        this.watchEnabled = watchEnabled;
    }

    public void process(ByteBuffer srcByteBuffer) throws AbortedException {
        if (this.currentJobState == JobState.CANCELED)
            throw new AbortedException();

        /*byte[] bytes = new byte[srcByteBuffer.remaining()];
        srcByteBuffer.get(bytes);
        logger.debug("[[{}]]", new String(bytes));*/

        lock.lock();
        try {
            //logger.debug("byte buffer before process... {}, write offset: {}", byteBuffer, this.writeOffset);
            if (byteBuffer.limit() + srcByteBuffer.remaining() >= limit) {
                this.byteBuffer.position(writeOffset);
                this.byteBuffer.limit(this.byteBuffer.capacity());
                this.byteBuffer.put(exceedMessaged.getBytes());
                this.writeOffset += exceedMessaged.getBytes().length;
                this.byteBuffer.flip();
            }
            else {
                this.byteBuffer.position(writeOffset);
                this.byteBuffer.limit(this.byteBuffer.capacity());
                this.writeOffset += srcByteBuffer.remaining();
                this.byteBuffer.put(srcByteBuffer);
                this.byteBuffer.flip();
            }
            //logger.debug("byte buffer after process... {}, write offset: {}", byteBuffer, this.writeOffset);
        } catch(BufferOverflowException e) {
            logger.warn("Jobs log buffer overflow");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        this.currentJobState = JobState.RUNNING;
        this.watch();
    }

    public void success() {
        try {
            this.sendLog(BuildLoggerType.INFO, "Job succeeded");
        } catch (AbortedException e) {
            //TODO nothing? e.printStackTrace();
        }
        lock.lock();
        this.currentJobState = JobState.SUCCESS;
        lock.unlock();

        this.finish();
    }

    public void fail(String message) {
        try {
            this.sendLog(BuildLoggerType.ERROR, String.format("Job Failed: %s", message));
        } catch (AbortedException e) {
            //TODO e.printStackTrace();
        }

        lock.lock();

        if (this.currentJobState != JobState.RUNNING) {
            lock.unlock();
            return;
        }

        this.currentJobState = JobState.FAILED;
        this.lock.unlock();

        this.finish();
    }

    /**
     * 서버에 취소 메시지를 전달합니다.
     *
     */
    public void cancel() {
        try {
            this.sendLog(BuildLoggerType.WARN, "Job Failed: canceled");
            //ERROR: Job failed: canceled
        } catch (AbortedException e) {
            //무시합니다. 그냥 무조건...
        }
    }

    //TODO buildLogger 의 그것과 중복되었습니다. 정리가 필요합니다.
    private final void sendLog(BuildLoggerType buildLoggerType, String prefix, String... arguments) throws AbortedException {
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
        messageBuilder.append(prefix);
        messageBuilder.append(String.join(" ", arguments));
        if (buildLoggerType != null)
            messageBuilder.append(AnsiColors.ANSI_RESET);
        messageBuilder.append("\r\n");
        //String message = String.format("%s%s%s", prefix, , AnsiColors.ANSI_RESET);

        try {
            this.process(ByteBuffer.wrap(messageBuilder.toString().getBytes("utf-8")));//TODO 그냥 String 을 전달, 처리하는 것을 만들 필요기 있겠다.
        } catch (UnsupportedEncodingException e) {
            //Ignore
        }

        if (buildLoggerType == BuildLoggerType.DEBUG) {
            logger.debug(messageBuilder.toString());
        }
    }

    private void finish() {
        //c.Close()
        this.finished = true;
        logger.debug("Job trace was finished with state '{}'.", this.currentJobState);

        //TODO 계속 무한으로 요청하는가?
        // Do final upload of job trace
        while(true) {
            logger.debug("Job trace fully update to server...");
            if (this.fullUpdate() != UpdateState.UPDATE_FAILED) {
                return;
            }

            try {
                TimeUnit.SECONDS.sleep(RunnerConstants.UPDATE_RETRY_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    private UpdateState fullUpdate() {
        lock.lock();
        TracePatch tracePatch = newTracePatch();
        byte[] message = tracePatch.get();
        lock.unlock();

        /*if this.sentJobState == this.currentJobState &&
                //this.sentTrace == len(trace) &&
                this.sentTrace == this.byteBuffer.limit() &&
                time.Since(c.sentTime) < traceForceSendInterval {
            return common.UpdateSucceeded
        }*/
        logger.debug("Job trace fully update to server with state: {}, sent job state: {}", this.currentJobState, this.sentJobState);
        if (this.sentJobState == this.currentJobState) {

            return UpdateState.UPDATE_SUCCEEDED;
        }

        UpdateState updateState = null;
        try {
            updateState = networkClient.updateJob(jobCredentials, currentJobState, new String(message, "UTF-8"));
            if (updateState == UpdateState.UPDATE_SUCCEEDED) {
                this.sentTrace = tracePatch.getLimit();
                this.sentJobState = this.currentJobState;
                this.sentTime = new Date();
            }

            return updateState;
        } catch (UnsupportedEncodingException e) {
            logger.error("로그 메시지를 인코딩하던 중 오류가 발생했습니다.", e);

            return UpdateState.UPDATE_FAILED;
        }
    }

    private void watch() {
        if (this.watchEnabled) {
            logger.debug("watching trace....");
            while (true) {
                if (this.finished) {
                    return;
                }

                UpdateState updateState = incrementalUpdate();
                //if (updateState == UpdateState.UPDATE_ABORT && abort()) {
                if (updateState == UpdateState.UPDATE_ABORT) {
                    logger.info("현재 작업이 CI 서버 측에서 취소되었습니다.");
                    this.abort();
                    this.finished = true;
                }

                try {
                    //Thread.sleep(this.traceUpdateInterval);
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage());//TODO details

                    return;
                }
            }
        }
    }

    private UpdateState incrementalUpdate() {
        try {
            lock.lock();
            TracePatch tracePatch = newTracePatch();
            byte[] message = tracePatch.get();
            lock.unlock();

            /*
            if c.sentState == state &&
		c.sentTrace == trace.Len() &&
		time.Since(c.sentTime) < traceForceSendInterval {
		return common.UpdateSucceeded
	}
             */

            if (this.sentJobState != this.currentJobState) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Current job state: {}, sent job state: {}", this.currentJobState, this.sentJobState);
                    logger.debug("Updating current job state to teamcode server...");
                }
                //FIXME upateState 는 언제 쓰는가?
                networkClient.updateJob(jobCredentials, currentJobState, null);
                this.sentJobState = this.currentJobState;
            }

            //TODO 실패 시 sentTract 는 어떻게?
            if (message.length > 0) {
                logger.debug("trace message: [{}]", new String(message, "UTF-8"));

                UpdateState updateState = networkClient.patchTrace(jobCredentials, tracePatch);
                switch(updateState) {
                    case UPDATE_NOT_FOUND:
                        return updateState;

                    case UPDATE_RANGE_MISMATCH:
                        //update = c.resendPatch(c.jobCredentials.ID, c.config, c.jobCredentials, tracePatch);
                        break;

                    case UPDATE_SUCCEEDED:
                        this.sentTrace = tracePatch.getLimit();
                        this.sentTime = new Date();

                        return UpdateState.UPDATE_SUCCEEDED;

                    case UPDATE_ABORT:

                        return UpdateState.UPDATE_ABORT;

                }
            }

            //TODO
            return UpdateState.UPDATE_SUCCEEDED;
        } catch(IllegalStateException e) {
            e.printStackTrace();
            //TODO 여기서 에러가날 수 있는데 (Range 오류) 이것을 처리해야 한다. 메시지를 더 못 보내게 한다던지 등?
            //TODO
            //c.config.Log().Errorln("Error while creating a tracePatch", err.Error())
            throw e;
        } catch(Throwable t) {
            t.printStackTrace();
            return UpdateState.UPDATE_FAILED;
        }
    }

    private void abort() {
        this.currentJobState = JobState.CANCELED;

        //return false;
        /*
        select {
            case c.abortCh <- true:
                return true

            default:
                return false
        }
         */
    }

    public void setTraceUpdateInterval(long traceUpdateInterval) {
        this.traceUpdateInterval = traceUpdateInterval;
    }

    private final TracePatch newTracePatch() {
        TracePatch tracePatch = TracePatch.builder()
                .trace(byteBuffer)
                .offset(this.sentTrace)
                .limit(byteBuffer.limit())
                .build();

        if (!tracePatch.validateRange()) {
            throw new IllegalStateException("Range is invalid, limit can't be less than offset");//TODO custom?
        }

        return tracePatch;
    }

    public boolean isFinished() {

        return this.finished;
    }

}
