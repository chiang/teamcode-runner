package io.teamcode.runner.trace;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import io.teamcode.runner.AbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ExecStartResultJobTraceCallback extends ResultCallbackTemplate<ExecStartResultCallback, Frame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecStartResultJobTraceCallback.class);

    private OutputStream stdout, stderr;

    private JobTrace jobTrace;

    public ExecStartResultJobTraceCallback(JobTrace jobTrace) {
        //this.stdout = stdout;
        //this.stderr = stderr;
        this.jobTrace = jobTrace;
    }


    @Override
    public void onNext(Frame frame) {
        if (frame != null) {
            try {
                switch (frame.getStreamType()) {
                    case STDOUT:
                    case RAW:
                        jobTrace.process(ByteBuffer.wrap(frame.getPayload()));
                        break;
                    case STDERR:
                        jobTrace.process(ByteBuffer.wrap(frame.getPayload()));
                    default:
                        LOGGER.error("unknown stream type:" + frame.getStreamType());
                }
                //} catch (IOException e) {
                //   onError(e);
                //}
            } catch (AbortedException e) {
                onError(e);//TODO 잘 취소 되는가?
            }

            //LOGGER.debug(frame.toString());
        }
    }
}
