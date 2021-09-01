package io.teamcode.runner.network.model;

import io.teamcode.runner.common.JobState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Created by chiang on 2017. 5. 11..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracePatchResponse {

    private ResponseEntity<?> responseEntity;

    private JobState remoteState;

    public boolean isAborted() {
        if (responseEntity.getStatusCode() == HttpStatus.FORBIDDEN) {
            return true;
        }

        if (remoteState == JobState.FAILED || remoteState == JobState.CANCELED) {
            return true;
        }

        return false;
    }
}
