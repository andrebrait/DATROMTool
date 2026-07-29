package io.github.datromtool.cli.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jline.terminal.Terminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TerminalUtils {

    private static final String TRIM_PREFIX = "(...)";

    /**
     * Control characters plus the invisible format characters (e.g. bidi overrides). {@code \\p{Cc}}
     * rather than {@code \\p{Cntrl}}: the latter is the POSIX class, ASCII-only, so it misses the C1
     * block — and U+009B is a single-character CSI that xterm-compatible terminals obey in 8-bit
     * mode, i.e. a complete bypass.
     */
    private static final Pattern TERMINAL_UNSAFE = Pattern.compile("[\\p{Cc}\\p{Cf}]");

    /**
     * Renders characters a terminal would obey rather than display — escape sequences, carriage
     * returns, bidi overrides — as their inert {@code \\uXXXX} spelling. Filenames and archive
     * entry names are attacker-controlled, so everything derived from them is rendered through
     * this before it reaches the screen (CWE-150).
     */
    public static String sanitizeForTerminal(@Nonnull String text) {
        return TERMINAL_UNSAFE.matcher(text)
                .replaceAll(match -> String.format("\\\\u%04X", (int) match.group().charAt(0)));
    }

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
