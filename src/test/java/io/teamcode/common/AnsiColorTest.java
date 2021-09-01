package io.teamcode.common;

import io.teamcode.runner.common.AnsiColors;
import org.junit.Test;

/**
 * Created by chiang on 2017. 8. 7..
 */
public class AnsiColorTest {

    @Test
    public void color() {
        String message = String.format("\n%sJob's log exceeded limit. %s\n", AnsiColors.ANSI_BOLD_RED, AnsiColors.ANSI_RESET);
        System.out.println(message);
    }
}
