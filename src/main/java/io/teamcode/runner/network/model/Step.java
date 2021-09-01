package io.teamcode.runner.network.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * pipeline 설정에서 Job 이 있는데 여기서 script, after_script 가 있다. 이것을 Step 이라고 한다. FIXME before 가 왜 없는지는 생각해 볼 필요가 있다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Step {

    private String name;

    private List<String> scripts;

    private int timeout;

    /*private

    Name         StepName   `json:"name"`
    Script       StepScript `json:"script"`
    Timeout      int        `json:"timeout"`
    When         StepWhen   `json:"when"`
    AllowFailure bool       `json:"allow_failure"`*/
}
