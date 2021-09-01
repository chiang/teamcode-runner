package io.teamcode.runner.executor;

import io.teamcode.runner.common.BuildStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * Created by chiang on 2017. 5. 7..
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorCommand {

    private BuildStage buildStage;

    private String script;

    private boolean predefined;

    private Function<String, String> abortFunction;

    /*Script     string
    Predefined bool
    Abort      chan interface{}*/
}
