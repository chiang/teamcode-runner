package io.teamcode.runner;

/**
 * Created by chiang on 2017. 5. 11..
 */
public class RunnerException extends RuntimeException {

    public RunnerException() {
        super();
    }

    public RunnerException(String s) {
        super(s);
    }

    public RunnerException(Throwable t) {
        super(t);
    }

    public RunnerException(String s, Throwable t) {
        super(s, t);
    }
}
