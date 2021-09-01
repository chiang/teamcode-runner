package io.teamcode.runner.network.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by chiang on 2017. 5. 11..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRunnerRequest {

    private String token;

    private VersionInfo info;
}
