package io.teamcode.runner.shell;

/**
 * Created by chiang on 2017. 5. 8..
 */
public class ShellProviderException extends RuntimeException {

    public ShellProviderException() {
        super();
    }

    public ShellProviderException(String s) {
        super(s);
    }

    public ShellProviderException(Throwable t) {
        super(t);
    }

    public ShellProviderException(String s, Throwable t) {
        super(s, t);
    }
}
