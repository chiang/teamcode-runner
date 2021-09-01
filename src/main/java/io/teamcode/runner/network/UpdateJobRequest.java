package io.teamcode.runner.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.teamcode.runner.common.JobState;
import io.teamcode.runner.network.model.VersionInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by chiang on 2017. 4. 27..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobRequest {

    private VersionInfo info;

    private String token;

    private JobState state;

    private String trace;

}
