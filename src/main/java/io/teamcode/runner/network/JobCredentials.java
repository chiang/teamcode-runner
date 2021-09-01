package io.teamcode.runner.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * Runner 에서 TeamCode CI 쪽에 요청할 때 사용하는 정보.
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCredentials {

    /**
     * The build ID to upload artifacts for, CI_JOB_ID
     */
    private Long id;

    /**
     * Build token, CI_JOB_TOKEN
     */
    private String token;

    /**
     * GitLab CI URL, CI_SERVER_URL
     */
    private String url;

}
