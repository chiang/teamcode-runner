package io.teamcode.runner.network.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Runner 가 TeamCode CI 쪽에 처리할 Job 이 있는지를 확인하는 요청 정보. Json 으로 요청하게 됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    private VersionInfo info;

    /**
     * Runner Token
     */
    private String token;

    private String lastUpdate;
}
