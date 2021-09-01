package io.teamcode.runner.shell;

import io.teamcode.runner.common.JobVariable;

/**
 * Created by chiang on 2017. 5. 6..
 */
public interface ShellWriter {

    void variable(JobVariable variable);

    void line(String text);

    void command(String command, String... arguments);

    void indent();

    void unIndent();


    /**
     * 파일이 존재하는지 체크하는 Shell Script 를 작성합니다.
     *
     * @param path
     */
    void ifFile(String path);

    void ifDirectory(String path);

    void ifCmd(String cmd, String... arguments);

    //else
    void elze();

    void endIf();

    /**
     * Home Directory 로 이동합니다.
     */
    void cd();

    void cd(String path);

    void mkDir(String path);

    void rmDir(String path);

    void rmFile(String path);

    void newFile(String path, String content);

    String getAbsolutePath(String dir);

    //print...
    void warning(String fmt, String... arguments);

    /**
     * Shell 에 echo 로 메시지를 출력합니다 (정확히는 echo 명령어를 만들어 줍니다).
     * @param format
     * @param arguments
     */
    void notice(String format, String... arguments);

    void emptyLine();

}
