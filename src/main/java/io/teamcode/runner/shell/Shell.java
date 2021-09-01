package io.teamcode.runner.shell;

import io.teamcode.runner.common.BuildStage;
import io.teamcode.runner.common.ShellScriptInfo;

import java.io.IOException;

/**
 * Created by chiang on 2017. 5. 6..
 */
public interface Shell {

    String getName();

    String generateShellScript(BuildStage buildStage, ShellScriptInfo shellScriptInfo) throws IOException;

    String generateScript(BuildStage buildStage, ShellScriptInfo shellScriptInfo) throws IOException;

    ShellConfiguration getShellConfiguration(ShellScriptInfo shellScriptInfo);

}
