package io.teamcode.runner.shell;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chiang on 2017. 5. 7..
 */
@Component
public class ShellProvider {

    private Map<String, Class<? extends Shell>> shells = new HashMap<>();

    @PostConstruct
    public void init() {
        //log.Debugln("Registering", shell.GetName(), "shell...")

        /*if shells[shell.GetName()] != nil {
            panic("Shell already exist: " + shell.GetName())
        }
        shells[shell.GetName()] = shell*/

        shells.put("sh", BashShell.class);
        shells.put("bash", BashShell.class);
    }

    public Shell getShell(final String name) throws ShellProviderException {

        try {
            return shells.get(name).newInstance();
        } catch (Throwable t) {
            throw new ShellProviderException(String.format("'%s' 쉘을 로드할 수 없습니다", name), t);
        }
    }
}
