package io.teamcode.runner;

/**
 * Created by chiang on 2017. 5. 8..
 */
public class BuildError extends RuntimeException {

    public BuildError() {
        super();
    }

    public BuildError(String s) {
        super(s);
    }
}
