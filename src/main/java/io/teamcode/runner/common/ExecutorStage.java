package io.teamcode.runner.common;

/**
 * Created by chiang on 2017. 4. 27..
 */
public enum ExecutorStage {

    CREATED("created"),
    PREPARE("prepare"),
    FINISH("finish"),
    CLEANUP("cleanup");

    private String label;

    ExecutorStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

}
