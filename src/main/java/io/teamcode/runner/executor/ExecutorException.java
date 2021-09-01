package io.teamcode.runner.executor;

/**
 * Created by chiang on 2017. 5. 8..
 */
public class ExecutorException extends Exception {

    public ExecutorException() {
        super();
    }

    public ExecutorException(String s) {
        super(s);
    }

    public ExecutorException(Throwable t) {
        super(t);
    }

    public ExecutorException(String s, Throwable t) {
        super(s, t);
    }
}
