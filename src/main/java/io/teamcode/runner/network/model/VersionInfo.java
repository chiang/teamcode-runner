package io.teamcode.runner.network.model;

import io.teamcode.runner.network.FeaturesInfo;
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
public class VersionInfo {

    private String name;

    private String version;

    private String revision;

    private String platform;

    private String architecture;

    private String executor;

    private FeaturesInfo features;

}
