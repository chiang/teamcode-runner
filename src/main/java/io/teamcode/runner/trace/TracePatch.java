package io.teamcode.runner.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by chiang on 2017. 5. 9..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracePatch {

    private static final Logger logger = LoggerFactory.getLogger(TracePatch.class);

    private ByteBuffer trace;

    private int offset;

    private int limit;

    public boolean validateRange() {
        //logger.debug("----------------limit: " + limit + ", offset: " + offset + ", capa: " + trace.capacity() + ", remaining: " + trace.remaining() + ", buffer: " + trace);
        if (this.limit >= this.offset) {
            return true;
        }

        return false;
    }

    public byte[] get() {
        if ((limit - offset) > 0) {
            byte[] bytes = new byte[limit - offset];
            //get 메소드의 offset 는 ByteBuffer 의 offset 이 아니라 담을 바이트 배열의 Offset 이다.
            for (int i = offset, j = 0; i < limit; i++, j++) {
                bytes[j] = trace.get(i);
            }

            return bytes;
        }
        else {
            return new byte[0];
        }
    }
}
