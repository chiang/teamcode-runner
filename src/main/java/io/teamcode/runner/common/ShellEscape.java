package io.teamcode.runner.common;

import org.apache.commons.codec.binary.Hex;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// ShellEscape is taken from https://github.com/solidsnack/shell-escape/blob/master/Text/ShellEscape/Bash.hs
/**
 * A Bash escaped string. The strings are wrapped in @$\'...\'@ if any
 * bytes within them must be escaped; otherwise, they are left as is.
 * Newlines and other control characters are represented as ANSI escape
 * sequences. High bytes are represented as hex codes. Thus Bash escaped
 * strings will always fit on one line and never contain non-ASCII bytes.
 *
 * TODO to static ?
 *
 */
public abstract class ShellEscape {

    private static final byte ACK           = 6;
    private static final byte TAB           = 9;
    private static final byte LF            = 10;
    private static final byte CR            = 13;
    private static final byte US            = 31;
    private static final byte SPACE         = 32;
    private static final byte AMPERSTAND    = 38;
    private static final byte SINGLE_QUOTE  = 39;
    private static final byte PLUS          = 43;
    private static final byte NINE          = 57;
    private static final byte QUESTION      = 63;
    private static final byte LOWERCASE_Z   = 90;
    private static final byte OPEN_BRACKET  = 91;
    private static final byte BACKSLASH     = 92;
    private static final byte UNDERSCORE    = 95;
    private static final byte CLOSE_BRACKET = 93;
    private static final byte BACKTICK      = 96;
    private static final byte TILDA         = 126;
    private static final byte DEL           = 127;

    public static final String escape(String text) {
        if (!StringUtils.hasLength(text)) {
            return "''";
        }

        boolean escaped = false;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] chars = text.getBytes();

        byte c;
        for(int i = 0; i < chars.length; i++) {
            c = chars[i];

            try {
                switch (c) {
                    case TAB:
                        if (escape((byte)'\t', outputStream))
                            escaped = true;
                        break;
                    case LF:
                        if (escape((byte)'\n', outputStream))
                            escaped = true;
                        break;
                    case CR:
                        if (escape((byte)'\r', outputStream))
                            escaped = true;
                        break;
                    case SINGLE_QUOTE:
                        if (backslash(c, outputStream))
                            escaped = true;
                        break;
                    case BACKSLASH:
                        if (backslash(c, outputStream))
                            escaped = true;
                        break;
                    case OPEN_BRACKET:
                        if (quoted(c, outputStream))
                            escaped = true;
                        break;
                    case UNDERSCORE:
                        if (literal(c, outputStream))
                            escaped = true;
                        break;
                    case DEL:
                        if (hex(c, outputStream))
                            escaped = true;
                        break;
                    default:
                        if (c <= US) {
                            if (hex(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= AMPERSTAND) {
                            if (quoted(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= PLUS) {
                            if (quoted(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= NINE) {
                            if (literal(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= QUESTION) {
                            if (quoted(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= LOWERCASE_Z) {
                            if (literal(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= CLOSE_BRACKET) {
                            if (quoted(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= BACKTICK) {
                            if (quoted(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= LOWERCASE_Z) {
                            if (literal(c, outputStream))
                                escaped = true;
                            break;
                        } else if (c <= TILDA) {
                            if (quoted(c, outputStream))
                                escaped = true;
                            break;
                        } else {
                            if (hex(c, outputStream))
                                escaped = true;
                            break;
                        }
                }
            } catch(IOException e) {
                //Ignore
            }
        }

        if (escaped) {
            return String.format("$'%s'", outputStream.toString());
        }
        else {
            return outputStream.toString();
        }
    }

    /**
     *
     * @param b
     * @return escaped
     */
    private final static boolean escape(byte b, ByteArrayOutputStream outputStream) {
        outputStream.write(b);
        return true;
    }

    private final static boolean hex(byte b, ByteArrayOutputStream outputStream) throws IOException {

        byte[] data = {BACKSLASH, 'x', 0, 0};
        char[] hex = Hex.encodeHex(new byte[]{b});
        data[2] = (byte)hex[0];
        data[3] = (byte)hex[1];
        outputStream.write(data);

        /*
        data := []byte{BACKSLASH, 'x', 0, 0}
        hex.Encode(data[2:], []byte{char})
        out.Write(data)
         */

        return true;
    }

    private static final boolean backslash(byte b, ByteArrayOutputStream outputStream) throws IOException {
        outputStream.write(new byte[]{BACKSLASH, b});

        return true;
    }

    private static final boolean quoted(byte b, ByteArrayOutputStream outputStream) {
        outputStream.write(b);

        return true;
    }

    private static final boolean literal(byte b, ByteArrayOutputStream outputStream) {
        outputStream.write(b);

        return false;
    }

}
