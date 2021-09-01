package io.teamcode.runner.shell;

import lombok.Data;

import java.util.List;

/**
 * Created by chiang on 2017. 5. 8..
 */
@Data
public class ShellConfiguration {

    private String command;

    private List<String> dockerCommand;

    /**
     * 미리 정의된 Docker Container 를 사용할 지 여부. 기본 값은 <code>false</code> 입니다.
     *
     */
    private boolean predefined;
}
