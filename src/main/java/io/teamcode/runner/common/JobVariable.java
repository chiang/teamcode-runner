package io.teamcode.runner.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Runner 에서 사용하는 Variables. <code>Variable</code> 클래스와는 다르게, Runner 내부에서 사용하게끔 정의한 클래스입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobVariable {

    private String name;

    private String value;

    private boolean publik;

    private boolean internal;

    private boolean file;

    /*
    Key      string `json:"key"`
	Value    string `json:"value"`
	Public   bool   `json:"public"`
	Internal bool   `json:"-"`
	File     bool   `json:"file"`
     */
}
