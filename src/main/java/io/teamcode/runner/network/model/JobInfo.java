package io.teamcode.runner.network.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * Created by chiang on 2017. 5. 8..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInfo {

    private String name;

    private String stage;

    private Long projectId;

    private String projectName;

    private String projectPath;

}
