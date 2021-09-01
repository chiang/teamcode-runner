package io.teamcode.runner.network.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by chiang on 2017. 5. 10..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryInfo {

    private String url;

    private String revision;
}
