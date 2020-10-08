package io.github.datromtool.cli.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jline.terminal.Terminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TerminalUtils {

    private static final String TRIM_PREFIX = "(...)";

    public static int availableColumns(@Nonnull String text, @Nullable Terminal terminal) {
        int width = terminal != null ? terminal.getWidth() : 80;
        return Math.max(0, width - text.length());
    }

    public static String repeat(char c, int times) {
        if (times <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static String trimTo(@Nonnull String s, int n) {
        if (s.length() > n) {
            return TRIM_PREFIX + s.substring(Math.max(
                    0,
                    Math.min(s.length(), s.length() - (n - TRIM_PREFIX.length()))));
        }
        return s;
    }
}
