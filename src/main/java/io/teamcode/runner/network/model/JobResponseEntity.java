package io.teamcode.runner.network.model;

import lombok.Builder;
import lombok.Data;

/**
 *
 */
@Data
@Builder
public class JobResponseEntity {

    private JobResponse jobResponse;

    private boolean success;

}
