package io.teamcode.runner.shell;

/**
 * Created by chiang on 2017. 5. 7..
 */

import io.teamcode.runner.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BashWriter implements ShellWriter {

    private static final Logger logger = LoggerFactory.getLogger(BashWriter.class);

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    private String temporaryPath;

    private int indent;

    @Override
    public void variable(JobVariable variable) {
        if (variable.isFile()) {//TODO
            this.line("mkdir -p");
            this.line("echo -p");
            this.line(String.format("export %s=", ShellEscape.escape(variable.getName())));

            //gitlab 기능. %q 는 quote 로 감싸는 옵션이다....
            //b.Line(fmt.Sprintf("mkdir -p %q", helpers.ToSlash(b.TemporaryPath)))
            //b.Line(fmt.Sprintf("echo -n %s > %q", helpers.ShellEscape(variable.Value), variableFile))
            //b.Line(fmt.Sprintf("export %s=%q", helpers.ShellEscape(variable.Key), variableFile))
        }
        else {
            this.line(String.format("export %s=%s", ShellEscape.escape(variable.getName()), ShellEscape.escape(variable.getValue())));
        }
    }

    @Override
    public void command(String command, String... arguments) {
        this.line(buildCommand(command, arguments));
    }

    @Override
    public void line(String text) {
        try {
            // r 은 오류가 남...
            //outputStream.write(String.format("%s%s\r\n", String.join("", Collections.nCopies(indent, "  ")), text).getBytes());
            outputStream.write(String.format("%s%s", String.join("", Collections.nCopies(indent, "  ")), text).getBytes());
            outputStream.write(System.lineSeparator().getBytes());
        } catch (IOException e) {
            //Ignore. ByteArray
            logger.error("Cannot write script line: ", e);
        }
    }

    @Override
    public void indent() {
        this.indent++;
    }

    @Override
    public void unIndent() {
        this.indent--;
    }

    @Override
    public void ifFile(final String path) {
        this.line(String.format("if [[ -e \"%s\" ]]; then", path));
        this.indent();
    }

    @Override
    public void ifDirectory(String path) {
        this.line(String.format("if [[ -d \"%s\" ]]; then", path));
        this.indent();
    }

    @Override
    public void ifCmd(String cmd, String... arguments) {
        String cmdLine = this.buildCommand(cmd, arguments);
        this.line(String.format("if %s >/dev/null 2>/dev/null; then", cmdLine));
        this.indent();
        /*
        cmdline := b.buildCommand(cmd, arguments...)
	b.Line(fmt.Sprintf("if %s >/dev/null 2>/dev/null; then", cmdline))
	b.Indent()
         */
    }

    @Override
    public void elze() {
        this.unIndent();
        this.line("else");
        this.indent();
    }

    @Override
    public void endIf() {
        this.unIndent();
        this.line("fi");
    }

    @Override
    public void mkDir(String path) {
        this.command("mkdir", "-p", path);
    }

    @Override
    public void cd() {
        this.command("cd");
    }

    @Override
    public void cd(String path) {
        this.command("cd", path);
        //this.command("echo", "hhhh");
    }

    @Override
    public void rmDir(final String path) {
        this.command("rm", "-r", "-f", path);
    }

    @Override
    public void rmFile(final String path) {
        this.command("rm", "-f", path);
    }

    @Override
    public void newFile(String path, String content) {
        this.line(String.format("echo '%s' > %s", content, path));

        //FIXME 아래 것을 실행하면 Escape 처리가 되면서 조금 그렇다...
        //this.command("echo", content, ">", path);
    }

    @Override
    public String getAbsolutePath(final String dir) {
        if (Paths.get(dir).isAbsolute()) {
            return dir;
        }
        else {
            return new StringBuilder("$PWD").append(dir).toString();
        }
    }

    @Override
    public void warning(final String format, final String... arguments) {
        String coloredText =
                new StringBuilder(AnsiColors.ANSI_YELLOW).append(String.format(format, arguments)).append(AnsiColors.ANSI_RESET).toString();

        this.line(String.format("echo %S", ShellEscape.escape(coloredText)));
    }

    @Override
    public void notice(final String format, String... arguments) {
        String coloredText =
                new StringBuilder(AnsiColors.ANSI_BOLD_GREEN).append(String.format(format, arguments)).append(AnsiColors.ANSI_RESET).toString();

        this.line(String.format("echo %S", ShellEscape.escape(coloredText)));
        //coloredText := helpers.ANSI_BOLD_GREEN + fmt.Sprintf(format, arguments...) + helpers.ANSI_RESET
        //b.Line("echo " + helpers.ShellEscape(coloredText))
    }

    @Override
    public void emptyLine() {
        this.line("echo");
    }

    /*
    func (b *BashWriter) Warning(format string, arguments ...interface{}) {
	coloredText := helpers.ANSI_YELLOW + fmt.Sprintf(format, arguments...) + helpers.ANSI_RESET
	b.Line("echo " + helpers.ShellEscape(coloredText))
}
     */


    private String buildCommand(String command, String... arguments) {
        List<String> commands = new ArrayList<>();
        commands.add(ShellEscape.escape(command));
        //commands.add(command);

        for (String argument: arguments) {
            commands.add(String.format("\"%s\"", argument));
        }

        //for _, argument := range arguments {
        //    list = append(list, strconv.Quote(argument))
        //}

        return String.join(" ", commands);
    }

    void writeScript(BuildStage buildStage, ShellScriptInfo shellScriptInfo) {

    }

    /**
     * <code>set -o noclobber</code> 는 출력을 저장할 파일이 존재하는지 확인해서 이미 존재할 경우 그 파일을 보호하기 위해서
     * 에러 메시지만 출력하고 실행한 명령어를 중단해 버리게 되어 출력 재지정 시 덮어 쓰기를 방지합니다.
     * <code>set +o noclobber</code> 는 위와는 반대로 명시한 파일에 데이터를 저장하게 되어 기존에 존재하는 파일의 경우 원래의
     * 데이터를 잃어버리고 새로운 데이터만 저장합니다.
     *
     *
     * @param isTrace
     * @return
     * @throws IOException
     */
    String finish(boolean isTrace) throws IOException {
        if (outputStream.size() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        if (isTrace)
            builder.append("set -o xtrace\n");

        builder.append("set -eo pipefail\n");//이렇게 설정하면 여러 명령이 파이프로 되어 있는 경우 중간에 하나만 실패해도 전체를 실패한느 것으로 간주합니다.
        builder.append("set +o noclobber\n");
        builder.append(String.format(": | eval %s\n", ShellEscape.escape(outputStream.toString())));
        //builder.append(String.format("%s\n", outputStream.toString()));
        builder.append("exit 0\n");

        return builder.toString();
    }

    public void setTemporaryPath(final String temporaryPath) {
        this.temporaryPath = temporaryPath;
    }

}
