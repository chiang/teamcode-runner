package io.teamcode.runner.shell;

import io.teamcode.runner.common.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by chiang on 2017. 4. 27..
 */
public class BashShell extends AbstractShell {

    private static final String NAME = "bash";

    private static final String[] BASH_DETECT_SHELL_ARRAY =
            {"if [ -x /usr/local/bin/bash ]; then",
             "  exec /usr/local/bin/bash $@",
             "elif [ -x /usr/bin/bash ]; then",
             "  exec /usr/bin/bash $@",
             "elif [ -x /bin/bash ]; then",
             "  exec /bin/bash $@",
             "elif [ -x /usr/local/bin/sh ]; then",
             "  exec /usr/local/bin/sh $@",
             "elif [ -x /usr/bin/sh ]; then",
             "  exec /usr/bin/sh $@",
             "elif [ -x /bin/sh ]; then",
             "  exec /bin/sh $@",
             "else",
             "  echo shell not found",
             "  exit 1",
             "fi"
            };

    private static final String BASH_DETECT_SHELL = String.join("\n", BASH_DETECT_SHELL_ARRAY);

    public BashShell() {
        super(NAME);
    }

    @Override
    public String getName() {

        return this.name;
    }

    @Override
    public String generateScript(BuildStage buildStage, ShellScriptInfo shellScriptInfo) throws IOException {

        BashWriter bashWriter = new BashWriter();
        bashWriter.setTemporaryPath(shellScriptInfo.getBuild().getBuildDir() + ".tmp");

        this.writeScript(bashWriter, buildStage, shellScriptInfo);

        return bashWriter.finish(shellScriptInfo.getBuild().isDebugTraceEnabled());
    }

    @Override
    public ShellConfiguration getShellConfiguration(ShellScriptInfo shellScriptInfo) {
        String detectScript;

        //TODO loginshell not supported yet.
        //if info.Type == common.LoginShell {
        //    detectScript = strings.Replace(bashDetectShell, "$@", "--login", -1)
        //    shellCommand = b.Shell + " --login"
        //} else {
            //detectScript = strings.Replace(bashDetectShell, "$@", "", -1)
            //shellCommand = b.Shell
        //}

        detectScript = BASH_DETECT_SHELL.replace("$@", "");

        ShellConfiguration shellConfiguration = new ShellConfiguration();
        shellConfiguration.setDockerCommand(Arrays.asList("sh", "-c", detectScript));

        return shellConfiguration;
    }

}
