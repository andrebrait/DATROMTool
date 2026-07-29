package io.github.datromtool.cli.progressbar;

import io.github.datromtool.io.FileScanner;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bar draws into a fixed block of rows and every draw returns the cursor to the top of it. A
 * failure that belongs to no scanning thread has no row of its own: it takes one line for itself
 * — that is what the user reads — and must then re-open the block, so the rows the bar draws into
 * still start where the cursor is. Without that, every later thread line renders one row off,
 * once per failure.
 */
class CommandLineProgressBarCursorTest {

    private static final int THREADS = 2;

    /** jansi renders these as cursor-next-line and cursor-previous-line. */
    private static final Pattern LINE_MOVE = Pattern.compile("\\[(\\d+)([EF])");

    /**
     * Rows the cursor ends up below where it started: newlines and cursor-next-line move down,
     * cursor-previous-line moves back up.
     */
    private static int netRowsMoved(String rendered) {
        int net = 0;
        for (int i = 0; i < rendered.length(); i++) {
            if (rendered.charAt(i) == '\n') {
                net++;
            }
        }
        Matcher matcher = LINE_MOVE.matcher(rendered);
        while (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            net += matcher.group(2).equals("E") ? amount : -amount;
        }
        return net;
    }

    @Test
    void aThreadLessFailureLeavesTheCursorWhereItFoundIt() throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(new ByteArrayInputStream(new byte[0]), captured)
                .build()) {
            CommandLineProgressBar bar =
                    new CommandLineProgressBar(terminal, "Scanning", "Scanning input...");
            bar.init(THREADS);
            bar.reportTotalItems(THREADS);
            bar.reportStart(1, Path.of("rom.bin"), 1024L);
            terminal.flush();
            captured.reset();

            bar.reportFailure(
                    FileScanner.Listener.NO_THREAD,
                    Path.of("unreadable"),
                    "Could not list the directory",
                    new java.io.IOException("boom"));
            terminal.flush();
        }

        String rendered = captured.toString(StandardCharsets.UTF_8);
        assertTrue(
                rendered.contains("Could not list the directory"),
                () -> "test premise: the failure must be reported to the user, got:\n" + rendered);
        assertEquals(
                1,
                netRowsMoved(rendered),
                () -> "the failure must take exactly its own row, got:\n" + rendered);
        assertTrue(
                rendered.contains("[" + (THREADS + 1) + "F"),
                () -> "the bar must re-open its " + (THREADS + 1) + "-row block after the line, "
                        + "so later thread lines still land on their own rows, got:\n" + rendered);
    }
}
