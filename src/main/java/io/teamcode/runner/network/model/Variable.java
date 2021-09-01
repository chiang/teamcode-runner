package io.teamcode.runner.network.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Job Variable. Job 내부 혹은 글로벌로 설정된 값을 표현합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Variable {

    private String name;

    private String value;
}
